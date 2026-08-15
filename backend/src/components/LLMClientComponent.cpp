#include "LLMClientComponent.hpp"

#include <nlohmann/json.hpp>
#include <regex>
#include <stdexcept>
#include <pugixml.hpp>

#include <userver/clients/http/client.hpp>
#include <userver/clients/http/component.hpp>
#include <userver/components/component_context.hpp>
#include <userver/server/handlers/http_handler_base.hpp>
#include <userver/yaml_config/schema.hpp>
#include <userver/yaml_config/merge_schemas.hpp>

namespace ya_brander::components {
  // Вспомогательные функции
  static void CollectAllNodes(const pugi::xml_node& node, std::vector<pugi::xml_node>& nodes) {
    nodes.push_back(node);
    for (pugi::xml_node child = node.first_child(); child; child = child.next_sibling()) {
        if (child.type() == pugi::node_element) {
            CollectAllNodes(child, nodes);
        }
    }
  }

  static void CheckMaxDepth(const pugi::xml_node& node, int current_depth, int& max_depth) {
    max_depth = std::max(max_depth, current_depth);
    for (pugi::xml_node child = node.first_child(); child; child = child.next_sibling()) {
        if (child.type() == pugi::node_element) {
            CheckMaxDepth(child, current_depth + 1, max_depth);
        }
    }
  }

  static void CheckUniqueIds(const pugi::xml_node& node, std::set<std::string>& ids,
                           std::vector<std::string>& errors) {
    pugi::xml_attribute id_attr = node.attribute("id");
    if (id_attr) {
        std::string id = id_attr.value();
        if (!ids.insert(id).second) {
            errors.push_back("Duplicate ID found: " + id);
        }

        // Проверка формата ID: ^[a-z][a-z0-9-]{0,63}$
        if (!std::regex_match(id, std::regex("^[a-z][a-z0-9-]{0,63}$"))) {
            errors.push_back("Invalid ID format: " + id + " (must be ^[a-z][a-z0-9-]{0,63}$)");
        }
    }

    for (pugi::xml_node child = node.first_child(); child; child = child.next_sibling()) {
        if (child.type() == pugi::node_element) {
            CheckUniqueIds(child, ids, errors);
        }
    }
  }

  static void ValidateElement(const pugi::xml_node& node,
                            const std::set<std::string>& allowed_elements,
                            const std::set<std::string>& allowed_attributes,
                            const std::set<std::string>& forbidden_elements,
                            const std::set<std::string>& css_attributes,
                            std::vector<std::string>& errors) {
    std::string name = node.name();

    // Проверка разрешенных элементов
    if (allowed_elements.find(name) == allowed_elements.end()) {
        errors.push_back("Unsupported element: <" + name + ">");
    }

    // Проверка атрибутов
    for (pugi::xml_attribute attr = node.first_attribute(); attr; attr = attr.next_attribute()) {
        std::string attr_name = attr.name();

        // Пропускаем специальные атрибуты для редактора
        if (attr_name == "data-editor-layer" || attr_name == "data-name") {
            continue;
        }

        // Проверяем, что атрибут разрешен
        if (allowed_attributes.find(attr_name) == allowed_attributes.end()) {
            errors.push_back("Unsupported attribute '" + attr_name + "' on <" + name + ">");
        }
    }

    // Запрещенные элементы
    if (forbidden_elements.find(name) != forbidden_elements.end()) {
        errors.push_back("Forbidden element: <" + name + ">");
    }

    // Запрещенные атрибуты style
    if (node.attribute("style")) {
        errors.push_back("style attribute is forbidden on <" + name + ">");
    }

    // Запрещенные CSS атрибуты
    for (pugi::xml_attribute attr = node.first_attribute(); attr; attr = attr.next_attribute()) {
        std::string attr_name = attr.name();
        if (css_attributes.find(attr_name) != css_attributes.end()) {
            errors.push_back("CSS attribute '" + attr_name + "' is forbidden on <" + name + ">");
        }
    }
  }

  static void ValidateViewBox(const pugi::xml_node& root, std::vector<std::string>& errors) {
    pugi::xml_attribute viewbox = root.attribute("viewBox");
    if (!viewbox) {
        errors.push_back("viewBox attribute is required");
        return;
    }

    std::string value = viewbox.value();
    std::istringstream iss(value);
    std::vector<double> numbers;
    double num;
    while (iss >> num) {
        numbers.push_back(num);
    }

    if (numbers.size() != 4) {
        errors.push_back("viewBox must contain exactly 4 numbers");
        return;
    }

    if (numbers[2] <= 0 || numbers[3] <= 0) {
        errors.push_back("viewBox width and height must be greater than 0");
    }

    if (std::isnan(numbers[0]) || std::isinf(numbers[0]) ||
        std::isnan(numbers[1]) || std::isinf(numbers[1]) ||
        std::isnan(numbers[2]) || std::isinf(numbers[2]) ||
        std::isnan(numbers[3]) || std::isinf(numbers[3])) {
        errors.push_back("viewBox contains NaN or Infinity values");
    }
  }

