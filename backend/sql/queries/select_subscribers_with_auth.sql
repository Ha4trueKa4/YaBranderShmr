SELECT u.id::text,
       u.login AS name,
       EXISTS (
           SELECT 1
           FROM subscriptions auth_sub
           WHERE auth_sub.subscriber_id = $2::uuid
             AND auth_sub.target_user_id = u.id
       ) AS is_subscribed
FROM subscriptions s
JOIN users u ON u.id = s.subscriber_id
WHERE s.target_user_id = $1::uuid
ORDER BY s.created_at DESC;
