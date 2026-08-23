INSERT INTO projects (author_id, name, description, is_private)
VALUES ($1::uuid, $2, $3, TRUE)
RETURNING id::text, created_at;
