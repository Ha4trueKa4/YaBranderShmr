#!/usr/bin/env bash
# Тестовый скрипт для /auth/register и /auth/login
# Использование: ./test_auth.sh [base_url]
# Требует: curl, jq

set -uo pipefail

BASE_URL="${1:-http://localhost:8080}"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

PASS=0
FAIL=0

# Случайные email/nickname, чтобы можно было запускать скрипт много раз подряд
RAND=$RANDOM$RANDOM
EMAIL="user${RAND}@example.com"
NICKNAME="user${RAND}"
PASSWORD="Passw0rd123"

check() {
    local name="$1"
    local expected_status="$2"
    local actual_status="$3"
    local body="$4"

    if [[ "$actual_status" == "$expected_status" ]]; then
        echo -e "${GREEN}[OK]${NC} $name (status $actual_status)"
        PASS=$((PASS + 1))
    else
        echo -e "${RED}[FAIL]${NC} $name (expected $expected_status, got $actual_status)"
        echo "       body: $body"
        FAIL=$((FAIL + 1))
    fi
}

# Выполняет запрос, печатает тело и статус в глобальные переменные RESP_BODY / RESP_STATUS
do_request() {
    local method="$1"
    local path="$2"
    local data="$3"
    local auth_header="${4:-}"

    local response
    if [[ -n "$auth_header" ]]; then
        response=$(curl -sS -X "$method" "${BASE_URL}${path}" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer ${auth_header}" \
            -d "$data" \
            -w "\n%{http_code}")
    else
        response=$(curl -sS -X "$method" "${BASE_URL}${path}" \
            -H "Content-Type: application/json" \
            -d "$data" \
            -w "\n%{http_code}")
    fi

    RESP_STATUS=$(echo "$response" | tail -n1)
    RESP_BODY=$(echo "$response" | sed '$d')
}

echo -e "${YELLOW}== Base URL: ${BASE_URL} ==${NC}"
echo -e "${YELLOW}== Тестовый пользователь: ${EMAIL} / ${NICKNAME} ==${NC}\n"

echo "--- POST /auth/register ---"

# 1. Успешная регистрация -> 201
do_request POST /auth/register \
    "{\"email\":\"${EMAIL}\",\"password\":\"${PASSWORD}\",\"nickname\":\"${NICKNAME}\"}"
check "Успешная регистрация" 201 "$RESP_STATUS" "$RESP_BODY"

ACCESS_TOKEN=$(echo "$RESP_BODY" | jq -r '.access_token // empty')
USER_ID=$(echo "$RESP_BODY" | jq -r '.user.id // empty')
echo "       token: ${ACCESS_TOKEN:0:20}..."
echo "       user_id: ${USER_ID}"

# 2. Повторная регистрация с тем же email -> 409 EMAIL_EXISTS
do_request POST /auth/register \
    "{\"email\":\"${EMAIL}\",\"password\":\"${PASSWORD}\",\"nickname\":\"another_${RAND}\"}"
check "Дубликат email -> 409" 409 "$RESP_STATUS" "$RESP_BODY"

# 3. Повторная регистрация с тем же nickname -> 409 NICKNAME_EXISTS
do_request POST /auth/register \
    "{\"email\":\"another_${RAND}@example.com\",\"password\":\"${PASSWORD}\",\"nickname\":\"${NICKNAME}\"}"
check "Дубликат nickname -> 409" 409 "$RESP_STATUS" "$RESP_BODY"

# 4. Невалидный email -> 400
do_request POST /auth/register \
    "{\"email\":\"not-an-email\",\"password\":\"${PASSWORD}\",\"nickname\":\"someone_${RAND}\"}"
check "Невалидный email -> 400" 400 "$RESP_STATUS" "$RESP_BODY"

# 5. Слабый пароль (короткий, без цифр) -> 400
do_request POST /auth/register \
    "{\"email\":\"weakpass_${RAND}@example.com\",\"password\":\"weak\",\"nickname\":\"someone2_${RAND}\"}"
check "Слабый пароль -> 400" 400 "$RESP_STATUS" "$RESP_BODY"

# 6. Слишком короткий nickname -> 400
do_request POST /auth/register \
    "{\"email\":\"shortnick_${RAND}@example.com\",\"password\":\"${PASSWORD}\",\"nickname\":\"ab\"}"
check "Слишком короткий nickname -> 400" 400 "$RESP_STATUS" "$RESP_BODY"

echo -e "\n--- POST /auth/login ---"

# 7. Успешный логин -> 200
do_request POST /auth/login \
    "{\"email\":\"${EMAIL}\",\"password\":\"${PASSWORD}\"}"
check "Успешный логин" 200 "$RESP_STATUS" "$RESP_BODY"

LOGIN_TOKEN=$(echo "$RESP_BODY" | jq -r '.access_token // empty')
echo "       token: ${LOGIN_TOKEN:0:20}..."

# 8. Неверный пароль -> 401
do_request POST /auth/login \
    "{\"email\":\"${EMAIL}\",\"password\":\"wrong-password\"}"
check "Неверный пароль -> 401" 401 "$RESP_STATUS" "$RESP_BODY"

# 9. Несуществующий email -> 401
do_request POST /auth/login \
    "{\"email\":\"no-such-user-${RAND}@example.com\",\"password\":\"${PASSWORD}\"}"
check "Несуществующий email -> 401" 401 "$RESP_STATUS" "$RESP_BODY"

# 10. Пустой пароль -> 401
do_request POST /auth/login \
    "{\"email\":\"${EMAIL}\",\"password\":\"\"}"
check "Пустой пароль -> 401" 401 "$RESP_STATUS" "$RESP_BODY"

echo -e "\n--- Проверка защищённого эндпоинта (пример) ---"
echo "Поправьте PROTECTED_PATH под свой реальный маршрут (например /projects)."

PROTECTED_PATH="/projects"

# 11. Запрос без токена (если ваш чекер допускает анонимов — ждём 200, иначе поправьте на 401)
do_request GET "$PROTECTED_PATH" "" ""
echo -e "${YELLOW}[INFO]${NC} Без токена: status=$RESP_STATUS body=$RESP_BODY"

# 12. Запрос с валидным токеном -> должен пройти (200/2xx)
if [[ -n "$ACCESS_TOKEN" ]]; then
    do_request GET "$PROTECTED_PATH" "" "$ACCESS_TOKEN"
    echo -e "${YELLOW}[INFO]${NC} С валидным токеном: status=$RESP_STATUS body=$RESP_BODY"
fi

# 13. Запрос с битым токеном -> 403 (ваш AuthCheckerJwt отдаёт kForbidden на невалидный JWT)
do_request GET "$PROTECTED_PATH" "" "this.is.not.a.valid.jwt"
check "Битый токен на защищённом эндпоинте -> 403" 403 "$RESP_STATUS" "$RESP_BODY"

echo -e "\n${YELLOW}== Итого: ${GREEN}${PASS} passed${NC}, ${RED}${FAIL} failed${NC} ==${NC}"

if [[ "$FAIL" -gt 0 ]]; then
    exit 1
fi