  static void ValidateIdFormat(const pugi::xml_node& node, std::vector<std::string>& errors) {
    pugi::xml_attribute id_attr = node.attribute("id");
    if (id_attr) {
        std::string id = id_attr.value();
        if (id.length() > 64) {
            errors.push_back("ID length exceeds 64 characters: " + id);
        }
    }

    for (pugi::xml_node child = node.first_child(); child; child = child.next_sibling()) {
        if (child.type() == pugi::node_element) {
            ValidateIdFormat(child, errors);
        }
    }
  }

  static void ValidateTextElements(const pugi::xml_node& node, std::vector<std::string>& errors) {
    if (std::string(node.name()) == "text") {
        // Проверка на наличие tspan внутри text
        for (pugi::xml_node child = node.first_child(); child; child = child.next_sibling()) {
            if (child.type() == pugi::node_element && std::string(child.name()) != "tspan") {
                errors.push_back("text element can only contain text nodes and tspan elements");
            }
        }

        // Проверка длины текста
        std::string text_content = node.text().get();
        if (text_content.length() > 1000) {
            errors.push_back("Text content exceeds 1000 characters");
        }

        // Проверка x и y атрибутов
        if (!node.attribute("x") && !node.attribute("y")) {
            errors.push_back("text element must have x and/or y attributes");
        }
    }

    // Запрет textPath
    if (std::string(node.name()) == "textPath") {
        errors.push_back("textPath is forbidden");
    }

    for (pugi::xml_node child = node.first_child(); child; child = child.next_sibling()) {
        if (child.type() == pugi::node_element) {
            ValidateTextElements(child, errors);
        }
    }
  }

  static void ValidateColorsAndUrls(const pugi::xml_node& node, std::vector<std::string>& errors) {
    // Проверка цветов и URL во всех атрибутах
    for (pugi::xml_attribute attr = node.first_attribute(); attr; attr = attr.next_attribute()) {
        std::string value = attr.value();
        std::string name = attr.name();
        // Проверка на внешние URL
        if (name != "xmlns" && (value.find("http://") != std::string::npos ||
            value.find("https://") != std::string::npos)) {
            errors.push_back("External URLs are forbidden: " + value);
        }

        // Проверка на data: URL
        if (value.find("data:") != std::string::npos) {
            errors.push_back("data: URLs are forbidden: " + value);
        }

        // Проверка на ссылки на внешние ресурсы
        if (value.find(".ttf") != std::string::npos ||
            value.find(".woff") != std::string::npos ||
            value.find(".otf") != std::string::npos) {
            errors.push_back("External font resources are forbidden: " + value);
        }
    }

    for (pugi::xml_node child = node.first_child(); child; child = child.next_sibling()) {
        if (child.type() == pugi::node_element) {
            ValidateColorsAndUrls(child, errors);
        }
    }
  }

  static void ValidateTransform(const pugi::xml_node& node, std::vector<std::string>& errors) {
    pugi::xml_attribute transform_attr = node.attribute("transform");
    if (!transform_attr) {
        return;
    }

    std::string transform = transform_attr.value();

    // Проверка на запрещенные операции
    if (transform.find("matrix") != std::string::npos ||
        transform.find("skewX") != std::string::npos ||
        transform.find("skewY") != std::string::npos) {
        errors.push_back("Only translate, rotate, and scale are allowed in transform");
    }

    // Проверка порядка операций (translate -> rotate -> scale)
    size_t translate_pos = transform.find("translate");
    size_t rotate_pos = transform.find("rotate");
    size_t scale_pos = transform.find("scale");

    if (rotate_pos != std::string::npos && translate_pos != std::string::npos) {
        if (rotate_pos < translate_pos) {
            errors.push_back("transform operations must be in order: translate -> rotate -> scale");
        }
    }

    if (scale_pos != std::string::npos) {
        if ((translate_pos != std::string::npos && scale_pos < translate_pos) ||
            (rotate_pos != std::string::npos && scale_pos < rotate_pos)) {
            errors.push_back("transform operations must be in order: translate -> rotate -> scale");
        }
    }

    // Проверка на отрицательный scale
    std::regex scale_regex("scale\\(([^)]+)\\)");
    std::smatch match;
    if (std::regex_search(transform, match, scale_regex)) {
        std::string scale_value = match[1];
        std::istringstream iss(scale_value);
        std::vector<double> scales;
        double num;
        while (iss >> num) {
            scales.push_back(num);
        }

        if (scales.size() >= 1 && (scales[0] <= 0 || (scales.size() >= 2 && scales[1] <= 0))) {
            errors.push_back("scale values must be greater than 0");
        }
    }

    for (pugi::xml_node child = node.first_child(); child; child = child.next_sibling()) {
        if (child.type() == pugi::node_element) {
            ValidateTransform(child, errors);
        }
    }
  }

