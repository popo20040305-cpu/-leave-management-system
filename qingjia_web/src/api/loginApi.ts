import request from '@/utils/request'
import type { LoginUserInfo } from '@/type/LoginUserInfo'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  loginUserInfo: LoginUserInfo
}

export function loginApi(params: LoginParams): Promise<LoginResponse> {
  return request.post<LoginResponse>('/api/login', params)
}

/** 退出登录 */
export function logoutApi(): Promise<null> {
  return request.get<null>('/api/logout')
}

export interface UpdatePasswordParams {
  oldPassword: string
  newPassword: string
}

/**
 * 修改当前登录用户的密码。
 * 后端改密成功后会作废当前 token，所以调用方拿到成功响应后必须清本地登录态并回登录页。
 */
export function updatePasswordApi(params: UpdatePasswordParams): Promise<null> {
  return request.post<null>('/api/updatepassword', params)
}
