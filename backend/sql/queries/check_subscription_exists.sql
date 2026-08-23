SELECT 1
FROM subscriptions
WHERE subscriber_id = $1::uuid AND target_user_id = $2::uuid;
