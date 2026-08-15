#include "svg_validator.hpp"

#include <pugixml.hpp>

#include <algorithm>
#include <cctype>
#include <charconv>
#include <cmath>
#include <cstddef>
#include <regex>
#include <sstream>
#include <string>
#include <unordered_map>
#include <unordered_set>
#include <utility>
#include <vector>

namespace ya_brander::components {
namespace {

constexpr std::size_t kMaxSvgBytes = 256 * 1024;
constexpr std::size_t kMaxElements = 500;
constexpr std::size_t kMaxNesting = 16;
constexpr std::size_t kMaxTextLength = 1000;
constexpr std::string_view kSvgNamespace = "http://www.w3.org/2000/svg";

const std::unordered_set<std::string> kAllowedElements{
    "svg",     "g",    "path",           "rect",           "circle",
    "ellipse", "line", "polyline",       "polygon",        "text",
    "tspan",   "defs", "linearGradient", "radialGradient", "stop",
};

const std::unordered_set<std::string> kVisualElements{
    "path", "rect", "circle", "ellipse", "line", "polyline", "polygon", "text",
};

const std::unordered_set<std::string> kGeometryAttributes{
    "x", "y",  "x1", "y1",    "x2",     "y2", "cx",     "cy",
    "r", "rx", "ry", "width", "height", "d",  "points", "offset",
};

const std::unordered_set<std::string> kPresentationAttributes{
    "fill",         "fill-opacity",   "stroke",          "stroke-opacity",
    "stroke-width", "stroke-linecap", "stroke-linejoin", "opacity",
    "font-family",  "font-size",      "font-weight",     "font-style",
    "text-anchor",  "stop-color",     "stop-opacity",
};

const std::unordered_set<std::string> kScalarNumberAttributes{
    "x",
    "y",
    "x1",
    "y1",
    "x2",
    "y2",
    "cx",
    "cy",
    "r",
    "rx",
    "ry",
    "width",
    "height",
    "offset",
    "fill-opacity",
    "stroke-opacity",
    "stroke-width",
    "opacity",
    "font-size",
    "stop-opacity",
};

const std::unordered_set<std::string> kNonNegativeAttributes{
    "r", "rx", "ry", "width", "height", "stroke-width", "font-size",
};

const std::unordered_set<std::string> kOpacityAttributes{
    "fill-opacity", "stroke-opacity", "opacity", "stop-opacity", "offset",
};

std::string NodeName(const pugi::xml_node& node) { return node.name(); }

std::string AttributeValue(const pugi::xml_node& node, const char* name) {
  return node.attribute(name).value();
}

bool HasAttribute(const pugi::xml_node& node, const char* name) {
  return static_cast<bool>(node.attribute(name));
}

void SkipSpaces(std::string_view value, std::size_t& position) {
  while (position < value.size()) {
    const unsigned char character = static_cast<unsigned char>(value[position]);
    if (character != ' ' && character != '\t' && character != '\r' &&
        character != '\n') {
      break;
    }
    ++position;
  }
}

bool ParseNumberAt(std::string_view value, std::size_t& position,
                   double& result) {
  const auto original_position = position;
  if (position < value.size() && value[position] == '+') {
    ++position;
  }
  if (position >= value.size()) {
    position = original_position;
    return false;
  }

  const char* begin = value.data() + position;
  const char* end = value.data() + value.size();
  const auto parsed =
      std::from_chars(begin, end, result, std::chars_format::general);
  if (parsed.ec != std::errc{} || parsed.ptr == begin ||
      !std::isfinite(result)) {
    position = original_position;
    return false;
  }
  position = static_cast<std::size_t>(parsed.ptr - value.data());
  return true;
}

bool ParseNumberList(std::string_view value, std::vector<double>& numbers) {
  std::size_t position = 0;
  SkipSpaces(value, position);
  if (position == value.size() || value[position] == ',') {
    return false;
  }

  while (position < value.size()) {
    double number = 0.0;
    if (!ParseNumberAt(value, position, number)) {
      return false;
    }
    numbers.push_back(number);

    const auto end_of_number = position;
    SkipSpaces(value, position);
    const bool has_whitespace = position != end_of_number;
    if (position == value.size()) {
      return true;
    }
    if (value[position] == ',') {
      ++position;
      SkipSpaces(value, position);
      if (position == value.size() || value[position] == ',') {
        return false;
      }
    } else if (!has_whitespace && value[position] != '+' &&
               value[position] != '-') {
      return false;
    }
  }
  return !numbers.empty();
}

bool IsValidId(std::string_view id) {
  static const std::regex pattern("^[a-z][a-z0-9-]{0,63}$");
  return std::regex_match(id.begin(), id.end(), pattern);
}

bool IsValidColor(std::string_view value, std::string* gradient_id) {
  static const std::regex hex_pattern(
      "^#(?:[0-9A-Fa-f]{3}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$");
  static const std::regex gradient_pattern(
      "^url\\(#([a-z][a-z0-9-]{0,63})\\)$");
  if (value == "none" ||
      std::regex_match(value.begin(), value.end(), hex_pattern)) {
    return true;
  }

  std::match_results<std::string_view::const_iterator> match;
  if (!std::regex_match(value.begin(), value.end(), match, gradient_pattern)) {
    return false;
  }
  if (gradient_id != nullptr) {
    *gradient_id = std::string(match[1].first, match[1].second);
  }
  return true;
}

bool IsValidPath(std::string_view value) {
  constexpr std::string_view kCommands = "MmLlHhVvCcSsQqTtAaZz";
  const auto required_parameters = [](const char command) {
    switch (
        static_cast<char>(std::toupper(static_cast<unsigned char>(command)))) {
      case 'M':
      case 'L':
      case 'T':
        return 2U;
      case 'H':
      case 'V':
        return 1U;
      case 'C':
        return 6U;
      case 'S':
      case 'Q':
        return 4U;
      case 'A':
        return 7U;
      case 'Z':
        return 0U;
      default:
        return 0U;
    }
  };
  const auto command_is_complete = [&required_parameters](
                                       const char command,
                                       const std::size_t parameter_count) {
    const auto required = required_parameters(command);
    return required == 0
               ? parameter_count == 0
               : parameter_count >= required && parameter_count % required == 0;
  };

  bool has_command = false;
  bool comma_pending = false;
  bool previous_was_number = false;
  char current_command = '\0';
  std::size_t parameter_count = 0;
  std::size_t position = 0;
  while (position < value.size()) {
    const auto before_spaces = position;
    SkipSpaces(value, position);
    const bool has_whitespace = position != before_spaces;
    if (position < value.size() && value[position] == ',') {
      if (!previous_was_number || comma_pending) {
        return false;
      }
      comma_pending = true;
      previous_was_number = false;
      ++position;
      continue;
    }
    if (position == value.size()) {
      break;
    }
    if (kCommands.find(value[position]) != std::string_view::npos) {
      if (comma_pending ||
          (has_command &&
           !command_is_complete(current_command, parameter_count))) {
        return false;
      }
      if (!has_command && value[position] != 'M' && value[position] != 'm') {
        return false;
      }
      current_command = value[position];
      parameter_count = 0;
      has_command = true;
      previous_was_number = false;
      ++position;
      continue;
    }

    if (!has_command || required_parameters(current_command) == 0 ||
        (previous_was_number && !has_whitespace && !comma_pending &&
         value[position] != '+' && value[position] != '-')) {
      return false;
    }
    double number = 0.0;
    if (!ParseNumberAt(value, position, number)) {
      return false;
    }
    ++parameter_count;
    comma_pending = false;
    previous_was_number = true;
  }
  return has_command && !comma_pending &&
         command_is_complete(current_command, parameter_count);
}

bool IsValidTransform(std::string_view value) {
  const std::unordered_map<std::string, int> operation_order{
      {"translate", 1}, {"rotate", 2}, {"scale", 3}};
  int previous_order = 0;
  bool has_operation = false;
  std::size_t position = 0;

  while (true) {
    SkipSpaces(value, position);
    if (position == value.size()) {
      return has_operation;
    }

    const auto name_start = position;
    while (position < value.size() &&
           ((value[position] >= 'A' && value[position] <= 'Z') ||
            (value[position] >= 'a' && value[position] <= 'z'))) {
      ++position;
    }
    const std::string name(value.substr(name_start, position - name_start));
    const auto order = operation_order.find(name);
    if (order == operation_order.end() || order->second <= previous_order) {
      return false;
    }

    SkipSpaces(value, position);
    if (position == value.size() || value[position] != '(') {
      return false;
    }
    const auto arguments_start = ++position;
    const auto closing_parenthesis = value.find(')', position);
    if (closing_parenthesis == std::string_view::npos) {
      return false;
    }
    const auto arguments =
        value.substr(arguments_start, closing_parenthesis - arguments_start);
    std::vector<double> numbers;
    if (!ParseNumberList(arguments, numbers)) {
      return false;
    }

    if ((name == "translate" && numbers.size() != 2) ||
        (name == "rotate" && numbers.size() != 3) ||
        (name == "scale" && numbers.size() != 2)) {
      return false;
    }
    if (name == "scale" && (numbers[0] <= 0.0 || numbers[1] <= 0.0)) {
      return false;
    }

    has_operation = true;
    previous_order = order->second;
    position = closing_parenthesis + 1;
  }
}

std::size_t Utf8Length(std::string_view value) {
  return static_cast<std::size_t>(
      std::count_if(value.begin(), value.end(), [](const char character) {
        return (static_cast<unsigned char>(character) & 0xC0U) != 0x80U;
      }));
}

bool ContainsNonWhitespace(std::string_view value) {
  return std::any_of(value.begin(), value.end(), [](const char character) {
    return character != ' ' && character != '\t' && character != '\r' &&
           character != '\n';
  });
}

std::string_view TrimWhitespace(std::string_view value) {
  std::size_t begin = 0;
  std::size_t end = value.size();
  while (begin < end && !ContainsNonWhitespace(value.substr(begin, 1))) {
    ++begin;
  }
  while (end > begin && !ContainsNonWhitespace(value.substr(end - 1, 1))) {
    --end;
  }
  return value.substr(begin, end - begin);
}

std::string BuildExceptionMessage(const std::vector<std::string>& errors) {
  if (errors.empty()) {
    return "SVG не прошёл валидацию";
  }
  std::ostringstream message;
  message << "SVG не прошёл валидацию: " << errors.front();
  if (errors.size() > 1) {
    message << " (и ещё " << errors.size() - 1 << ')';
  }
  return message.str();
}

class Validator final {
 public:
  explicit Validator(std::string_view svg) : svg_(svg) {}

