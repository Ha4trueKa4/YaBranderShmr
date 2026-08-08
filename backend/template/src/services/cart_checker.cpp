#include "cart_checker.hpp"
#include "models/item_id.hpp"

namespace code_architecture {

CartChecker::CartChecker(userver::storages::postgres::ClusterPtr pg)
    : item_repo_factory_(pg),
      validator_factory_(pg),
      pg_(pg) {
    auto composite = std::make_unique<CompositeUserResolver>();
    composite->AddResolver(std::make_unique<DoctorAccountResolver>(pg_));
    composite->AddResolver(std::make_unique<UserAccountResolver>(pg_));
    user_resolver_ = std::move(composite);
}

std::vector<Problem> CartChecker::Check(int user_id, const std::vector<std::string>& item_ids) {
    std::vector<Problem> problems;
    auto user_opt = user_resolver_->FindUser(user_id);
    const UserInfo* user_ptr = user_opt ? &*user_opt : nullptr;

    for (const auto& raw_item : item_ids) {
        auto parsed = ParseItemId(raw_item);
        if (std::holds_alternative<ParseError>(parsed)) {
            auto err = std::get<ParseError>(parsed);
            std::string code = (err == ParseError::WRONG_CATEGORY) ? "WRONG_CATEGORY" : "INCORRECT_ITEM_ID";
            std::string lower_raw = raw_item;
            for (char& c : lower_raw) c = static_cast<char>(std::tolower(static_cast<unsigned char>(c)));
            problems.push_back({lower_raw, code});
            continue;
        }

        ItemId item = std::move(std::get<ItemId>(parsed));
        std::string lower_raw = raw_item;
        for (char& c : lower_raw) c = static_cast<char>(std::tolower(static_cast<unsigned char>(c)));

        auto* repo = item_repo_factory_.GetRepository(item.type);
        if (!repo) {
            problems.push_back({lower_raw, "WRONG_CATEGORY"});
            continue;
        }
        auto item_record = repo->FindItem(item);
        if (!item_record) {
            problems.push_back({lower_raw, "ITEM_NOT_FOUND"});
            continue;
        }

        auto* validator = validator_factory_.GetValidator(item.type);
        if (!validator) {
            problems.push_back({lower_raw, "WRONG_CATEGORY"});
            continue;
        }
        auto problem = validator->Validate(user_ptr, *item_record, user_id);
        if (problem) {
            problems.push_back({lower_raw, *problem});
        }
    }
    return problems;
}

}  // namespace code_architecture