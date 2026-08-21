#pragma once
#include <optional>
#include <string>

namespace ya_brander::models {

    /// @brief Снимок состояния фоновой задачи генерации на момент опроса.
    struct GenerationJob {
        std::string id;
        std::string project_id;
        std::string status;  // "pending" | "done" | "error"
        std::optional<std::string> result_url;
        std::optional<std::string> error_message;
    };

}  // namespace ya_brander::models