  std::vector<std::string> Run() {
    if (svg_.size() > kMaxSvgBytes) {
      errors_.emplace_back("размер SVG превышает 256 КиБ");
      return std::move(errors_);
    }
    if (svg_.empty()) {
      errors_.emplace_back("SVG-документ пуст");
      return std::move(errors_);
    }

    pugi::xml_document document;
    constexpr unsigned int kParseOptions =
        pugi::parse_default | pugi::parse_comments | pugi::parse_pi |
        pugi::parse_declaration | pugi::parse_doctype | pugi::parse_ws_pcdata;
    const auto parse_result = document.load_buffer(
        svg_.data(), svg_.size(), kParseOptions, pugi::encoding_utf8);
    if (!parse_result) {
      errors_.emplace_back("ответ не является корректным XML-документом");
      return std::move(errors_);
    }

    const pugi::xml_node root = document.document_element();
    if (!root) {
      errors_.emplace_back("в документе отсутствует корневой элемент svg");
      return std::move(errors_);
    }
    ValidateEnvelope(root);
    ValidateDocumentNodes(document, root);
    ValidateRoot(root);
    ValidateElement(root, 0, 0);
    ValidateGradientReferences();
    return std::move(errors_);
  }

 private:
  void AddError(const std::string& message) {
    if (errors_.size() < 100) {
      errors_.push_back(message);
    }
  }

