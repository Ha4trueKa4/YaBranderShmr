#pragma once
#include "repositories/user_resolver.hpp"
#include "repositories/item_repository.hpp"
#include "validation/item_validator.hpp"

#include <userver/storages/postgres/cluster.hpp>
#include <memory>
#include <vector>
#include <string>

namespace code_architecture {

struct Problem {
    std::string item_id;
    std::string problem;
};

class CartChecker {
public:
    CartChecker(userver::storages::postgres::ClusterPtr pg);
    std::vector<Problem> Check(int user_id, const std::vector<std::string>& item_ids);
private:
    std::unique_ptr<UserResolverBase> user_resolver_;
    ItemRepositoryFactory item_repo_factory_;
    ItemValidatorFactory validator_factory_;
    userver::storages::postgres::ClusterPtr pg_;
};

}  // namespace code_architecture