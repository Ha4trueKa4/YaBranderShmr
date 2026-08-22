SELECT id::text,
       author_id::text,
       name,
       COALESCE(description, '') AS description,
       is_private,
       created_at,
       last_version_key
FROM projects
WHERE author_id = $1::uuid
ORDER BY created_at DESC;