  void ValidateEnvelope(const pugi::xml_node& root) {
    const auto root_offset = root.offset_debug();
    if (root_offset < 0 ||
        static_cast<std::size_t>(root_offset) > svg_.size()) {
      AddError("не удалось определить границы корневого svg");
      return;
    }

    std::size_t root_start = static_cast<std::size_t>(root_offset);
    if (root_start > 0 && svg_[root_start - 1] == '<') {
      --root_start;
    }
    const auto prefix = TrimWhitespace(svg_.substr(0, root_start));
    if (!prefix.empty() &&
        !(prefix.starts_with("<?xml") && prefix.ends_with("?>") &&
          prefix.find("?>") == prefix.size() - 2)) {
      AddError("перед корневым svg разрешена только XML-декларация");
    }

    std::string wrapped = "<svg-validation-envelope>";
    wrapped.append(svg_.substr(root_start));
    wrapped += "</svg-validation-envelope>";

    pugi::xml_document envelope_document;
    constexpr unsigned int kEnvelopeParseOptions =
        pugi::parse_default | pugi::parse_comments | pugi::parse_pi |
        pugi::parse_doctype | pugi::parse_ws_pcdata;
    const auto parse_result = envelope_document.load_buffer(
        wrapped.data(), wrapped.size(), kEnvelopeParseOptions,
        pugi::encoding_utf8);
    if (!parse_result) {
      AddError("после корневого svg обнаружены посторонние данные");
      return;
    }

    std::size_t element_count = 0;
    for (const pugi::xml_node& node :
         envelope_document.document_element().children()) {
      if (node.type() == pugi::node_element) {
        ++element_count;
      } else if (node.type() == pugi::node_pcdata &&
                 !ContainsNonWhitespace(node.value())) {
        continue;
      } else {
        AddError("вне корневого svg обнаружены посторонние данные");
      }
    }
    if (element_count != 1) {
      AddError("документ должен содержать ровно один корневой svg");
    }
  }

