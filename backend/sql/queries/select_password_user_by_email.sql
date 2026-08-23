SELECT 1
FROM users
WHERE email = $1 AND provider_id IS NULL;
