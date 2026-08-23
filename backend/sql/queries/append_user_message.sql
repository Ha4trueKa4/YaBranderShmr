UPDATE projects
SET chat_history = chat_history || jsonb_build_array(
        jsonb_build_object(
            'role', 'user',
            'text', $2::text,
            'job_id', $3::text,
            'created_at', NOW()
        )
    ),
    updated_at = NOW()
WHERE id = $1::uuid;