  void ValidateDocumentNodes(const pugi::xml_document& document,
                             const pugi::xml_node& root) {
    for (const pugi::xml_node& node : document.children()) {
      if (node == root || node.type() == pugi::node_declaration) {
        continue;
      }
      if (node.type() == pugi::node_pcdata) {
        if (ContainsNonWhitespace(node.value())) {
          AddError("вне корневого svg обнаружен текст");
        }
        continue;
      }
      if (node.type() == pugi::node_doctype) {
        AddError("DOCTYPE и пользовательские XML entities запрещены");
        continue;
      }
      AddError("в документе разрешены только XML-декларация и корневой svg");
    }
  }

  void ValidateRoot(const pugi::xml_node& root) {
    if (NodeName(root) != "svg") {
      AddError("корневой элемент должен быть svg");
    }
    if (AttributeValue(root, "xmlns") != kSvgNamespace) {
      AddError(
          "корневой svg должен иметь xmlns=\"http://www.w3.org/2000/svg\"");
    }

    if (!HasAttribute(root, "viewBox")) {
      AddError("у корневого svg отсутствует обязательный viewBox");
    } else {
      std::vector<double> view_box;
      if (!ParseNumberList(AttributeValue(root, "viewBox"), view_box) ||
          view_box.size() != 4) {
        AddError("viewBox должен содержать четыре конечных числа");
      } else if (view_box[2] <= 0.0 || view_box[3] <= 0.0) {
        AddError("ширина и высота viewBox должны быть больше нуля");
      }
    }
  }

  void ValidateElement(const pugi::xml_node& node, std::size_t depth,
                       std::size_t group_depth) {
    if (node.type() != pugi::node_element) {
      return;
    }
    ++element_count_;
    if (element_count_ == kMaxElements + 1) {
      AddError("число SVG-элементов превышает 500");
    }
    if (depth > kMaxNesting) {
      AddError("вложенность SVG-элементов превышает 16");
    }

    const std::string name = NodeName(node);
    if (!kAllowedElements.count(name)) {
      AddError("запрещённый SVG-элемент: " + name);
    }
    if (name == "svg" && depth != 0) {
      AddError("элемент svg разрешён только как корневой");
    }
    if (name.find(':') != std::string::npos) {
      AddError("элементы с префиксом пространства имён запрещены: " + name);
    }

    const std::size_t next_group_depth = group_depth + (name == "g" ? 1U : 0U);
    if (next_group_depth > kMaxNesting) {
      AddError("вложенность групп превышает 16");
    }

    ValidateAttributes(node, name);
    ValidateRequiredAttributes(node, name);
    ValidateChildren(node, name, depth, next_group_depth);
  }

