SELECT is_private, last_version_key
FROM projects
WHERE id = $1::uuid
FOR UPDATE;
