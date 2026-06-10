package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.bag.audioandroid.R
import com.bag.audioandroid.ui.appSegmentedButtonColors
import com.bag.audioandroid.ui.audioInputTextFieldColors
import com.bag.audioandroid.ui.component.ActionButton
import com.bag.audioandroid.ui.model.FactionThemeOption
import com.bag.audioandroid.ui.model.ThemeStyleOption
import com.bag.audioandroid.ui.theme.AppThemeAccentTokens
import com.bag.audioandroid.ui.theme.audioEncodeGlyphColorsForFactionTheme
import kotlinx.coroutines.android.awaitFrame
import java.lang.System.nanoTime
import kotlin.math.roundToInt

@Composable
internal fun DebugAnimationSection(
    accentTokens: AppThemeAccentTokens,
    selectedThemeStyle: ThemeStyleOption,
    factionThemes: List<FactionThemeOption>,
    selectedFactionTheme: FactionThemeOption,
    isDualThemeAnimationEnabled: Boolean,
    onDualThemeAnimationEnabledChange: (Boolean) -> Unit,
) {
    var durationInput by rememberSaveable { mutableStateOf("5") }
    var splitDurationInput by rememberSaveable { mutableStateOf("2.5") }
    var animationPlaybackDurationMs by rememberSaveable { mutableLongStateOf(0L) }
    var animationSplitDurationMs by rememberSaveable { mutableLongStateOf(2500L) }
    var animationSequenceDurationMs by rememberSaveable { mutableLongStateOf(0L) }
    var animationStartNanos by rememberSaveable { mutableLongStateOf(0L) }
    var animationElapsedMs by rememberSaveable { mutableLongStateOf(0L) }
    val availableThemes =
        remember(factionThemes, selectedFactionTheme) {
            buildAnimationThemeOptions(
                factionThemes = factionThemes,
                selectedFactionTheme = selectedFactionTheme,
            )
        }
    var firstThemeId by rememberSaveable { mutableStateOf(selectedFactionTheme.id) }
    var secondThemeId by rememberSaveable {
        mutableStateOf(
            availableThemes.firstOrNull { it.id != selectedFactionTheme.id }?.id ?: selectedFactionTheme.id,
        )
    }
    var themePickerTarget by rememberSaveable { mutableStateOf<AnimationThemePickerTarget?>(null) }
    val isAnimating = animationSequenceDurationMs > 0L
    val animationThemeMode =
        if (isDualThemeAnimationEnabled) {
            AnimationThemeMode.Dual
        } else {
            AnimationThemeMode.Single
        }
    val firstTheme =
        availableThemes.firstOrNull { it.id == firstThemeId } ?: availableThemes.firstOrNull() ?: selectedFactionTheme
    val secondTheme =
        availableThemes.firstOrNull { it.id == secondThemeId } ?: availableThemes.firstOrNull() ?: selectedFactionTheme
    val playbackElapsedMs = (animationElapsedMs - AnimationLeadInMs).coerceAtLeast(0L)
    val isInPlayback = isAnimating && animationElapsedMs in AnimationLeadInMs until (AnimationLeadInMs + animationPlaybackDurationMs)
    val isInThemeBHold =
        isDualThemeAnimationEnabled &&
            isAnimating &&
            animationElapsedMs >= (AnimationLeadInMs + animationPlaybackDurationMs) &&
            animationElapsedMs < animationSequenceDurationMs
    val splitProgressThreshold =
        if (animationPlaybackDurationMs > 0L) {
            (animationSplitDurationMs.toFloat() / animationPlaybackDurationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            1f
        }
    val livePlaybackProgress =
        if (animationPlaybackDurationMs > 0L) {
            (playbackElapsedMs.toFloat() / animationPlaybackDurationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    val activeTheme =
        if (
            isDualThemeAnimationEnabled &&
            ((isInPlayback && livePlaybackProgress >= splitProgressThreshold) || isInThemeBHold)
        ) {
            secondTheme
        } else {
            firstTheme
        }
    val glyphProgressDisplay =
        GlyphProgressDisplayModel(
            progress0To1 =
                when {
                    isInPlayback -> livePlaybackProgress
                    isInThemeBHold -> 1f
                    else -> 0f
                },
            isActive = isInPlayback,
            showIdleCoreRing = isInThemeBHold,
        )

    LaunchedEffect(animationStartNanos, animationSequenceDurationMs) {
        if (animationSequenceDurationMs <= 0L) {
            animationElapsedMs = 0L
            return@LaunchedEffect
        }
        while (true) {
            val frameTimeNanos = awaitFrame()
            val elapsedMs = ((frameTimeNanos - animationStartNanos).coerceAtLeast(0L)) / 1_000_000L
            animationElapsedMs = elapsedMs
            if (elapsedMs >= animationSequenceDurationMs) {
                animationSequenceDurationMs = 0L
                animationPlaybackDurationMs = 0L
                animationElapsedMs = 0L
                break
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        DebugAnimationHeader()
        DebugAnimationModeRow(
            animationThemeMode = animationThemeMode,
            onDualThemeAnimationEnabledChange = onDualThemeAnimationEnabledChange,
        )
        Spacer(modifier = Modifier.height(6.dp))
        AudioEncodeStatusSection(
            glyphProgressDisplay = glyphProgressDisplay,
            encodeProgressDisplay = null,
            glyphBaseSize = 132.dp,
            showGlyphCropGuide = true,
            glyphColorsOverride = audioEncodeGlyphColorsForFactionTheme(activeTheme),
        )
        Spacer(modifier = Modifier.height(8.dp))
        DebugAnimationTimingInputs(
            durationInput = durationInput,
            onDurationInputChange = { durationInput = sanitizeAnimationDurationInput(it) },
            splitDurationInput = splitDurationInput,
            onSplitDurationInputChange = { splitDurationInput = sanitizeAnimationDurationInput(it) },
            isDualThemeAnimationEnabled = isDualThemeAnimationEnabled,
            selectedThemeStyle = selectedThemeStyle,
        )
        DebugAnimationThemeSelectors(
            isDualThemeAnimationEnabled = isDualThemeAnimationEnabled,
            firstTheme = firstTheme,
            secondTheme = secondTheme,
            selectedBorderColor = accentTokens.selectionBorderAccentTint,
            onFirstThemeClick = { themePickerTarget = AnimationThemePickerTarget.First },
            onSecondThemeClick = { themePickerTarget = AnimationThemePickerTarget.Second },
        )
        DebugAnimationActions(
            isAnimating = isAnimating,
            progress = livePlaybackProgress,
            accentTokens = accentTokens,
            onStart = {
                val seconds = durationInput.toDoubleOrNull()?.coerceIn(0.1, 999.99) ?: return@DebugAnimationActions
                val splitSeconds =
                    if (isDualThemeAnimationEnabled) {
                        splitDurationInput.toDoubleOrNull()?.coerceIn(0.0, seconds) ?: return@DebugAnimationActions
                    } else {
                        seconds
                    }
                val playbackDurationMs = (seconds * 1000.0).toLong()
                val themeBHoldDurationMs =
                    if (isDualThemeAnimationEnabled) {
                        AnimationThemeBHoldMs
                    } else {
                        0L
                    }
                animationStartNanos = nanoTime()
                animationPlaybackDurationMs = playbackDurationMs
                animationSplitDurationMs = (splitSeconds * 1000.0).toLong()
                animationSequenceDurationMs = AnimationLeadInMs + playbackDurationMs + themeBHoldDurationMs
                animationElapsedMs = 0L
            },
            onStop = {
                animationSequenceDurationMs = 0L
                animationPlaybackDurationMs = 0L
                animationElapsedMs = 0L
            },
        )
    }
    themePickerTarget?.let { pickerTarget ->
        FactionThemePickerDialog(
            title =
                stringResource(
                    if (pickerTarget == AnimationThemePickerTarget.First) {
                        R.string.config_animation_theme_a_label
                    } else {
                        R.string.config_animation_theme_b_label
                    },
                ),
            options = availableThemes,
            selectedThemeId =
                if (pickerTarget == AnimationThemePickerTarget.First) {
                    firstTheme.id
                } else {
                    secondTheme.id
                },
            onDismiss = { themePickerTarget = null },
            onThemeSelected = { theme ->
                when (pickerTarget) {
                    AnimationThemePickerTarget.First -> firstThemeId = theme.id
                    AnimationThemePickerTarget.Second -> secondThemeId = theme.id
                }
                themePickerTarget = null
            },
        )
    }
}

@Composable
private fun DebugAnimationHeader() {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.config_animation_title),
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.config_animation_subtitle),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun DebugAnimationModeRow(
    animationThemeMode: AnimationThemeMode,
    onDualThemeAnimationEnabledChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.config_animation_mode_title),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.CenterVertically),
        )
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.weight(1f),
        ) {
            AnimationThemeMode.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == animationThemeMode,
                    onClick = { onDualThemeAnimationEnabledChange(option == AnimationThemeMode.Dual) },
                    shape =
                        SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = AnimationThemeMode.entries.size,
                        ),
                    colors = appSegmentedButtonColors(),
                ) {
                    Text(
                        text =
                            stringResource(
                                if (option == AnimationThemeMode.Single) {
                                    R.string.config_animation_mode_single
                                } else {
                                    R.string.config_animation_mode_dual
                                },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun DebugAnimationTimingInputs(
    durationInput: String,
    onDurationInputChange: (String) -> Unit,
    splitDurationInput: String,
    onSplitDurationInputChange: (String) -> Unit,
    isDualThemeAnimationEnabled: Boolean,
    selectedThemeStyle: ThemeStyleOption,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedTextField(
            value = durationInput,
            onValueChange = onDurationInputChange,
            label = { Text(stringResource(R.string.config_animation_duration_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = MaterialTheme.shapes.medium,
            colors = audioInputTextFieldColors(selectedThemeStyle),
            modifier = Modifier.weight(1f),
        )
        if (isDualThemeAnimationEnabled) {
            OutlinedTextField(
                value = splitDurationInput,
                onValueChange = onSplitDurationInputChange,
                label = { Text(stringResource(R.string.config_animation_first_part_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = MaterialTheme.shapes.medium,
                colors = audioInputTextFieldColors(selectedThemeStyle),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun DebugAnimationThemeSelectors(
    isDualThemeAnimationEnabled: Boolean,
    firstTheme: FactionThemeOption,
    secondTheme: FactionThemeOption,
    selectedBorderColor: Color,
    onFirstThemeClick: () -> Unit,
    onSecondThemeClick: () -> Unit,
) {
    if (isDualThemeAnimationEnabled) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AnimationThemeSelector(
                label = stringResource(R.string.config_animation_theme_a_label),
                theme = firstTheme,
                selectedBorderColor = selectedBorderColor,
                modifier = Modifier.weight(1f),
                onClick = onFirstThemeClick,
            )
            AnimationThemeSelector(
                label = stringResource(R.string.config_animation_theme_b_label),
                theme = secondTheme,
                selectedBorderColor = selectedBorderColor,
                modifier = Modifier.weight(1f),
                onClick = onSecondThemeClick,
            )
        }
    } else {
        AnimationThemeSelector(
            label = stringResource(R.string.config_animation_theme_label),
            theme = firstTheme,
            selectedBorderColor = selectedBorderColor,
            modifier = Modifier.fillMaxWidth(),
            onClick = onFirstThemeClick,
        )
    }
}

@Composable
private fun DebugAnimationActions(
    isAnimating: Boolean,
    progress: Float,
    accentTokens: AppThemeAccentTokens,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ActionButton(
            text = stringResource(R.string.config_animation_start),
            onClick = onStart,
            enabled = !isAnimating,
            textColor = accentTokens.disclosureAccentTint,
            borderColor = accentTokens.selectionBorderAccentTint,
            borderWidth = 2.dp,
            modifier = Modifier.weight(1f),
        )
        ActionButton(
            text = stringResource(R.string.config_animation_stop),
            onClick = onStop,
            enabled = isAnimating,
            borderColor = accentTokens.selectionBorderAccentTint,
            borderWidth = 2.dp,
            modifier = Modifier.weight(1f),
        )
    }
    if (isAnimating) {
        Text(
            text = "${(progress * 100f).roundToInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val AnimationLeadInMs = 1000L
private const val AnimationThemeBHoldMs = 2000L

private fun sanitizeAnimationDurationInput(input: String): String {
    val builder = StringBuilder()
    var hasDecimalSeparator = false
    var fractionalDigits = 0

    input.forEach { char ->
        when {
            char.isDigit() && !hasDecimalSeparator -> {
                if (builder.length < 3) {
                    builder.append(char)
                }
            }
            char.isDigit() && hasDecimalSeparator && fractionalDigits < 2 -> {
                builder.append(char)
                fractionalDigits += 1
            }
            char == '.' && !hasDecimalSeparator && builder.isNotEmpty() -> {
                builder.append(char)
                hasDecimalSeparator = true
            }
        }
    }

    return builder.toString()
}

private enum class AnimationThemePickerTarget {
    First,
    Second,
}

private enum class AnimationThemeMode {
    Single,
    Dual,
}

private fun buildAnimationThemeOptions(
    factionThemes: List<FactionThemeOption>,
    selectedFactionTheme: FactionThemeOption,
): List<FactionThemeOption> {
    val builtInThemes = if (factionThemes.isNotEmpty()) factionThemes else listOf(selectedFactionTheme)
    return if (builtInThemes.any { it.id == selectedFactionTheme.id }) {
        builtInThemes
    } else {
        listOf(selectedFactionTheme) + builtInThemes
    }
}

@Composable
private fun AnimationThemeSelector(
    label: String,
    theme: FactionThemeOption,
    selectedBorderColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, selectedBorderColor.copy(alpha = 0.7f)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = factionThemeDisplayName(theme),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                FactionThemePreviewSwatch(theme = theme, modifier = Modifier.width(72.dp))
            }
        }
    }
}

@Composable
private fun FactionThemePickerDialog(
    title: String,
    options: List<FactionThemeOption>,
    selectedThemeId: String,
    onDismiss: () -> Unit,
    onThemeSelected: (FactionThemeOption) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Column(
                    modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    options.forEach { option ->
                        val isSelected = option.id == selectedThemeId
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { onThemeSelected(option) },
                            shape = RoundedCornerShape(16.dp),
                            tonalElevation = 0.dp,
                            border =
                                BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color =
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                                        },
                                ),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = factionThemeDisplayName(option),
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                )
                                FactionThemePreviewSwatch(
                                    theme = option,
                                    modifier = Modifier.width(84.dp),
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(R.string.common_cancel))
                    }
                }
            }
        }
    }
}

@Composable
private fun FactionThemePreviewSwatch(
    theme: FactionThemeOption,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .height(28.dp)
                .border(
                    BorderStroke(2.dp, theme.outlineColor),
                    shape = RoundedCornerShape(10.dp),
                ),
    ) {
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(28.dp)
                    .background(
                        color = theme.primaryColor,
                        shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                    ),
        )
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(28.dp)
                    .background(
                        color = theme.secondaryColor,
                        shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                    ),
        )
    }
}

@Composable
private fun factionThemeDisplayName(theme: FactionThemeOption): String = theme.titleOverride ?: stringResource(theme.titleResId)
