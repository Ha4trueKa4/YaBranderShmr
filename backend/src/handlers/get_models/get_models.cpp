#include "get_models.hpp"
#include <userver/clients/http/request.hpp>
#include <userver/yaml_config/merge_schemas.hpp>
#include <userver/formats/json/serialize.hpp>

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
  }

  // Возвращаем тело ответа как строку
  return response->body();
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

}