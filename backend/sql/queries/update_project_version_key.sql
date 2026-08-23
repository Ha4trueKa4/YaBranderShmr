UPDATE project_versions
SET s3_file_key = $2
WHERE id = $1::uuid;