  void ValidateAttributes(const pugi::xml_node& node, const std::string& name) {
    for (const pugi::xml_attribute& attribute : node.attributes()) {
      const std::string attribute_name = attribute.name();
      const std::string value = attribute.value();

      if (!IsAllowedAttribute(name, attribute_name)) {
        AddError("атрибут " + attribute_name + " запрещён для элемента " +
                 name);
        continue;
      }
      ValidateAttributeValue(name, attribute_name, value);
    }
  }

  bool IsAllowedAttribute(const std::string& element,
                          const std::string& attribute) const {
    if (element == "svg") {
      return attribute == "xmlns" || attribute == "viewBox" ||
             attribute == "width" || attribute == "height";
    }
    if (attribute == "id") {
      return true;
    }
    if (attribute == "data-editor-layer" || attribute == "data-name") {
      return element == "g";
    }
    if (attribute == "transform") {
      return element == "g" || kVisualElements.count(element) != 0;
    }
    if (kGeometryAttributes.count(attribute)) {
      return IsAllowedGeometryAttribute(element, attribute);
    }
    if (kPresentationAttributes.count(attribute)) {
      return element == "g" || kVisualElements.count(element) != 0 ||
             element == "tspan" ||
             (element == "stop" &&
              (attribute == "stop-color" || attribute == "stop-opacity" ||
               attribute == "opacity"));
    }
    return false;
  }

  bool IsAllowedGeometryAttribute(const std::string& element,
                                  const std::string& attribute) const {
    if (element == "path") {
      return attribute == "d";
    }
    if (element == "rect") {
      return attribute == "x" || attribute == "y" || attribute == "width" ||
             attribute == "height" || attribute == "rx" || attribute == "ry";
    }
    if (element == "circle") {
      return attribute == "cx" || attribute == "cy" || attribute == "r";
    }
    if (element == "ellipse") {
      return attribute == "cx" || attribute == "cy" || attribute == "rx" ||
             attribute == "ry";
    }
    if (element == "line") {
      return attribute == "x1" || attribute == "y1" || attribute == "x2" ||
             attribute == "y2";
    }
    if (element == "polyline" || element == "polygon") {
      return attribute == "points";
    }
    if (element == "text" || element == "tspan") {
      return attribute == "x" || attribute == "y";
    }
    if (element == "linearGradient") {
      return attribute == "x1" || attribute == "y1" || attribute == "x2" ||
             attribute == "y2";
    }
    if (element == "radialGradient") {
      return attribute == "cx" || attribute == "cy" || attribute == "r";
    }
    return element == "stop" && attribute == "offset";
  }

