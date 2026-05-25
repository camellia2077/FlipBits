namespace bag {
namespace {

std::size_t SafeFrameSamples(const CoreConfig& config) {
  return config.frame_samples > 0
             ? static_cast<std::size_t>(config.frame_samples)
             : static_cast<std::size_t>(0);
}

std::size_t CountMiniUnits(const std::string& text) {
  auto morse_code_point_size = [](unsigned char lead) -> std::size_t {
    if ((lead & 0x80U) == 0) {
      return 1;
    }
    if ((lead & 0xE0U) == 0xC0U) {
      return 2;
    }
    if ((lead & 0xF0U) == 0xE0U) {
      return 3;
    }
    if ((lead & 0xF8U) == 0xF0U) {
      return 4;
    }
    return 1;
  };

  auto upper_ascii = [](char ch) {
    return (ch >= 'a' && ch <= 'z') ? static_cast<char>('A' + (ch - 'a')) : ch;
  };

  auto symbol_units = [&](char ch) -> std::size_t {
    switch (upper_ascii(ch)) {
      case 'A':
        return 8;
      case 'B':
        return 12;
      case 'C':
        return 14;
      case 'D':
        return 10;
      case 'E':
        return 4;
      case 'F':
        return 12;
      case 'G':
        return 12;
      case 'H':
        return 10;
      case 'I':
        return 6;
      case 'J':
        return 16;
      case 'K':
        return 12;
      case 'L':
        return 12;
      case 'M':
        return 10;
      case 'N':
        return 8;
      case 'O':
        return 14;
      case 'P':
        return 14;
      case 'Q':
        return 16;
      case 'R':
        return 10;
      case 'S':
        return 8;
      case 'T':
        return 6;
      case 'U':
        return 10;
      case 'V':
        return 12;
      case 'W':
        return 12;
      case 'X':
        return 14;
      case 'Y':
        return 16;
      case 'Z':
        return 14;
      case '0':
        return 22;
      case '1':
        return 20;
      case '2':
        return 18;
      case '3':
        return 16;
      case '4':
        return 14;
      case '5':
        return 12;
      case '6':
        return 14;
      case '7':
        return 16;
      case '8':
        return 18;
      case '9':
        return 20;
      case '.':
      case ',':
      case '?':
      case '\'':
      case '!':
      case '/':
      case '(':
      case ')':
      case '&':
      case ':':
      case ';':
      case '=':
      case '+':
      case '-':
      case '_':
      case '"':
      case '$':
      case '@':
        return 20;
      case ' ':
        return 7;
      default:
        return 4;
    }
  };

  std::size_t units = 0;
  for (std::size_t index = 0; index < text.size();) {
    const unsigned char lead = static_cast<unsigned char>(text[index]);
    const std::size_t code_point_size = morse_code_point_size(lead);
    units += symbol_units(text[index]);
    index += std::min(code_point_size, text.size() - index);
  }
  return units;
}

}  // namespace

EncodeWorkPlan BuildEncodeWorkPlan(const CoreConfig& config,
                                   const std::string& text) {
  EncodeWorkPlan plan{};
  plan.payload_byte_count = text.size();
  plan.segment_count = 1;

  switch (config.mode) {
    case TransportMode::kFlash: {
      const std::vector<std::uint8_t> bytes(text.begin(), text.end());
      const std::size_t frame_samples = SafeFrameSamples(config);
      const std::size_t prepare_text_work =
          std::max<std::size_t>(bytes.size(), 1);
      const std::size_t prepare_layout_work =
          std::max<std::size_t>(bytes.size() * 2, 1);
      const std::size_t render_work = std::max<std::size_t>(
          bytes.size() * 8 * std::max<std::size_t>(frame_samples, 1), 1);
      plan.preparing_input_work_units = prepare_text_work + prepare_layout_work;
      plan.rendering_pcm_work_units = render_work;
      plan.postprocessing_work_units = 1;
      plan.finalizing_work_units = 1;
      plan.total_work_units =
          plan.preparing_input_work_units + plan.rendering_pcm_work_units +
          plan.postprocessing_work_units + plan.finalizing_work_units;
      plan.estimated_pcm_sample_count = render_work;
      break;
    }
    case TransportMode::kPro: {
      const std::size_t payload_size = text.size();
      const std::size_t symbol_count = payload_size * 2;
      const std::size_t render_work = std::max<std::size_t>(
          symbol_count * std::max<std::size_t>(SafeFrameSamples(config), 1), 1);
      plan.preparing_input_work_units = std::max<std::size_t>(payload_size, 1) +
                                        std::max<std::size_t>(symbol_count, 1);
      plan.rendering_pcm_work_units = render_work;
      plan.postprocessing_work_units = 0;
      plan.finalizing_work_units = 1;
      plan.total_work_units = plan.preparing_input_work_units +
                              plan.rendering_pcm_work_units +
                              plan.finalizing_work_units;
      plan.estimated_pcm_sample_count = render_work;
      break;
    }
    case TransportMode::kUltra: {
      const std::size_t payload_size = text.size();
      const std::size_t frame_size = payload_size + 4;
      const std::size_t symbol_count = frame_size * 2;
      const std::size_t render_work = std::max<std::size_t>(
          symbol_count * std::max<std::size_t>(SafeFrameSamples(config), 1), 1);
      plan.preparing_input_work_units =
          std::max<std::size_t>(payload_size, 1) +
          std::max<std::size_t>(frame_size + symbol_count, 1);
      plan.rendering_pcm_work_units = render_work;
      plan.postprocessing_work_units = 0;
      plan.finalizing_work_units = 1;
      plan.total_work_units = plan.preparing_input_work_units +
                              plan.rendering_pcm_work_units +
                              plan.finalizing_work_units;
      plan.estimated_pcm_sample_count = render_work;
      break;
    }
    case TransportMode::kMini: {
      const std::size_t units = CountMiniUnits(text);
      const std::size_t render_work = std::max<std::size_t>(
          units * std::max<std::size_t>(SafeFrameSamples(config), 1), 1);
      plan.preparing_input_work_units = std::max<std::size_t>(text.size(), 1);
      plan.rendering_pcm_work_units = render_work;
      plan.postprocessing_work_units = 0;
      plan.finalizing_work_units = 1;
      plan.total_work_units = plan.preparing_input_work_units +
                              plan.rendering_pcm_work_units +
                              plan.finalizing_work_units;
      plan.estimated_pcm_sample_count = render_work;
      break;
    }
    default:
      plan.finalizing_work_units = 1;
      plan.total_work_units = 1;
      break;
  }
  return plan;
}

}  // namespace bag
