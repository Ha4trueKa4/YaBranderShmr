POST   /auth/register
POST   /auth/login
GET    /users/{id}                          — профиль пользователя
GET    /users/{id}/projects                 — проекты пользователя

POST   /projects                            — создать проект
GET    /projects/{id}
PATCH  /projects/{id}                       — обновить метаданные / видимость
DELETE /projects/{id}

POST   /projects/{id}/generate             — первичная генерация через LLM
POST   /projects/{id}/chat                 — редактирование через чат с ИИ

GET    /projects/{id}/versions             — история изменений
GET    /projects/{id}/versions/{vid}       — конкретная версия
POST   /projects/{id}/versions/{vid}/restore

GET    /feed                               — общая лента (пагинация)
GET    /feed/subscriptions                 — лента подписок

POST   /users/{id}/subscribe
DELETE /users/{id}/subscribe
GET    /users/{id}/subscribers
GET    /users/{id}/subscriptions