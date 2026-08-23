INSERT INTO subscriptions (subscriber_id, target_user_id)
VALUES ($1::uuid, $2::uuid)
ON CONFLICT DO NOTHING;
