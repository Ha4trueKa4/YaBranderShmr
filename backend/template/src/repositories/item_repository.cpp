#include "item_repository.hpp"

namespace code_architecture {

CommonItemRepository::CommonItemRepository(userver::storages::postgres::ClusterPtr pg)
    : pg_(std::move(pg)) {}

std::optional<ItemRecord> CommonItemRepository::FindItem(const ItemId& item) const {
    auto result = pg_->Execute(
        userver::storages::postgres::ClusterHostType::kMaster,
        "SELECT id FROM code_architecture.common_item WHERE id = $1", item.id);
    if (!result.IsEmpty()) {
        return ItemRecord{item.id, std::nullopt};
    }
    return std::nullopt;
}

SpecialItemRepository::SpecialItemRepository(userver::storages::postgres::ClusterPtr pg)
    : pg_(std::move(pg)) {}

std::optional<ItemRecord> SpecialItemRepository::FindItem(const ItemId& item) const {
    auto result = pg_->Execute(
        userver::storages::postgres::ClusterHostType::kMaster,
        "SELECT id, specialty_id FROM code_architecture.special_item WHERE id = $1", item.id);
    if (!result.IsEmpty()) {
        auto row = result.Front();
        int spec_id = row["specialty_id"].As<int>();
        return ItemRecord{item.id, spec_id};
    }
    return std::nullopt;
}

PrescriptionItemRepository::PrescriptionItemRepository(userver::storages::postgres::ClusterPtr pg)
    : pg_(std::move(pg)) {}

std::optional<ItemRecord> PrescriptionItemRepository::FindItem(const ItemId& item) const {
    auto result = pg_->Execute(
        userver::storages::postgres::ClusterHostType::kMaster,
        "SELECT id FROM code_architecture.prescription_item WHERE id = $1", item.id);
    if (!result.IsEmpty()) {
        return ItemRecord{item.id, std::nullopt};
    }
    return std::nullopt;
}

ItemRepositoryFactory::ItemRepositoryFactory(userver::storages::postgres::ClusterPtr pg)
    : common_(pg), special_(pg), prescription_(pg) {}

ItemRepositoryBase* ItemRepositoryFactory::GetRepository(const std::string& type) {
    if (type == "common") return &common_;
    if (type == "special") return &special_;
    if (type == "prescription") return &prescription_;
    return nullptr;
}

}  // namespace code_architecture