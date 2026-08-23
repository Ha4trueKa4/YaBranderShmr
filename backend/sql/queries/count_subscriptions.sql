SELECT COUNT(*)
FROM subscriptions
WHERE subscriber_id = $1::uuid;
