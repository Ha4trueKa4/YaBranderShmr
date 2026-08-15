#include "get_version.hpp"
#include <userver/formats/json/serialize.hpp>

namespace ya_brander::handlers {

    GetVersionHandler::GetVersionHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context)
        : HttpHandlerBase(config, context), project_manager_(context.FindComponent<components::ProjectManagerComponent>()) {}

    std::string GetVersionHandler::HandleRequestThrow(const userver::server::http::HttpRequest& request, userver::server::request::RequestContext& context) const {
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
        // Только автору можно: история проекта приватна для остальных юзеров даже если проект публичный.
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
        return userver::formats::json::ToString(res.ExtractValue());
    }

}