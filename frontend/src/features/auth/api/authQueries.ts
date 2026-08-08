import { useMutation } from '@tanstack/react-query';
import type { LoginRequest, RegisterRequest } from '@shared/api/types';

import { login } from './login';
import { register } from './register';

export function useLoginMutation() {
    return useMutation({
        mutationKey: ['auth', 'login'],
        mutationFn: (body: LoginRequest) => login(body),
    });
}

export function useRegisterMutation() {
    return useMutation({
        mutationKey: ['auth', 'register'],
        mutationFn: (body: RegisterRequest) => register(body),
    });
}
