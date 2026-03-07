#pragma once

#include <atomic>
#include <cstdlib>
#include <filesystem>
#include <string>
#include <vector>

#include "test_fs.h"

#ifdef _WIN32
#include <windows.h>
#endif

namespace test {

struct ProcessResult {
    int exit_code = -1;
    std::string output;
};

inline std::string QuoteForShell(const std::string& value) {
    std::string quoted = "\"";
    for (char ch : value) {
        if (ch == '"') {
            quoted += "\"\"";
        } else {
            quoted.push_back(ch);
        }
    }
    quoted.push_back('"');
    return quoted;
}

#ifdef _WIN32
inline std::wstring ToWide(const std::string& value) {
    if (value.empty()) {
        return {};
    }

    const int required_size =
        MultiByteToWideChar(CP_UTF8, 0, value.c_str(), -1, nullptr, 0);
    if (required_size <= 0) {
        return std::wstring(value.begin(), value.end());
    }

    std::wstring wide(static_cast<size_t>(required_size), L'\0');
    MultiByteToWideChar(CP_UTF8, 0, value.c_str(), -1, wide.data(), required_size);
    if (!wide.empty() && wide.back() == L'\0') {
        wide.pop_back();
    }
    return wide;
}

inline std::wstring QuoteForCommandLine(const std::wstring& value) {
    std::wstring quoted = L"\"";
    for (wchar_t ch : value) {
        if (ch == L'"') {
            quoted += L'\\';
        }
        quoted.push_back(ch);
    }
    quoted.push_back(L'"');
    return quoted;
}
#endif

inline ProcessResult RunProcess(const std::filesystem::path& executable,
                                const std::vector<std::string>& args,
                                const std::filesystem::path& scratch_dir) {
    ProcessResult result{};
#ifdef _WIN32
    SECURITY_ATTRIBUTES security_attributes{};
    security_attributes.nLength = sizeof(security_attributes);
    security_attributes.bInheritHandle = TRUE;

    HANDLE read_pipe = nullptr;
    HANDLE write_pipe = nullptr;
    if (!CreatePipe(&read_pipe, &write_pipe, &security_attributes, 0)) {
        result.output = "CreatePipe failed.";
        return result;
    }

    if (!SetHandleInformation(read_pipe, HANDLE_FLAG_INHERIT, 0)) {
        CloseHandle(read_pipe);
        CloseHandle(write_pipe);
        result.output = "SetHandleInformation failed.";
        return result;
    }

    STARTUPINFOW startup_info{};
    startup_info.cb = sizeof(startup_info);
    startup_info.dwFlags = STARTF_USESTDHANDLES;
    startup_info.hStdInput = GetStdHandle(STD_INPUT_HANDLE);
    startup_info.hStdOutput = write_pipe;
    startup_info.hStdError = write_pipe;

    std::wstring command_line = QuoteForCommandLine(executable.wstring());
    for (const auto& arg : args) {
        command_line += L" ";
        command_line += QuoteForCommandLine(ToWide(arg));
    }

    std::vector<wchar_t> mutable_command(command_line.begin(), command_line.end());
    mutable_command.push_back(L'\0');

    PROCESS_INFORMATION process_info{};
    const BOOL create_ok = CreateProcessW(
        nullptr,
        mutable_command.data(),
        nullptr,
        nullptr,
        TRUE,
        CREATE_NO_WINDOW,
        nullptr,
        nullptr,
        &startup_info,
        &process_info);

    CloseHandle(write_pipe);

    if (!create_ok) {
        result.output = "CreateProcess failed with error " + std::to_string(GetLastError()) + ".";
        CloseHandle(read_pipe);
        return result;
    }

    char buffer[4096];
    DWORD bytes_read = 0;
    while (ReadFile(read_pipe, buffer, sizeof(buffer), &bytes_read, nullptr) && bytes_read > 0) {
        result.output.append(buffer, buffer + bytes_read);
    }

    WaitForSingleObject(process_info.hProcess, INFINITE);
    DWORD exit_code = 0;
    GetExitCodeProcess(process_info.hProcess, &exit_code);
    result.exit_code = static_cast<int>(exit_code);

    CloseHandle(process_info.hThread);
    CloseHandle(process_info.hProcess);
    CloseHandle(read_pipe);
#else
    static std::atomic<unsigned long long> next_capture_id{0};
    std::filesystem::create_directories(scratch_dir);

    const auto capture_path =
        scratch_dir / ("process_" + std::to_string(next_capture_id.fetch_add(1)) + ".txt");

    std::string command = QuoteForShell(executable.string());
    for (const auto& arg : args) {
        command += " ";
        command += QuoteForShell(arg);
    }
    command += " > " + QuoteForShell(capture_path.string()) + " 2>&1";

    result.exit_code = std::system(command.c_str());
    if (std::filesystem::exists(capture_path)) {
        result.output = ReadTextFile(capture_path);
    }
#endif
    return result;
}

}  // namespace test
