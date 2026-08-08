import { createApiClient } from './client';

// export type { ApiError, ApiResult } from './types';

const API_URL = import.meta.env.DEV ? import.meta.env.VITE_API_URL : import.meta.env.VITE_API_URL;
// TODO FIXME Подставить URL

export const api = createApiClient(API_URL);