  void ValidateAttributeValue(const std::string& element,
                              const std::string& attribute,
                              const std::string& value) {
    if (attribute == "xmlns") {
      return;
    }
    std::string lower_value = value;
    std::transform(lower_value.begin(), lower_value.end(), lower_value.begin(),
                   [](const unsigned char character) {
                     return static_cast<char>(std::tolower(character));
                   });
    if (lower_value.find("://") != std::string::npos ||
        lower_value.find("//") != std::string::npos ||
        lower_value.find("data:") != std::string::npos ||
        lower_value.find("javascript:") != std::string::npos ||
        lower_value.find("file:") != std::string::npos ||
        lower_value.find("ftp:") != std::string::npos) {
      AddError("внешние URL и data:-URL в атрибутах запрещены");
      return;
    }
    if (attribute == "id") {
      if (!IsValidId(value)) {
        AddError("id должен соответствовать ^[a-z][a-z0-9-]{0,63}$: " + value);
      } else if (!ids_.insert(value).second) {
        AddError("id должен быть уникальным: " + value);
      }
      if (element == "linearGradient" || element == "radialGradient") {
        gradient_ids_.insert(value);
      }
      return;
    }
    if (attribute == "data-editor-layer") {
      if (value != "true") {
        AddError("data-editor-layer у группы должен быть равен true");
      }
      return;
    }
    if (attribute == "transform") {
      if (!IsValidTransform(value)) {
        AddError("transform должен следовать порядку translate, rotate, scale");
      }
      return;
    }
    if (attribute == "viewBox") {
      return;
    }
    if (attribute == "d") {
      if (!IsValidPath(value)) {
        AddError("атрибут d содержит недопустимую команду или число");
      }
      return;
    }
    if (attribute == "points") {
      std::vector<double> points;
      const std::size_t minimum_points = element == "polygon" ? 6U : 4U;
      if (!ParseNumberList(value, points) || points.size() < minimum_points ||
          points.size() % 2 != 0) {
        AddError("points должен содержать пары конечных чисел");
      }
      return;
    }
    if (kScalarNumberAttributes.count(attribute)) {
      std::vector<double> numbers;
      if (!ParseNumberList(value, numbers) || numbers.size() != 1) {
        AddError("атрибут " + attribute + " должен быть конечным числом");
        return;
      }
      if (kNonNegativeAttributes.count(attribute) && numbers.front() < 0.0) {
        AddError("атрибут " + attribute + " не может быть отрицательным");
      }
      if (kOpacityAttributes.count(attribute) &&
          (numbers.front() < 0.0 || numbers.front() > 1.0)) {
        AddError("атрибут " + attribute + " должен быть в диапазоне от 0 до 1");
      }
      return;
    }
    if (attribute == "fill" || attribute == "stroke" ||
        attribute == "stop-color") {
      std::string gradient_id;
      if (!IsValidColor(value, &gradient_id)) {
        AddError("атрибут " + attribute +
                 " должен быть hex-цветом, none или локальным градиентом");
      } else if (!gradient_id.empty()) {
        gradient_references_.push_back(std::move(gradient_id));
      }
      return;
    }
    if (attribute == "font-family") {
      static const std::unordered_set<std::string> fonts{
          "Arial",      "Helvetica", "Inter",    "Roboto",
          "sans-serif", "serif",     "monospace"};
      if (!fonts.count(value)) {
        AddError("использован неподдерживаемый font-family: " + value);
      }
      return;
    }
    if (attribute == "stroke-linecap" && value != "butt" && value != "round" &&
        value != "square") {
      AddError("использован неподдерживаемый stroke-linecap");
    } else if (attribute == "stroke-linejoin" && value != "miter" &&
               value != "round" && value != "bevel") {
      AddError("использован неподдерживаемый stroke-linejoin");
    } else if (attribute == "font-style" && value != "normal" &&
               value != "italic" && value != "oblique") {
      AddError("использован неподдерживаемый font-style");
    } else if (attribute == "text-anchor" && value != "start" &&
               value != "middle" && value != "end") {
      AddError("использован неподдерживаемый text-anchor");
    } else if (attribute == "font-weight") {
      ValidateFontWeight(value);
    }
  }

  void ValidateFontWeight(const std::string& value) {
    if (value == "normal" || value == "bold") {
      return;
    }
    std::vector<double> numbers;
    if (!ParseNumberList(value, numbers) || numbers.size() != 1 ||
        numbers.front() < 1.0 || numbers.front() > 1000.0 ||
        std::floor(numbers.front()) != numbers.front()) {
      AddError("использован неподдерживаемый font-weight");
    }
  }

