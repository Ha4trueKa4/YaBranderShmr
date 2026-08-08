#include "user_resolver.hpp"
#include <userver/storages/postgres/cluster.hpp>

namespace code_architecture {

DoctorAccountResolver::DoctorAccountResolver(userver::storages::postgres::ClusterPtr pg)
    : pg_(std::move(pg)) {}

std::optional<UserInfo> DoctorAccountResolver::FindUser(int user_id) const {
    auto result = pg_->Execute(
        userver::storages::postgres::ClusterHostType::kMaster,
        "SELECT id, specialty_id FROM code_architecture.doctor_account WHERE id = $1",
        user_id);
    if (!result.IsEmpty()) {
        auto row = result.Front();
        int id = row["id"].As<int>();
        int spec_id = row["specialty_id"].As<int>();
        return UserInfo{id, UserKind::Doctor, spec_id};
    }
    return std::nullopt;
}

UserAccountResolver::UserAccountResolver(userver::storages::postgres::ClusterPtr pg)
    : pg_(std::move(pg)) {}

std::optional<UserInfo> UserAccountResolver::FindUser(int user_id) const {
    auto result = pg_->Execute(
        userver::storages::postgres::ClusterHostType::kMaster,
        "SELECT id FROM code_architecture.user_account WHERE id = $1",
        user_id);
    if (!result.IsEmpty()) {
        int id = result.AsSingleRow<int>();
        return UserInfo{id, UserKind::Regular, std::nullopt};
    }
    return std::nullopt;
}

void CompositeUserResolver::AddResolver(std::unique_ptr<UserResolverBase> resolver) {
    resolvers_.push_back(std::move(resolver));
}

std::optional<UserInfo> CompositeUserResolver::FindUser(int user_id) const {
    for (const auto& r : resolvers_) {
        auto user = r->FindUser(user_id);
        if (user) return user;
    }
    return std::nullopt;
}

}  // namespace code_architecture