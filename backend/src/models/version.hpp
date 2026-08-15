#pragma once
#include <cstdint>
#include <string>
#include <userver/storages/postgres/io/chrono.hpp>

namespace ya_brander::models {

    /// @brief Метаданные одной версии проекта.
    struct Version {
        std::string id;
        std::string project_id;
        std::int32_t version_num;
        userver::storages::postgres::TimePointTz created_at;
    };

}  // namespace ya_brander::models