  static void ValidateFontFamily(const pugi::xml_node& node, std::vector<std::string>& errors) {
    pugi::xml_attribute font_attr = node.attribute("font-family");
    if (!font_attr) {
        return;
    }

    std::string font_family = font_attr.value();
    static const std::set<std::string> allowed_fonts = {
        "Arial", "Helvetica", "Inter", "Roboto",
        "sans-serif", "serif", "monospace"
    };

    // Проверяем, что font-family содержит только разрешенные шрифты
    bool allowed = false;
    for (const auto& font : allowed_fonts) {
        if (font_family.find(font) != std::string::npos) {
            allowed = true;
            break;
        }
    }

    if (!allowed) {
        errors.push_back("Unsupported font-family: " + font_family);
    }

    for (pugi::xml_node child = node.first_child(); child; child = child.next_sibling()) {
        if (child.type() == pugi::node_element) {
            ValidateFontFamily(child, errors);
        }
    }
  }

  static void ValidateRootElement(const pugi::xml_node& root, std::vector<std::string>& errors) {
    // Проверка xmlns
    pugi::xml_attribute xmlns = root.attribute("xmlns");
    if (!xmlns || std::string(xmlns.value()) != "http://www.w3.org/2000/svg") {
        errors.push_back("xmlns must be 'http://www.w3.org/2000/svg'");
    }

    // Проверка viewBox (основная проверка в отдельной функции)
    ValidateViewBox(root, errors);
  }

  // Основная функция валидации
  std::string ValidateSVG(const std::string& svg_content) {
    std::vector<std::string> errors;

    // 1. Проверка размера
    if (svg_content.size() > 256 * 1024) {
        errors.push_back("SVG size exceeds 256 KiB limit");
    }

    // 2. Парсинг XML
    pugi::xml_document doc;
    pugi::xml_parse_result result = doc.load_string(svg_content.c_str());
    if (!result) {
        throw SVGValidationError("Invalid XML: " + std::string(result.description()));
    }

    // 3. Проверка корневого элемента
    pugi::xml_node root = doc.document_element();
    if (std::string(root.name()) != "svg") {
        errors.push_back("Root element must be <svg>");
    }

    if (!errors.empty()) {
      std::string all_errors;
      for (auto er: errors) {
        all_errors = all_errors + ". " + er;
      }
      throw SVGValidationError(all_errors);
    }

    // 4. Валидация корневого элемента
    ValidateRootElement(root, errors);

    // 5. Сбор всех элементов для проверки
    std::vector<pugi::xml_node> all_nodes;
    CollectAllNodes(root, all_nodes);

    // 6. Проверка лимитов
    if (all_nodes.size() > 500) {
        errors.push_back("Number of SVG elements exceeds 500 limit");
    }

    // 7. Проверка глубины вложенности
    int max_depth = 0;
    CheckMaxDepth(root, 0, max_depth);
    if (max_depth > 16) {
        errors.push_back("Maximum nesting depth exceeds 16");
    }

    // 8. Проверка уникальности ID
    std::set<std::string> ids;
    CheckUniqueIds(root, ids, errors);

    // 9. Настройка разрешенных элементов и атрибутов
    static const std::set<std::string> allowed_elements = {
        "svg", "g", "defs", "linearGradient", "radialGradient", "stop",
        "path", "rect", "circle", "ellipse", "line", "polyline", "polygon",
        "text", "tspan"
    };

    static const std::set<std::string> allowed_attributes = {
        "xmlns", "viewBox", "id", "data-editor-layer", "data-name",
        "transform", "x", "y", "x1", "y1", "x2", "y2", "cx", "cy",
        "r", "rx", "ry", "width", "height", "d", "points", "offset",
        "fill", "fill-opacity", "stroke", "stroke-opacity", "stroke-width",
        "stroke-linecap", "stroke-linejoin", "opacity",
        "font-family", "font-size", "font-weight", "font-style",
        "text-anchor", "stop-color", "stop-opacity",
        "gradientUnits", "spreadMethod"
    };

    static const std::set<std::string> forbidden_elements = {
        "script", "style", "foreignObject", "iframe", "image",
        "use", "symbol", "marker", "filter", "mask", "clipPath"
    };

    static const std::set<std::string> css_attributes = {
        "transform-origin"
    };

    // 10. Проверка элементов
    for (const auto& node : all_nodes) {
        ValidateElement(node, allowed_elements, allowed_attributes,
                       forbidden_elements, css_attributes, errors);
    }

    // 11. Проверка текстовых узлов
    ValidateTextElements(root, errors);

    // 12. Проверка формата ID
    ValidateIdFormat(root, errors);

    // 13. Проверка цветов и URL
    ValidateColorsAndUrls(root, errors);

    // 14. Проверка transform
    ValidateTransform(root, errors);

    // 15. Проверка font-family
    ValidateFontFamily(root, errors);

    if (!errors.empty()) {
      std::string all_errors;
      for (auto er: errors) {
        all_errors = all_errors + ". " + er;
      }
      throw SVGValidationError(all_errors);
    }

    return svg_content;
  }

