#pragma once

#if !defined(FLIPBITS_TEST_IMPORT_STD) || !FLIPBITS_HAS_STD_MODULE_PROVIDER
#include <functional>
#include <iostream>
#include <sstream>
#include <stdexcept>
#include <string>
#include <string_view>
#include <type_traits>
#include <vector>
#endif

namespace test {

namespace detail {

inline std::string EscapeString(std::string_view value) {
    std::string out;
    out.reserve(value.size() + 2);
    for (const char ch : value) {
        switch (ch) {
            case '\\':
                out += "\\\\";
                break;
            case '\n':
                out += "\\n";
                break;
            case '\r':
                out += "\\r";
                break;
            case '\t':
                out += "\\t";
                break;
            case '"':
                out += "\\\"";
                break;
            default:
                out.push_back(ch);
                break;
        }
    }
    return out;
}

template <typename T>
std::string ToDebugString(const T& value) {
    using Decayed = std::decay_t<T>;
    if constexpr (std::is_same_v<Decayed, std::string>) {
        return "\"" + EscapeString(value) + "\"";
    } else if constexpr (std::is_same_v<Decayed, std::string_view>) {
        return "\"" + EscapeString(value) + "\"";
    } else if constexpr (std::is_same_v<Decayed, const char*> ||
                         std::is_same_v<Decayed, char*>) {
        return value == nullptr ? "null" : "\"" + EscapeString(value) + "\"";
    } else if constexpr (std::is_same_v<Decayed, char>) {
        return "'" + EscapeString(std::string(1, value)) + "'";
    } else if constexpr (std::is_same_v<Decayed, bool>) {
        return value ? "true" : "false";
    } else if constexpr (std::is_enum_v<Decayed>) {
        using Underlying = std::underlying_type_t<Decayed>;
        if constexpr (std::is_signed_v<Underlying>) {
            return std::to_string(static_cast<long long>(value));
        } else {
            return std::to_string(static_cast<unsigned long long>(value));
        }
    } else if constexpr (std::is_integral_v<Decayed>) {
        if constexpr (std::is_signed_v<Decayed>) {
            return std::to_string(static_cast<long long>(value));
        } else {
            return std::to_string(static_cast<unsigned long long>(value));
        }
    } else if constexpr (std::is_floating_point_v<Decayed>) {
        std::ostringstream stream;
        stream << value;
        return stream.str();
    } else if constexpr (requires(std::ostringstream& stream, const Decayed& item) {
                             stream << item;
                         }) {
        std::ostringstream stream;
        stream << value;
        return stream.str();
    } else {
        return "<unprintable>";
    }
}

inline std::string MergeContext(const std::string& message, const std::string& context) {
    if (context.empty()) {
        return message;
    }
    return message + " [" + context + "]";
}

}  // namespace detail

class Runner {
public:
    using TestFn = std::function<void()>;

    void Add(const std::string& name, TestFn fn) {
        tests_.push_back({name, std::move(fn)});
    }

    int Run() const {
        int failed = 0;
        for (const auto& test : tests_) {
            try {
                test.fn();
                std::cout << "[PASS] " << test.name << "\n";
            } catch (const std::exception& ex) {
                ++failed;
                std::cerr << "[FAIL] " << test.name << " :: " << ex.what() << "\n";
            } catch (...) {
                ++failed;
                std::cerr << "[FAIL] " << test.name << " :: unknown error\n";
            }
        }
        std::cout << "Total: " << tests_.size() << ", Failed: " << failed << "\n";
        return failed == 0 ? 0 : 1;
    }

private:
    struct TestCase {
        std::string name;
        TestFn fn;
    };

    std::vector<TestCase> tests_;
};

inline void Fail(const std::string& message) {
    throw std::runtime_error(message);
}

template <typename T, typename U>
inline void AssertEq(const T& lhs, const U& rhs, const std::string& message) {
    if (!(lhs == rhs)) {
        Fail(message + " expected=" + detail::ToDebugString(rhs) +
             " actual=" + detail::ToDebugString(lhs));
    }
}

template <typename T, typename U>
inline void AssertEqWithContext(const T& lhs,
                                const U& rhs,
                                const std::string& message,
                                const std::string& context) {
    AssertEq(lhs, rhs, detail::MergeContext(message, context));
}

inline void AssertTrue(bool value, const std::string& message) {
    if (!value) {
        Fail(message);
    }
}

inline void AssertContains(const std::string& haystack,
                           const std::string& needle,
                           const std::string& message) {
    if (haystack.find(needle) == std::string::npos) {
        Fail(message);
    }
}

template <typename Fn>
inline void AssertThrows(Fn&& fn, const std::string& message) {
    try {
        fn();
    } catch (...) {
        return;
    }
    Fail(message);
}

}  // namespace test
