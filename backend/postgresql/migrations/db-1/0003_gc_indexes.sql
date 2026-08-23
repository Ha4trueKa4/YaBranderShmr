CREATE INDEX IF NOT EXISTS idx_project_versions_s3_file_key
    ON project_versions (s3_file_key)
    WHERE s3_file_key IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_projects_last_version_key
    ON projects (last_version_key)
    WHERE last_version_key IS NOT NULL;
