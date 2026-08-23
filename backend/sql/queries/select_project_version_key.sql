SELECT s3_file_key
FROM project_versions
WHERE id = $1::uuid AND project_id = $2::uuid;
