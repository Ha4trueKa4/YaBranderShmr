SELECT id::text,
       author_id::text,
       name,
       COALESCE(description, '') AS description,
       is_private,
       created_at
FROM projects
WHERE id = $1::uuid;
