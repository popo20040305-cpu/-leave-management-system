export const BASE_URL = 'http://localhost:8089/qingjia'

/**
 * 无需携带 token 的接口白名单（按请求 url 做包含匹配）。
 *
 * 修复：原来写的是 'api/login' / 'api/logout'，但 loginApi 实际请求的 url 是 '/login'，
 * '/login'.includes('api/login') === false，于是登录请求被自己的请求拦截器判定为
 * "未登录"直接 reject，请求根本发不到后端。
 */
export const WHITE_LIST = ['/api/login', '/api/logout']
