#include "list_versions.hpp"
#include <userver/formats/json/serialize.hpp>
#include <userver/utils/datetime.hpp>

namespace ya_brander::handlers {

    ListVersionsHandler::ListVersionsHandler(const userver::components::ComponentConfig& config, const userver::components::ComponentContext& context)
        : HttpHandlerBase(config, context), project_manager_(context.FindComponent<components::ProjectManagerComponent>()) {}

    std::string ListVersionsHandler::HandleRequestThrow(const userver::server::http::HttpRequest& request, userver::server::request::RequestContext& context) const {
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

        const auto versions = project_manager_.ListVersions(id);

        userver::formats::json::ValueBuilder arr(userver::formats::common::Type::kArray);
        for (const auto& v : versions) {
            userver::formats::json::ValueBuilder item;
            item["id"] = v.id;
            item["version_num"] = v.version_num;
            item["created_at"] = userver::utils::datetime::Timestring(v.created_at.GetUnderlying());
            arr.PushBack(std::move(item));
        }

        userver::formats::json::ValueBuilder res;
        res["versions"] = arr.ExtractValue();
        return userver::formats::json::ToString(res.ExtractValue());
    }

}