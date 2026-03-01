#include <filesystem>
#include <fstream>
#include <iostream>
#include <stdexcept>
#include <string>

#include "bag/common/version.h"
#include "decoder.h"
#include "encoder.h"

namespace {
constexpr char kCliPresentationVersion[] = "0.1.0";

void print_versions() {
    std::cout << "presentation: v" << kCliPresentationVersion << "\n"
              << "core: v" << bag::CoreVersion() << "\n";
}

void print_usage() {
    std::cout << "Usage:\n"
              << "  binary_audio_cpp encode --text <TEXT> --out <OUTPUT.wav>\n"
              << "  binary_audio_cpp encode --text-file <FILE> --out <OUTPUT.wav>\n"
              << "  binary_audio_cpp decode --in <INPUT.wav> [--out-text <OUTPUT.txt>]\n"
              << "  binary_audio_cpp version\n"
              << "  binary_audio_cpp --version\n";
}

std::string read_text_file(const std::filesystem::path& path) {
    std::ifstream file(path, std::ios::binary);
    if (!file) {
        throw std::runtime_error("Failed to open text file: " + path.string());
    }
    std::string content((std::istreambuf_iterator<char>(file)),
                        std::istreambuf_iterator<char>());
    return content;
}

std::filesystem::path normalize_output_path(const std::string& raw,
                                            const std::string& default_name,
                                            const std::string& extension) {
    std::filesystem::path path(raw);
    bool ends_with_sep = !raw.empty() &&
                         (raw.back() == '/' || raw.back() == '\\');

    if (std::filesystem::exists(path) && std::filesystem::is_directory(path)) {
        path /= default_name;
    } else if (ends_with_sep) {
        path /= default_name;
    } else if (!path.has_extension()) {
        path.replace_extension(extension);
    }
    return path;
}
}  // namespace

int main(int argc, char* argv[]) {
    if (argc < 2) {
        print_usage();
        return 1;
    }

    std::string command = argv[1];
    if (command == "version" || command == "--version") {
        print_versions();
        return 0;
    }

    try {
        if (command == "encode") {
            std::string text;
            std::string text_file;
            std::string out = "data/output_audio/output.wav";

            for (int i = 2; i < argc; ++i) {
                std::string arg = argv[i];
                if (arg == "--text" && i + 1 < argc) {
                    text = argv[++i];
                } else if (arg == "--text-file" && i + 1 < argc) {
                    text_file = argv[++i];
                } else if (arg == "--out" && i + 1 < argc) {
                    out = argv[++i];
                } else {
                    print_usage();
                    return 1;
                }
            }

            if ((!text.empty()) == (!text_file.empty())) {
                std::cerr << "Please provide exactly one of --text or --text-file.\n";
                return 1;
            }

            if (!text_file.empty()) {
                text = read_text_file(text_file);
            }

            auto output_path = normalize_output_path(out, "output.wav", ".wav");
            encode_text_to_wav(text, output_path);
            std::cout << "Output WAV: " << output_path.string() << "\n";
        } else if (command == "decode") {
            std::string input;
            std::string out_text;

            for (int i = 2; i < argc; ++i) {
                std::string arg = argv[i];
                if (arg == "--in" && i + 1 < argc) {
                    input = argv[++i];
                } else if (arg == "--out-text" && i + 1 < argc) {
                    out_text = argv[++i];
                } else {
                    print_usage();
                    return 1;
                }
            }

            if (input.empty()) {
                print_usage();
                return 1;
            }

            std::string decoded = decode_wav_to_text(input);
            if (!out_text.empty()) {
                auto output_path = normalize_output_path(out_text, "decoded.txt", ".txt");
                auto parent = output_path.parent_path();
                if (!parent.empty()) {
                    std::filesystem::create_directories(parent);
                }
                std::ofstream out_file(output_path, std::ios::binary);
                if (!out_file) {
                    throw std::runtime_error("Failed to write output text file.");
                }
                out_file << decoded;
            }
            std::cout << decoded << "\n";
        } else {
            print_usage();
            return 1;
        }
    } catch (const std::exception& ex) {
        std::cerr << "Error: " << ex.what() << "\n";
        return 1;
    }

    return 0;
}
