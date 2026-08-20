#include "get_models.hpp"
#include <userver/clients/http/request.hpp>
#include <userver/yaml_config/merge_schemas.hpp>
#include <userver/formats/json/serialize.hpp>
#include <userver/formats/json/value.hpp>
#include <userver/formats/json/value_builder.hpp>
#include <string_view>
#include <vector>

namespace ya_brander::handlers {

GetModels::GetModels(const userver::components::ComponentConfig& config,
                     const userver::components::ComponentContext& context)
    : HttpHandlerBase(config, context),
      http_client_(context.FindComponent<userver::components::HttpClient>()
                         .GetHttpClient()),
      api_url_(config["api-url"].As<std::string>()) {
  const char* key = std::getenv("YANDEX_AI_API_KEY");
  if (key == nullptr || std::string_view(key).empty()) {
    throw std::runtime_error("YANDEX_AI_API_KEY is not set");
  }
  api_key_ = key;
}

std::string GetModels::HandleRequestThrow(
    const userver::server::http::HttpRequest& request,
    userver::server::request::RequestContext&) const {

  // Создаем запрос к Яндекс API
  auto request_yandex = http_client_.CreateRequest();
  request_yandex.method(userver::clients::http::HttpMethod::kGet);
  request_yandex.url(api_url_);
  request_yandex.headers({{"Authorization", "Api-Key " + api_key_}});
  request_yandex.timeout(std::chrono::seconds(10));

  // Выполняем запрос
  auto response = request_yandex.perform();

  // Проверяем статус ответа
  if (!response->IsOk()) {
    request.SetResponseStatus(response->status_code());
    return response->body();
  }

  // Парсим оригинальный JSON
  auto json_body = userver::formats::json::FromString(response->body());

  // Создаем билдер для нового ответа
  userver::formats::json::ValueBuilder response_builder;
  response_builder["object"] = json_body["object"]; // сохраняем корневой "object": "list"

  // Список разрешенных моделей
  const std::vector<std::string_view> allowed_models = {
    "/deepseek-v4-flash/latest",
    "/gpt-oss-120b/latest",
    "/qwen3-235b-a22b-fp8/latest"
  };

  // Билдер для отфильтрованного массива data
  userver::formats::json::ValueBuilder data_builder(userver::formats::json::Type::kArray);

  // Итерируемся по моделям и фильтруем
  if (json_body.HasMember("data") && json_body["data"].IsArray()) {
    for (const auto& item : json_body["data"]) {
      std::string id = item["id"].As<std::string>();

      bool keep = false;
      for (const auto& allowed : allowed_models) {
        // Проверяем, содержит ли URI модели разрешенную подстроку
        if (id.find(allowed) != std::string::npos) {
          keep = true;
          break;
        }
      }

      if (keep) {
        data_builder.PushBack(item);
      }
    }
  }

  response_builder["data"] = data_builder;

  // Сериализуем обратно в строку
  return userver::formats::json::ToString(response_builder.ExtractValue());
}

userver::yaml_config::Schema GetModels::GetStaticConfigSchema() {
  return userver::yaml_config::MergeSchemas<
      userver::server::handlers::HttpHandlerBase>(R"(
type: object
description: Handler for getting available models
additionalProperties: false
properties:
    api-url:
        type: string
        description: URL for Yandex GPT API
)");
}

void AppendGetModels(userver::components::ComponentList& component_list) {
  component_list.Append<GetModels>();
}

} // namespace ya_brander::handlers