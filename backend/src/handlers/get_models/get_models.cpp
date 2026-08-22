#include "get_models.hpp"
#include "../../components/llm_models.hpp"

#include <algorithm>
#include <array>
#include <cstdlib>
#include <nlohmann/json.hpp>
#include <stdexcept>
#include <string>
#include <string_view>
#include <utility>
#include <userver/clients/http/request.hpp>
#include <userver/yaml_config/merge_schemas.hpp>

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

  auto models = nlohmann::json::parse(response->body());
  if (!models.contains("data") || !models["data"].is_array()) {
    throw std::runtime_error("Invalid models response from Yandex AI Studio");
  }
  if (!models.contains("object")) {
    models["object"] = "list";
  }

  constexpr std::array<std::string_view, 3> kAllowedModels = {
      "/deepseek-v4-flash/latest",
      "/gpt-oss-120b/latest",
      "/qwen3-235b-a22b-fp8/latest",
  };
  auto filtered_models = nlohmann::json::array();
  for (const auto& model : models["data"]) {
    if (!model.contains("id") || !model["id"].is_string()) {
      continue;
    }
    const auto& id = model["id"].get_ref<const std::string&>();
    const bool is_allowed = std::any_of(
        kAllowedModels.begin(), kAllowedModels.end(),
        [&id](std::string_view allowed) {
          return id.find(allowed) != std::string::npos;
        });
    if (is_allowed) {
      filtered_models.push_back(model);
    }
  }
  models["data"] = std::move(filtered_models);

  const auto openai_model_uri = std::string{components::kOpenAIModelUri};
  const char* openai_key = std::getenv("OPENAI_API_KEY");
  const bool openai_is_configured =
      openai_key != nullptr && !std::string_view(openai_key).empty();
  const bool luna_exists = std::any_of(
      models["data"].begin(), models["data"].end(),
      [&openai_model_uri](const auto& model) {
        return model.value("id", "") == openai_model_uri;
      });
  if (openai_is_configured && !luna_exists) {
    models["data"].push_back({{"id", openai_model_uri},
                              {"owned_by", "OpenAI"}});
  }

  return models.dump();
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
