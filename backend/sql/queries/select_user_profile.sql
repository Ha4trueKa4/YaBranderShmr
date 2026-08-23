SELECT id::text, login
FROM users
WHERE id = $1::uuid;
