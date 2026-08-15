#include "restore_version.hpp"
#include <userver/formats/json/serialize.hpp>

namespace ya_brander::handlers {

    RestoreVersionHandler::RestoreVersionHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context)
        : HttpHandlerBase(config, context), project_manager_(context.FindComponent<components::ProjectManagerComponent>()) {}

    std::string RestoreVersionHandler::HandleRequestThrow(const userver::server::http::HttpRequest& request, userver::server::request::RequestContext& context) const {
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
        // Только автор может делать роллбэк в своем проекте
        if (project->author_id != *user_id) {
            request.SetResponseStatus(userver::server::http::HttpStatus::kForbidden);
            return {};
        }

        const auto& version_id = request.GetPathArg("version_id");
        if (project_manager_.GetVersionUrl(id, version_id).empty()) {
            request.SetResponseStatus(userver::server::http::HttpStatus::kNotFound);
            return {};
        }

        const auto new_version_id = project_manager_.RestoreVersion(id, version_id);
        const auto url = project_manager_.GetLatestVersionUrl(id);

        userver::formats::json::ValueBuilder res;
        res["version_id"] = new_version_id;
        res["url"] = url;
        return userver::formats::json::ToString(res.ExtractValue());
    }

}