namespace bag::mini_mode {

using std::uint8_t;

namespace {

char NormalizeMorseChar(char ch) {
  const unsigned char value = static_cast<unsigned char>(ch);
  if (value >= 'a' && value <= 'z') {
    return static_cast<char>('A' + (value - 'a'));
  }
  return static_cast<char>(value);
}

}  // namespace

std::string_view MorsePatternForChar(char ch) {
  switch (NormalizeMorseChar(ch)) {
    case 'A':
      return ".-";
    case 'B':
      return "-...";
    case 'C':
      return "-.-.";
    case 'D':
      return "-..";
    case 'E':
      return ".";
    case 'F':
      return "..-.";
    case 'G':
      return "--.";
    case 'H':
      return "....";
    case 'I':
      return "..";
    case 'J':
      return ".---";
    case 'K':
      return "-.-";
    case 'L':
      return ".-..";
    case 'M':
      return "--";
    case 'N':
      return "-.";
    case 'O':
      return "---";
    case 'P':
      return ".--.";
    case 'Q':
      return "--.-";
    case 'R':
      return ".-.";
    case 'S':
      return "...";
    case 'T':
      return "-";
    case 'U':
      return "..-";
    case 'V':
      return "...-";
    case 'W':
      return ".--";
    case 'X':
      return "-..-";
    case 'Y':
      return "-.--";
    case 'Z':
      return "--..";
    case '0':
      return "-----";
    case '1':
      return ".----";
    case '2':
      return "..---";
    case '3':
      return "...--";
    case '4':
      return "....-";
    case '5':
      return ".....";
    case '6':
      return "-....";
    case '7':
      return "--...";
    case '8':
      return "---..";
    case '9':
      return "----.";
    case '.':
      return ".-.-.-";
    case ',':
      return "--..--";
    case '?':
      return "..--..";
    case '\'':
      return ".----.";
    case '!':
      return "-.-.--";
    case '/':
      return "-..-.";
    case '(':
      return "-.--.";
    case ')':
      return "-.--.-";
    case '&':
      return ".-...";
    case ':':
      return "---...";
    case ';':
      return "-.-.-.";
    case '=':
      return "-...-";
    case '+':
      return ".-.-.";
    case '-':
      return "-....-";
    case '_':
      return "..--.-";
    case '"':
      return ".-..-.";
    case '$':
      return "...-..-";
    case '@':
      return ".--.-.";
    case ' ':
      return "";
    default:
      return {};
  }
}

char CharForMorsePattern(std::string_view pattern) {
  static constexpr std::pair<std::string_view, char> kReverseTable[] = {
      {".-", 'A'},    {"-...", 'B'},  {"-.-.", 'C'},  {"-..", 'D'},
      {".", 'E'},     {"..-.", 'F'},  {"--.", 'G'},   {"....", 'H'},
      {"..", 'I'},    {".---", 'J'},  {"-.-", 'K'},   {".-..", 'L'},
      {"--", 'M'},    {"-.", 'N'},    {"---", 'O'},   {".--.", 'P'},
      {"--.-", 'Q'},  {".-.", 'R'},   {"...", 'S'},   {"-", 'T'},
      {"..-", 'U'},   {"...-", 'V'},  {".--", 'W'},   {"-..-", 'X'},
      {"-.--", 'Y'},  {"--..", 'Z'},  {"-----", '0'}, {".----", '1'},
      {"..---", '2'}, {"...--", '3'}, {"....-", '4'}, {".....", '5'},
      {"-....", '6'}, {"--...", '7'}, {"---..", '8'}, {"----.", '9'},
      {".-.-.-", '.'}, {"--..--", ','}, {"..--..", '?'}, {".----.", '\''},
      {"-.-.--", '!'}, {"-..-.", '/'}, {"-.--.", '('}, {"-.--.-", ')'},
      {".-...", '&'}, {"---...", ':'}, {"-.-.-.", ';'}, {"-...-", '='},
      {".-.-.", '+'}, {"-....-", '-'}, {"..--.-", '_'}, {".-..-.", '"'},
      {"...-..-", '$'}, {".--.-.", '@'},
  };
  for (const auto& entry : kReverseTable) {
    if (entry.first == pattern) {
      return entry.second;
    }
  }
  return '\0';
}

bool IsMorseText(std::string_view text) {
  bool previous_was_space = false;
  for (char ch : text) {
    const char normalized = NormalizeMorseChar(ch);
    if (static_cast<unsigned char>(normalized) > 0x7F) {
      return false;
    }
    if (normalized == ' ') {
      if (previous_was_space) {
        continue;
      }
      previous_was_space = true;
      continue;
    }
    previous_was_space = false;
    if (MorsePatternForChar(normalized).empty()) {
      return false;
    }
  }
  return true;
}

ErrorCode EncodeTextToPayload(const std::string& text,
                              std::vector<uint8_t>* out_payload) {
  if (out_payload == nullptr || !IsMorseText(text)) {
    return ErrorCode::kInvalidArgument;
  }

  out_payload->clear();
  out_payload->reserve(text.size());
  bool previous_was_space = false;
  for (char ch : text) {
    const char normalized = NormalizeMorseChar(ch);
    if (normalized == ' ') {
      if (!previous_was_space && !out_payload->empty()) {
        out_payload->push_back(static_cast<uint8_t>(' '));
      }
      previous_was_space = true;
      continue;
    }
    previous_was_space = false;
    out_payload->push_back(static_cast<uint8_t>(normalized));
  }
  if (!out_payload->empty() && out_payload->back() == static_cast<uint8_t>(' ')) {
    out_payload->pop_back();
  }
  return ErrorCode::kOk;
}

ErrorCode DecodePayloadToText(const std::vector<uint8_t>& payload,
                              std::string* out_text) {
  if (out_text == nullptr) {
    return ErrorCode::kInvalidArgument;
  }
  for (uint8_t value : payload) {
    const char ch = static_cast<char>(value);
    if (ch != ' ' && MorsePatternForChar(ch).empty()) {
      return ErrorCode::kInvalidArgument;
    }
  }
  out_text->assign(payload.begin(), payload.end());
  return ErrorCode::kOk;
}

std::size_t MorseUnitCountForPayloadByte(uint8_t value) {
  const char ch = static_cast<char>(value);
  if (ch == ' ') {
    return kMorseWordGapUnits;
  }
  const std::string_view pattern = MorsePatternForChar(ch);
  if (pattern.empty()) {
    return 0;
  }
  std::size_t units = 0;
  for (std::size_t index = 0; index < pattern.size(); ++index) {
    units += pattern[index] == '-' ? kMorseDashUnits : kMorseDotUnits;
    if (index + 1 < pattern.size()) {
      units += kMorseElementGapUnits;
    }
  }
  return units;
}

std::size_t MorseTrailingGapUnits(const std::vector<uint8_t>& payload,
                                  std::size_t byte_index) {
  if (byte_index + 1 >= payload.size() ||
      payload[byte_index] == static_cast<uint8_t>(' ') ||
      payload[byte_index + 1] == static_cast<uint8_t>(' ')) {
    return 0;
  }
  return kMorseLetterGapUnits;
}

}  // namespace bag::mini_mode
