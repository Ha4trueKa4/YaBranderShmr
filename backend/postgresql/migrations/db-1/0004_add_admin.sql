DO $$
DECLARE
    admin_email text := current_setting('my.admin_email', true);
    admin_password_hash text := current_setting('my.admin_password_hash', true);
    admin_id uuid := '00000000-0000-0000-0000-000000000000';
BEGIN
    IF admin_email IS NULL OR admin_password_hash IS NULL OR
       admin_email = '' OR admin_password_hash = '' THEN
        RAISE NOTICE 'Admin email or password hash not set, skipping admin creation';
        RAISE NOTICE 'Please set ADMIN_EMAIL and ADMIN_PASSWORD_HASH environment variables';
        RETURN;
    END IF;

    INSERT INTO users (id, login, email, password_hash, provider, provider_id, created_at) VALUES (
        admin_id,
        'admin',
        admin_email,
        admin_password_hash,
        NULL,
        NULL,
        NOW()
    ) ON CONFLICT (id) DO UPDATE SET
        login = EXCLUDED.login,
        email = EXCLUDED.email,
        password_hash = EXCLUDED.password_hash,
        provider = NULL,
        provider_id = NULL,
        created_at = EXCLUDED.created_at;

    RAISE NOTICE 'Admin user successfully created/updated with ID: %', admin_id;
    RAISE NOTICE 'Email: %', admin_email;
END $$;