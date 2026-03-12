param(
    [Parameter(Mandatory = $true)]
    [string]$StartCommit,

    [Parameter(Mandatory = $true)]
    [string[]]$SquashCommits,

    [Parameter(Mandatory = $true)]
    [string]$MessageFile,

    [string]$EndCommit = "HEAD",
    [string]$BackupPrefix = "backup/squash"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$scriptPath = $MyInvocation.MyCommand.Path
if (-not $env:BILLS_SQUASH_TEMP_RUN -and -not [string]::IsNullOrWhiteSpace($scriptPath)) {
    $repoRoot = (git rev-parse --show-toplevel).Trim()
    if ($LASTEXITCODE -eq 0 -and -not [string]::IsNullOrWhiteSpace($repoRoot)) {
        $tempDir = Join-Path $repoRoot "temp"
        New-Item -ItemType Directory -Force -Path $tempDir | Out-Null
        $tempScript = Join-Path $tempDir ("reusable_squash_first_parent.{0}.ps1" -f (Get-Date -Format "yyyyMMdd-HHmmss"))
        Copy-Item -LiteralPath $scriptPath -Destination $tempScript -Force

        $prevTempRun = $env:BILLS_SQUASH_TEMP_RUN
        $env:BILLS_SQUASH_TEMP_RUN = "1"
        try {
            & $tempScript @PSBoundParameters
            return
        } finally {
            if ($null -ne $prevTempRun) {
                $env:BILLS_SQUASH_TEMP_RUN = $prevTempRun
            } else {
                Remove-Item Env:BILLS_SQUASH_TEMP_RUN -ErrorAction SilentlyContinue
            }
        }
    }
}

function Get-Parents([string]$Commit) {
    $line = (git show -s --format=%P $Commit).Trim()
    if ([string]::IsNullOrWhiteSpace($line)) { return @() }
    return @($line.Split(" ", [System.StringSplitOptions]::RemoveEmptyEntries))
}

function Write-Utf8NoBomFile([string]$Path, [string]$Content) {
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($Path, $Content, $utf8NoBom)
}

function CherryPick-NoCommit([string]$OldCommit) {
    $parents = @(Get-Parents $OldCommit)
    if ($parents.Count -gt 1) {
        git cherry-pick --no-commit -m 1 $OldCommit | Out-Host
    } else {
        git cherry-pick --no-commit $OldCommit | Out-Host
    }
    if ($LASTEXITCODE -ne 0) { throw "Cherry-pick --no-commit failed: $OldCommit" }
}

function CherryPick-Commit([string]$OldCommit) {
    $parents = @(Get-Parents $OldCommit)
    if ($parents.Count -gt 1) {
        git cherry-pick -m 1 $OldCommit | Out-Host
    } else {
        git cherry-pick $OldCommit | Out-Host
    }
    if ($LASTEXITCODE -ne 0) { throw "Cherry-pick failed: $OldCommit" }
}

function Commit-With-Message([string]$AuthorSourceCommit, [string]$MessageText) {
    $tmp = New-TemporaryFile
    Write-Utf8NoBomFile -Path $tmp -Content $MessageText

    $author = (git show -s --format="%an <%ae>" $AuthorSourceCommit).Trim()
    $authorDate = (git show -s --format="%aI" $AuthorSourceCommit).Trim()

    $prevAuthorDate = $env:GIT_AUTHOR_DATE
    $env:GIT_AUTHOR_DATE = $authorDate

    git commit --author "$author" -F $tmp | Out-Host
    if ($LASTEXITCODE -ne 0) { throw "git commit failed." }

    if ($null -ne $prevAuthorDate) {
        $env:GIT_AUTHOR_DATE = $prevAuthorDate
    } else {
        Remove-Item Env:GIT_AUTHOR_DATE -ErrorAction SilentlyContinue
    }

    Remove-Item -LiteralPath $tmp -Force
}

$resolvedEnd = (git rev-parse $EndCommit).Trim()
$resolvedStart = (git rev-parse $StartCommit).Trim()
$base = (git rev-parse "$resolvedStart^").Trim()

$oldCommits = @(git rev-list --first-parent --reverse "$base..$resolvedEnd")
if ($oldCommits.Count -eq 0) { throw "No commits in selected range." }

$resolvedGroup = @()
foreach ($c in $SquashCommits) {
    $resolvedGroup += (git rev-parse $c).Trim()
}
if ($resolvedGroup.Count -lt 2) { throw "SquashCommits must contain at least 2 commits." }

if ($resolvedGroup[0] -ne $resolvedStart) {
    throw "StartCommit must be the first commit of SquashCommits."
}

$messageText = [System.IO.File]::ReadAllText((Resolve-Path $MessageFile), [System.Text.Encoding]::UTF8)
if ([string]::IsNullOrWhiteSpace($messageText)) { throw "MessageFile is empty." }

$indexByCommit = @{}
for ($i = 0; $i -lt $oldCommits.Count; $i++) { $indexByCommit[$oldCommits[$i]] = $i }

foreach ($c in $resolvedGroup) {
    if (-not $indexByCommit.ContainsKey($c)) { throw "Commit not in range: $c" }
}
for ($i = 1; $i -lt $resolvedGroup.Count; $i++) {
    if ($indexByCommit[$resolvedGroup[$i]] -ne ($indexByCommit[$resolvedGroup[$i - 1]] + 1)) {
        throw "SquashCommits are not contiguous in first-parent history."
    }
}

$oldHead = (git rev-parse HEAD).Trim()
$backupBranch = "$BackupPrefix-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
git branch $backupBranch | Out-Host
if ($LASTEXITCODE -ne 0) { throw "Failed to create backup branch." }

Write-Host "backup_branch=$backupBranch"
Write-Host "rewrite_base=$base"
Write-Host "rewrite_end=$resolvedEnd"
Write-Host "rewrite_count=$($oldCommits.Count)"

git reset --hard $base | Out-Host
if ($LASTEXITCODE -ne 0) { throw "git reset --hard failed." }

$newSquashCommit = $null
$i = 0
while ($i -lt $oldCommits.Count) {
    $old = $oldCommits[$i]

    if ($old -eq $resolvedGroup[0]) {
        foreach ($g in $resolvedGroup) {
            if ($oldCommits[$i] -ne $g) { throw "Unexpected order in squash group at index $i." }
            CherryPick-NoCommit $g
            $i++
        }

        Commit-With-Message -AuthorSourceCommit $resolvedGroup[-1] -MessageText $messageText
        $newSquashCommit = (git rev-parse HEAD).Trim()
        continue
    }

    if ($resolvedGroup -contains $old) {
        throw "Encountered non-leading commit from squash group: $old"
    }

    CherryPick-Commit $old
    $i++
}

$newHead = (git rev-parse HEAD).Trim()
Write-Host "old_head=$oldHead"
Write-Host "new_head=$newHead"
Write-Host "new_squash_commit=$newSquashCommit"
Write-Host "next_action=default_force_push_and_retag_if_needed"
