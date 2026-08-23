SELECT u.id::text, u.login AS name
FROM subscriptions s
JOIN users u ON u.id = s.subscriber_id
WHERE s.target_user_id = $1::uuid
ORDER BY s.created_at DESC;
