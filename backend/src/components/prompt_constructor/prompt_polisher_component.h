#pragma once

#include <string>
#include <userver/components/component_list.hpp>
#include <userver/components/loggable_component_base.hpp>
#include <userver/clients/http/client.hpp>

namespace ya_brander::components {

class PromptPolisherComponent : public userver::components::LoggableComponentBase {
public:
  static constexpr std::string_view kName = "prompt-polisher";

  PromptPolisherComponent(const userver::components::ComponentConfig& config,
                          const userver::components::ComponentContext& context);

  std::string PolishGenerate(const std::string& raw_prompt) const;

  std::string PolishEdit(const std::string& raw_prompt) const;

  static userver::yaml_config::Schema GetStaticConfigSchema();

private:
  userver::clients::http::Client& http_client_;
  std::string api_url_;
  std::string api_key_;
};

void AppendPromptPolisher(userver::components::ComponentList& component_list);

} // namespace ya_brander::components