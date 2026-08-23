SELECT chat_history
FROM projects
WHERE id = $1::uuid;
