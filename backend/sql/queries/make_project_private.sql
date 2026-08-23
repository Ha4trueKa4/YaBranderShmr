UPDATE projects
SET is_private = TRUE, updated_at = NOW()
WHERE id = $1::uuid;
