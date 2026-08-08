import { api } from '@shared/api/api';
import {
    zPostAuthRegisterResponse,
    zRegisterRequest,
    type AuthResponse,
    type RegisterRequest,
} from '@shared/api/types';

export async function register(body: RegisterRequest): Promise<AuthResponse> {
    const validBody = zRegisterRequest.parse(body);
    const response = await api<unknown, RegisterRequest>('/auth/register', {
        method: 'POST',
        body: validBody,
    });

    return zPostAuthRegisterResponse.parse(response);
}
