SELECT id::text
FROM users
WHERE id = $1::uuid;
