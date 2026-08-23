UPDATE projects
SET last_version_key = $2, updated_at = NOW()
WHERE id = $1::uuid;