  void ValidateRequiredAttributes(const pugi::xml_node& node,
                                  const std::string& name) {
    if ((name == "g" || kVisualElements.count(name)) &&
        !HasAttribute(node, "id")) {
      AddError("у редактируемого элемента " + name + " отсутствует id");
    }
    if (name == "g" && !HasAttribute(node, "data-editor-layer")) {
      AddError("у группы отсутствует data-editor-layer=\"true\"");
    }
    if ((name == "linearGradient" || name == "radialGradient") &&
        !HasAttribute(node, "id")) {
      AddError("у градиента отсутствует id");
    }
    if (name == "text" &&
        (!HasAttribute(node, "x") || !HasAttribute(node, "y"))) {
      AddError("у элемента text должны быть числовые атрибуты x и y");
    }
    static const std::unordered_map<std::string, std::vector<const char*>>
        required_geometry{
            {"path", {"d"}},
            {"rect", {"width", "height"}},
            {"circle", {"cx", "cy", "r"}},
            {"ellipse", {"cx", "cy", "rx", "ry"}},
            {"line", {"x1", "y1", "x2", "y2"}},
            {"polyline", {"points"}},
            {"polygon", {"points"}},
        };
    const auto required = required_geometry.find(name);
    if (required != required_geometry.end()) {
      for (const char* attribute : required->second) {
        if (!HasAttribute(node, attribute)) {
          AddError("у элемента " + name + " отсутствует атрибут " + attribute);
        }
      }
    }
  }

  void ValidateChildren(const pugi::xml_node& node, const std::string& name,
                        std::size_t depth, std::size_t group_depth) {
    for (const pugi::xml_node& child : node.children()) {
      if (child.type() == pugi::node_element) {
        const std::string child_name = NodeName(child);
        if (!IsAllowedChild(name, child_name)) {
          AddError("элемент " + child_name + " нельзя помещать внутрь " + name);
        }
        ValidateElement(child, depth + 1, group_depth);
        continue;
      }
      if (child.type() == pugi::node_pcdata) {
        const std::string text = child.value();
        if (name != "text" && name != "tspan" && ContainsNonWhitespace(text)) {
          AddError("текстовые узлы разрешены только внутри text и tspan");
        }
        continue;
      }
      AddError("комментарии, инструкции и CDATA внутри SVG запрещены");
    }

    if (name == "text") {
      std::string text;
      CollectText(node, text);
      if (Utf8Length(text) > kMaxTextLength) {
        AddError("длина текста одного элемента превышает 1000 символов");
      }
    }
  }

  void CollectText(const pugi::xml_node& node, std::string& text) const {
    for (const pugi::xml_node& child : node.children()) {
      if (child.type() == pugi::node_pcdata) {
        text += child.value();
      } else if (child.type() == pugi::node_element &&
                 NodeName(child) == "tspan") {
        CollectText(child, text);
      }
    }
  }

  bool IsAllowedChild(const std::string& parent,
                      const std::string& child) const {
    if (parent == "svg" || parent == "g") {
      return child == "g" || child == "defs" ||
             kVisualElements.count(child) != 0;
    }
    if (parent == "defs") {
      return child == "linearGradient" || child == "radialGradient";
    }
    if (parent == "linearGradient" || parent == "radialGradient") {
      return child == "stop";
    }
    if (parent == "text") {
      return child == "tspan";
    }
    return false;
  }

  void ValidateGradientReferences() {
    for (const auto& reference : gradient_references_) {
      if (!gradient_ids_.count(reference)) {
        AddError("локальная ссылка указывает на отсутствующий градиент: " +
                 reference);
      }
    }
  }

  std::string_view svg_;
  std::vector<std::string> errors_;
  std::unordered_set<std::string> ids_;
  std::unordered_set<std::string> gradient_ids_;
  std::vector<std::string> gradient_references_;
  std::size_t element_count_{0};
};

}  // namespace

SvgValidationError::SvgValidationError(std::vector<std::string> errors)
    : std::runtime_error(BuildExceptionMessage(errors)),
      errors_(std::move(errors)) {}

const std::vector<std::string>& SvgValidationError::Errors() const noexcept {
  return errors_;
}

std::vector<std::string> ValidateSvg(std::string_view svg) {
  return Validator(svg).Run();
}

void ValidateSvgOrThrow(std::string_view svg) {
  auto errors = ValidateSvg(svg);
  if (!errors.empty()) {
    throw SvgValidationError(std::move(errors));
  }
}

}  // namespace ya_brander::components
