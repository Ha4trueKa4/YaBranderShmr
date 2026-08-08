import { api } from '@shared/api/api';
import {
    zLoginRequest,
    zPostAuthLoginResponse,
    type AuthResponse,
    type LoginRequest,
} from '@shared/api/types';

export async function login(body: LoginRequest): Promise<AuthResponse> {
    const validBody = zLoginRequest.parse(body);
    const response = await api<unknown, LoginRequest>('/auth/login', {
        method: 'POST',
        body: validBody,
    });

    return zPostAuthLoginResponse.parse(response);
}
