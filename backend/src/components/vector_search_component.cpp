#include "vector_search_component.h"

#include <fmt/format.h>

#include <userver/yaml_config/merge_schemas.hpp>
#include <userver/components/component_config.hpp>
#include <userver/clients/http/component.hpp>
#include <userver/components/component_context.hpp>
#include <userver/storages/postgres/component.hpp>
#include <userver/formats/json/serialize.hpp>
#include <userver/formats/serialize/common_containers.hpp>
#include <userver/formats/json/value_builder.hpp>
#include <userver/logging/log.hpp>

#include <ya_brander/sql_queries.hpp>

namespace ya_brander::components {

VectorSearchComponent::VectorSearchComponent(
    const userver::components::ComponentConfig& config,
    const userver::components::ComponentContext& context)
    : LoggableComponentBase(config, context),
      pg_cluster_(context.FindComponent<userver::components::Postgres>(
                       config["pg-component-name"].As<std::string>("postgres-db-1"))
                       .GetCluster()),
      http_client_(context.FindComponent<userver::components::HttpClient>().GetHttpClient()),
      embedding_url_(config["embedding-url"].As<std::string>()),
      embedding_model_(config["embedding-model"].As<std::string>("text-embeddings-v2-query")) {

    const char* key = std::getenv("YANDEX_AI_API_KEY");
    if (key == nullptr || std::string_view(key).empty()) {
        api_key_.clear();
    } else {
        api_key_ = key;
    }
}

std::vector<std::string> VectorSearchComponent::FindSimilar(
    const std::string& description, int limit) const {

    if (api_key_.empty()) {
        return {};
    }

    std::vector<double> embedding;
    try {
        embedding = GetEmbedding(description);
    } catch (const std::exception& e) {
        return {};
    }

    std::string vec_str = "[";
    for (size_t i = 0; i < embedding.size(); ++i) {
        vec_str += fmt::format("{:.8f}", embedding[i]);
        if (i + 1 < embedding.size()) vec_str += ",";
    }
    vec_str += "]";

    return SearchInDb(vec_str, limit, kMaxDistance);
}

std::vector<double> VectorSearchComponent::GetEmbedding(const std::string& text) const {
    userver::formats::json::ValueBuilder payload;
    payload["modelUri"] = fmt::format("emb://{}/{}/latest", kFolderId, embedding_model_);
    payload["text"] = text;

    auto request = http_client_.CreateRequest();
    request.method(userver::clients::http::HttpMethod::kPost);
    request.url(embedding_url_);
    request.headers({
        {"Authorization", "Api-Key " + api_key_},
        {"Content-Type", "application/json"}
    });
    request.timeout(15000);
    request.data(userver::formats::json::ToString(payload.ExtractValue()));

    try {
        auto response = request.perform();
        if (response->status_code() != 200) {
            return {};
        }
        auto json_body = userver::formats::json::FromString(response->body());
        return json_body["embedding"].As<std::vector<double>>();
    } catch (const std::exception& e) {
        return {};
    }
}

std::vector<std::string> VectorSearchComponent::SearchInDb(
    const std::string& vec_str, int limit, double max_distance) const {
    try {
        auto result = pg_cluster_->Execute(
            userver::storages::postgres::ClusterHostType::kMaster,
            sql::kFindSimilarVectorExamples, vec_str, limit, max_distance);

        std::vector<std::string> svgs;
        svgs.reserve(result.Size());
        for (const auto& row : result) {
            svgs.push_back(row["svg_data"].As<std::string>());
        }
        return svgs;
    } catch (const std::exception& e) {
        LOG_WARNING() << "RAG vector search failed, continuing without examples: "
                      << e.what();
        return {};
    }
}

userver::yaml_config::Schema VectorSearchComponent::GetStaticConfigSchema() {
    return userver::yaml_config::MergeSchemas<
        userver::components::LoggableComponentBase
    >(R"(
type: object
description: Vector search component (Yandex embeddings + pgvector)
additionalProperties: false
properties:
    embedding-url:
        type: string
        description: URL of the Yandex embedding API
    pg-component-name:
        type: string
        description: Name of the postgres component
    embedding-model:
        type: string
        description: Embedding model name
    )");
}

} // namespace ya_brander::components
