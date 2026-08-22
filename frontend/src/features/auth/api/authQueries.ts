import type {
  LoginRequest,
  RegisterRequest,
  YandexAuthRequest,
} from '@shared/api/types'
import { useMutation } from '@tanstack/react-query'

import { login } from './login'
import { register } from './register'
import { loginWithYandex } from './yandexLogin'

export function useLoginMutation() {
  return useMutation({
    mutationKey: ['auth', 'login'],
    mutationFn: (body: LoginRequest) => login(body),
  })
}

export function useRegisterMutation() {
  return useMutation({
    mutationKey: ['auth', 'register'],
    mutationFn: (body: RegisterRequest) => register(body),
  })
}

export function useYandexLoginMutation() {
  return useMutation({
    mutationKey: ['auth', 'yandex'],
    mutationFn: (body: YandexAuthRequest) => loginWithYandex(body),
  })
}
