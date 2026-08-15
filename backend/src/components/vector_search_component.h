#pragma once

#include <string>
#include <vector>

#include <userver/clients/http/client.hpp>
#include <userver/components/loggable_component_base.hpp>
#include <userver/storages/postgres/cluster.hpp>

namespace ya_brander::components {

class VectorSearchComponent : public userver::components::LoggableComponentBase {
public:
    static constexpr std::string_view kName = "vector-search";
    static constexpr const char* kFolderId = "b1gttnvmtpd0tagdlb7o";  
    static constexpr double kMaxDistance = 0.7;

    VectorSearchComponent(const userver::components::ComponentConfig& config,
                          const userver::components::ComponentContext& context);

    static userver::yaml_config::Schema GetStaticConfigSchema(); 
    
    virtual std::vector<std::string> FindSimilar(const std::string& description, int limit) const;

    virtual ~VectorSearchComponent() = default;

private:
    std::vector<double> GetEmbedding(const std::string& text) const;
    std::vector<std::string> SearchInDb(const std::string& vec_str, int limit, double max_distance) const;

    userver::storages::postgres::ClusterPtr pg_cluster_;
    userver::clients::http::Client& http_client_;
    std::string api_key_;
    std::string embedding_url_;
    std::string db_table_;
    std::string embedding_model_;
};

} // namespace ya_brander::components