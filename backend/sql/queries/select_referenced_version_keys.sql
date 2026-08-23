SELECT s3_file_key AS k
FROM project_versions
WHERE s3_file_key IS NOT NULL
UNION
SELECT last_version_key AS k
FROM projects
WHERE last_version_key IS NOT NULL;
