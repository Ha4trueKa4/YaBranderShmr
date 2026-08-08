#include "item_validator.hpp"

namespace code_architecture {

std::optional<std::string> CommonItemValidator::Validate(
    const UserInfo* user, const ItemRecord&, int) const {
    if (!user) {
        return std::string("NO_USER");
    }
    return std::nullopt;
}

PrescriptionItemValidator::PrescriptionItemValidator(userver::storages::postgres::ClusterPtr pg)
    : pg_(std::move(pg)) {}

std::optional<std::string> PrescriptionItemValidator::Validate(
    const UserInfo* user, const ItemRecord& item, int user_id) const {
    if (!user) {
        return std::string("NO_USER_NO_PRESCRIPTION");
    }
    if (user->kind == UserKind::Doctor) {
        return std::nullopt;
    }
    auto result = pg_->Execute(
        userver::storages::postgres::ClusterHostType::kMaster,
        "SELECT 1 FROM code_architecture.prescription WHERE user_id = $1 AND item_id = $2",
        user_id, item.id);
    if (result.IsEmpty()) {
        return std::string("NO_PRESCRIPTION");
    }
    return std::nullopt;
}

std::optional<std::string> SpecialItemValidator::Validate(
    const UserInfo* user, const ItemRecord& item, int) const {
    if (!user) {
        return std::string("NO_USER_SPECIAL_ITEM");
    }
    if (user->kind != UserKind::Doctor) {
        return std::string("ITEM_IS_SPECIAL");
    }
    if (!user->specialty_id.has_value() || user->specialty_id.value() != item.specialty_id) {
        return std::string("ITEM_SPECIAL_WRONG_SPECIFIC");
    }
    return std::nullopt;
}

ItemValidatorFactory::ItemValidatorFactory(userver::storages::postgres::ClusterPtr pg)
    : prescription_(pg) {}

ItemValidatorBase* ItemValidatorFactory::GetValidator(const std::string& type) {
    if (type == "common") return &common_;
    if (type == "special") return &special_;
    if (type == "prescription") return &prescription_;
    return nullptr;
}

}  // namespace code_architecture