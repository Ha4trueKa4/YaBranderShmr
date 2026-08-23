SELECT password_hash, provider
FROM users
WHERE id = $1::uuid;
