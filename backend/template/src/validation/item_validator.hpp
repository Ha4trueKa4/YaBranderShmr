#pragma once
#include "models/item_id.hpp"
#include "models/user_info.hpp"
#include "repositories/item_repository.hpp"
#include <userver/storages/postgres/cluster.hpp>
#include <optional>
#include <string>

namespace code_architecture {

class ItemValidatorBase {
public:
    virtual ~ItemValidatorBase() = default;
    virtual std::optional<std::string> Validate(
        const UserInfo* user, const ItemRecord& item, int user_id) const = 0;
};

class CommonItemValidator : public ItemValidatorBase {
public:
    std::optional<std::string> Validate(
        const UserInfo* user, const ItemRecord& item, int user_id) const override;
};

class PrescriptionItemValidator : public ItemValidatorBase {
public:
    explicit PrescriptionItemValidator(userver::storages::postgres::ClusterPtr pg);
    std::optional<std::string> Validate(
        const UserInfo* user, const ItemRecord& item, int user_id) const override;
private:
    userver::storages::postgres::ClusterPtr pg_;
};

class SpecialItemValidator : public ItemValidatorBase {
public:
    std::optional<std::string> Validate(
        const UserInfo* user, const ItemRecord& item, int user_id) const override;
};

class ItemValidatorFactory {
public:
    explicit ItemValidatorFactory(userver::storages::postgres::ClusterPtr pg);
    ItemValidatorBase* GetValidator(const std::string& type);
private:
    CommonItemValidator common_;
    PrescriptionItemValidator prescription_;
    SpecialItemValidator special_;
};

}  // namespace code_architecture