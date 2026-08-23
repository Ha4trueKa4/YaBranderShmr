#pragma once
#include <stdexcept>
#include <string>

#include <userver/clients/http/client.hpp>

namespace ya_brander::auth {

struct YandexProfile {
    std::string provider_id;
    std::string email;
    std::string login;
};

// code/access_token, присланный клиентом, сам по себе невалиден, просрочен
// или не проходит проверку подписи (JWT-флоу) — вина на входных данных
// запроса, а не на доступности Yandex API. Хендлеры должны мапить это на
// 400, а не на 502 (в отличие от сетевых сбоев/5xx со стороны Яндекса).
class InvalidYandexCredentialError final : public std::runtime_error {
public:
    explicit InvalidYandexCredentialError(const std::string& message)
        : std::runtime_error(message) {}
};

// Общая логика похода во внешние сервисы Yandex ID: обмен authorization
// code на access_token (веб-флоу) и получение профиля пользователя — либо
// из JSON (после обмена code), либо из JWT, подписанного client_secret'ом
// приложения (SDK-флоу, access_token уже финальный). Используется и во
// входе/регистрации через Yandex ID (/auth/yandex), и в привязке Yandex ID
// к существующему email-аккаунту (/auth/yandex/link).
class YandexProfileClient {
public:
    explicit YandexProfileClient(userver::clients::http::Client& http_client);

    // Ровно один из двух параметров должен быть непустым: code — веб-флоу,
    // access_token — SDK-флоу.
    YandexProfile FetchProfile(const std::string& code, const std::string& access_token) const;

private:
    std::string ExchangeCodeForToken(const std::string& code) const;
    YandexProfile FetchProfileFromJson(const std::string& access_token) const;
    YandexProfile FetchProfileFromJwt(const std::string& access_token) const;

    userver::clients::http::Client& http_client_;
    std::string client_id_;
    std::string client_secret_;
};

}  // namespace ya_brander::auth
