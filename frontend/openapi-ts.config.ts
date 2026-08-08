import { defineConfig } from '@hey-api/openapi-ts';

export default defineConfig({
    input: '../swagger.yml',
    output: './src/shared/api/generated',
    plugins: ['@hey-api/typescript', 'zod'],
});
