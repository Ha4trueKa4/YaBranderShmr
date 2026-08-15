#include "get_version.hpp"
#include <userver/formats/json/value_builder.hpp>

namespace ya_brander::handlers {

    GetVersionHandler::GetVersionHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context)
        : HttpHandlerJsonBase(config, context), project_manager_(context.FindComponent<components::ProjectManagerComponent>()) {}

    userver::formats::json::Value GetVersionHandler::HandleRequestJsonThrow(
        const userver::server::http::HttpRequest& request,
        const userver::formats::json::Value& /*request_json*/,
        userver::server::request::RequestContext& context) const {
        std::string* user_id = context.GetDataOptional<std::string>("user_id");
        if (!user_id) {
            request.SetResponseStatus(userver::server::http::HttpStatus::kUnauthorized);
            return {};
        }

        const auto& id = request.GetPathArg("id");
        auto project = project_manager_.GetProject(id);

        if (!project) {
            request.SetResponseStatus(userver::server::http::HttpStatus::kNotFound);
            return {};
        }
        // Только автор может смотреть историю, она приватна для других юзеров даже у публичных проектов
        if (project->author_id != *user_id) {
            request.SetResponseStatus(userver::server::http::HttpStatus::kForbidden);
            return {};
        }

        const auto& version_id = request.GetPathArg("version_id");
        const auto url = project_manager_.GetVersionUrl(id, version_id);
        if (url.empty()) {
            request.SetResponseStatus(userver::server::http::HttpStatus::kNotFound);
            return {};
        }

        userver::formats::json::ValueBuilder res;
        res["version_id"] = version_id;
        res["url"] = url;
        return res.ExtractValue();
    }

}