  std::string LLMClientComponent::ExtractSVG(const std::string& text) const {
    size_t start = text.find("<svg");
    if (start == std::string::npos) {
      throw std::runtime_error("No SVG content found in LLM response");
    }

    size_t end = text.find("</svg>", start);
    if (end == std::string::npos) {
      throw std::runtime_error("Wrong SVG content in LLM response");
    }
    return text.substr(start, end - start + 6); // +6 для "</svg>"
  }

  std::string LLMClientComponent::SendPrompt(const LlmPrompt& prompt,
     std::size_t max_output_tokens,
     const std::string& Model,
     double TEMPERATURE) const {
    if (prompt.input.empty()) {
      throw std::invalid_argument("Empty prompt provided");
    }
        userver::formats::json::ValueBuilder request_body;
        request_body["model"] = "gpt://b1gttnvmtpd0tagdlb7o/" + Model;
        request_body["temperature"] = TEMPERATURE;
        request_body["max_output_tokens"] = max_output_tokens;
        request_body["instructions"] = prompt.instructions;
        request_body["input"] = prompt.input;

  auto json_value = request_body.ExtractValue();
  std::string json_data = userver::formats::json::ToString(json_value);

  auto request = http_client_.CreateRequest();
  request.method(userver::clients::http::HttpMethod::kPost);
  request.url(api_url_);
  request.headers({{"Authorization", "Api-Key " + api_key_},
                   {"Content-Type", "application/json"}});

        // Увеличиваем таймауты для SSL
        request.timeout(120000);  // 120 секунд

        request.data(json_data);

        auto response = request.perform();
        std::string response_body = response->body();
        LOG_INFO() << "LLM response" << response_body; //delete later

        if (response->status_code() != 200) {
          std::string error_msg;
          try {
            auto error_json = nlohmann::json::parse(response_body);
            if (error_json.contains("error")) {
              auto error = error_json["error"];
              error_msg = "API Error: " + error.value("code", "Unknown") +
                         " - " + error.value("message", "Unknown error");
            } else if (error_json.contains("message")) {
              error_msg = "API Error: " + error_json["message"].get<std::string>();
            } else {
              error_msg = "HTTP Error " + std::to_string(response->status_code());
            }
          } catch (const std::exception& e) {
            error_msg = "HTTP Error " + std::to_string(response->status_code()) +
                       ", body: " + response_body.substr(0, 200);
          }
          throw std::runtime_error(error_msg);
        }
        auto response_json = nlohmann::json::parse(response->body());

        std::string json_string = response_json.dump();
        std::string extracted_text;
        bool text_found = false;
        for (const auto& output_item : response_json["output"]) {
          if (output_item.contains("content") && output_item["content"].is_array() && !output_item["content"].empty()) {
            for (const auto& content_item : output_item["content"]) {
              if (content_item.contains("text") && content_item["text"].is_string()) {
                extracted_text = content_item["text"].get<std::string>();
                text_found = true;
                break;
              }
            }
            if (text_found) break;
          }
        }
        std::string svg = ExtractSVG(extracted_text);
        ValidateSVG(svg);
        return svg;
  }

userver::yaml_config::Schema LLMClientComponent::GetStaticConfigSchema() {
  return userver::yaml_config::MergeSchemas<
      userver::components::LoggableComponentBase>(R"(
type: object
description: LLM client component for Yandex AI Studio
additionalProperties: false
properties:
    api-url:
        type: string
        description: URL for Yandex GPT API
    api-key:
        type: string
        description: API Key
    )");
}

void AppendLLMClient(userver::components::ComponentList& component_list) {
  component_list.Append<components::LLMClientComponent>();
}

}  // namespace ya_brander::components
