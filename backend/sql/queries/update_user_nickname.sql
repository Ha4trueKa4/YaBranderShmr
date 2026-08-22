UPDATE users
SET login = $1
WHERE id = $2::uuid
RETURNING id::text, login;
