#pragma once
#include "models/item_id.hpp"
#include <userver/storages/postgres/cluster.hpp>
#include <optional>
#include <memory>

namespace code_architecture {

struct ItemRecord {
    int id;
    std::optional<int> specialty_id;
};

class ItemRepositoryBase {
public:
    virtual ~ItemRepositoryBase() = default;
    virtual std::optional<ItemRecord> FindItem(const ItemId& item) const = 0;
};

class CommonItemRepository : public ItemRepositoryBase {
public:
    explicit CommonItemRepository(userver::storages::postgres::ClusterPtr pg);
    std::optional<ItemRecord> FindItem(const ItemId& item) const override;
private:
    userver::storages::postgres::ClusterPtr pg_;
};

class SpecialItemRepository : public ItemRepositoryBase {
public:
    explicit SpecialItemRepository(userver::storages::postgres::ClusterPtr pg);
    std::optional<ItemRecord> FindItem(const ItemId& item) const override;
private:
    userver::storages::postgres::ClusterPtr pg_;
};

class PrescriptionItemRepository : public ItemRepositoryBase {
public:
    explicit PrescriptionItemRepository(userver::storages::postgres::ClusterPtr pg);
    std::optional<ItemRecord> FindItem(const ItemId& item) const override;
private:
    userver::storages::postgres::ClusterPtr pg_;
};

class ItemRepositoryFactory {
public:
    explicit ItemRepositoryFactory(userver::storages::postgres::ClusterPtr pg);
    ItemRepositoryBase* GetRepository(const std::string& type);
private:
    CommonItemRepository common_;
    SpecialItemRepository special_;
    PrescriptionItemRepository prescription_;
};

}  // namespace code_architecture