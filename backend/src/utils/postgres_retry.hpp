#pragma once

#include <chrono>
#include <string>
#include <string_view>

#include <userver/engine/sleep.hpp>
#include <userver/logging/log.hpp>
#include <userver/server/handlers/exceptions.hpp>
#include <userver/server/http/http_error.hpp>
#include <userver/server/http/http_status.hpp>
#include <userver/storages/postgres/exceptions.hpp>
#include <userver/utils/rand.hpp>

namespace ya_brander::utils {

namespace pg = userver::storages::postgres;

class DatabaseUnavailableError final
    : public userver::server::http::CustomHandlerException {
 public:
  DatabaseUnavailableError(std::string_view operation, std::string_view cause)
      : CustomHandlerException(
            userver::server::handlers::HandlerErrorCode::kServerSideError,
            userver::server::http::HttpStatus::kServiceUnavailable,
            MessageBuilder{operation, cause}) {}

 private:
  class MessageBuilder final {
   public:
    static constexpr bool kIsExternalBodyFormatted = true;

    MessageBuilder(std::string_view operation, std::string_view cause)
        : internal_message_("PostgreSQL SELECT '") {
      internal_message_.append(operation);
      internal_message_.append("' failed after retry: ");
      internal_message_.append(cause);
    }

    std::string GetServiceCode() const { return "DATABASE_UNAVAILABLE"; }

    std::string GetExternalBody() const {
      return R"({"code":"DATABASE_UNAVAILABLE","message":"Database is temporarily unavailable"})";
    }

    const std::string& GetInternalMessage() const { return internal_message_; }

   private:
    std::string internal_message_;
  };
};

inline bool IsTransientPostgresError(const pg::RuntimeError& error) {
  return dynamic_cast<const pg::ConnectionError*>(&error) != nullptr ||
         dynamic_cast<const pg::PoolError*>(&error) != nullptr ||
         dynamic_cast<const pg::CannotConnectNow*>(&error) != nullptr ||
         dynamic_cast<const pg::AdminShutdown*>(&error) != nullptr ||
         dynamic_cast<const pg::CrashShutdown*>(&error) != nullptr;
}

template <typename Callable>
auto ExecuteSelectWithRetry(std::string_view operation, Callable&& execute) {
  try {
    return execute();
  } catch (const pg::RuntimeError& error) {
    if (!IsTransientPostgresError(error)) {
      throw;
    }
    LOG_WARNING() << "PostgreSQL SELECT '" << operation
                  << "' failed, retrying once: " << error.what();
  }

  const auto retry_delay = std::chrono::milliseconds{
      userver::utils::RandRange(50, 151)};
  userver::engine::InterruptibleSleepFor(retry_delay);

  try {
    return execute();
  } catch (const pg::RuntimeError& error) {
    if (!IsTransientPostgresError(error)) {
      throw;
    }
    LOG_ERROR() << "PostgreSQL SELECT '" << operation
                << "' failed after retry: " << error.what();
    throw DatabaseUnavailableError(operation, error.what());
  }
}

}  // namespace ya_brander::utils
