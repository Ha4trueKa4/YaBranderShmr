SELECT provider_id
FROM users
WHERE id = $1::uuid;
