UPDATE projects
SET is_private = FALSE, updated_at = NOW()
WHERE id = $1::uuid;
