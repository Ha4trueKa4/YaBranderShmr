SELECT last_version_key, is_private
FROM projects
WHERE id = $1::uuid;
