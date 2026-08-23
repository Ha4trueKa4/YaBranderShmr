SELECT u.id::text, u.login AS name
FROM subscriptions s
JOIN users u ON u.id = s.target_user_id
WHERE s.subscriber_id = $1::uuid
ORDER BY s.created_at DESC;
