INSERT INTO project_versions (project_id, version_num)
VALUES (
    $1::uuid,
    (
        SELECT COALESCE(MAX(version_num), 0) + 1
        FROM project_versions
        WHERE project_id = $1::uuid
    )
)
RETURNING id::text;
