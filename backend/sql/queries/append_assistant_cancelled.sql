UPDATE projects
SET chat_history = chat_history || jsonb_build_array(
    jsonb_build_object(
        'role', 'assistant',
        'status', 'cancelled',
        'job_id', $2::text,
        'created_at', NOW()
    )
),
updated_at = NOW()
WHERE id = $1::uuid;
