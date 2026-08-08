#pragma once
#include "models/user_info.hpp"
#include <userver/storages/postgres/cluster.hpp>
#include <memory>
#include <optional>
#include <vector>

namespace code_architecture {


class UserResolverBase {
public:
    virtual ~UserResolverBase() = default;
    virtual std::optional<UserInfo> FindUser(int user_id) const = 0;
};


class DoctorAccountResolver : public UserResolverBase {
public:
    explicit DoctorAccountResolver(userver::storages::postgres::ClusterPtr pg);
    std::optional<UserInfo> FindUser(int user_id) const override;
private:
    userver::storages::postgres::ClusterPtr pg_;
};


class UserAccountResolver : public UserResolverBase {
public:
    explicit UserAccountResolver(userver::storages::postgres::ClusterPtr pg);
    std::optional<UserInfo> FindUser(int user_id) const override;
private:
    userver::storages::postgres::ClusterPtr pg_;
};


class CompositeUserResolver : public UserResolverBase {
public:
    void AddResolver(std::unique_ptr<UserResolverBase> resolver);
    std::optional<UserInfo> FindUser(int user_id) const override;
private:
    std::vector<std::unique_ptr<UserResolverBase>> resolvers_;
};

}  // namespace code_architecture