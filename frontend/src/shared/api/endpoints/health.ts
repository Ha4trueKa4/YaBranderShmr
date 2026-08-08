import { api } from '../api/instance.ts';

const HEALTH_URL = import.meta.env.VITE_HEALTH_URL ?? 'http://51.250.38.122:8080';

export function getHealth(): Promise<string> {
    return api(HEALTH_URL);
}
