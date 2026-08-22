#include "restore_version.hpp"
#include <userver/formats/json/value_builder.hpp>

namespace ya_brander::handlers {

    RestoreVersionHandler::RestoreVersionHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context)
        : HttpHandlerJsonBase(config, context), project_manager_(context.FindComponent<components::ProjectManagerComponent>()) {}

    userver::formats::json::Value RestoreVersionHandler::HandleRequestJsonThrow(
        const userver::server::http::HttpRequest& request,
        const userver::formats::json::Value& /*request_json*/,
        userver::server::request::RequestContext& context) const {
        const auto& user_id = context.GetData<std::string>("user_id");

        const auto& id = request.GetPathArg("id");
        auto project = project_manager_.GetProject(id);

        if (!project) {
            request.SetResponseStatus(userver::server::http::HttpStatus::kNotFound);
            return {};
        }
        // Только автор может сделать роллбэк своего проекта.
        if (project->author_id != user_id) {
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
        return res.ExtractValue();
    }

}