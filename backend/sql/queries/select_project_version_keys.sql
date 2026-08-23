SELECT s3_file_key
FROM project_versions
WHERE project_id = $1::uuid AND s3_file_key IS NOT NULL;
