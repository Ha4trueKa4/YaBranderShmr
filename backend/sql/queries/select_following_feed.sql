SELECT p.id::text AS id,
       p.author_id::text AS author_id,
       u.login AS author_login,
       p.name,
       p.created_at,
       COALESCE(p.last_version_key, '') AS last_version_key
FROM projects p
JOIN users u ON u.id = p.author_id
JOIN subscriptions s ON s.target_user_id = p.author_id
WHERE p.is_private = FALSE
  AND s.subscriber_id = $4::uuid
  AND ($1::timestamptz IS NULL OR (p.created_at, p.id) < ($1::timestamptz, $2::uuid))
ORDER BY p.created_at DESC, p.id DESC
LIMIT $3;
