import { createApiClient } from './client';

export { ApiError, createApiClient } from './client';
export type { ApiRequestOptions, QueryParams } from './client';
export type { ApiResult } from './types';
export * from './generated';
export * from './schemas';


const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1';

export const api = createApiClient(API_URL);
