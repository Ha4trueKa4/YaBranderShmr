SELECT COUNT(*)
FROM subscriptions
WHERE target_user_id = $1::uuid;
