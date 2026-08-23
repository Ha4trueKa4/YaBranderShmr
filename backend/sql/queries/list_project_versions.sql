SELECT id::text, project_id::text, version_num, created_at
FROM project_versions
WHERE project_id = $1::uuid AND s3_file_key IS NOT NULL
ORDER BY version_num DESC;
