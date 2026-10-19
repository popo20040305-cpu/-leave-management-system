import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { BASE_URL, WHITE_LIST } from './config'
import { tokenStorage } from './storage'
import { ElMessage } from 'element-plus'

const service: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // FormData（销假照片 / 教师头像 / 学生头像上传）不能带 JSON 的 Content-Type。
    // axios 1.x 的默认 transformRequest 见到「Content-Type: application/json」
    // 就会把 FormData 先 JSON.stringify 一遍再发（见 lib/defaults/index.js 的
    // hasJSONContentType ? JSON.stringify(formDataToJSON(data)) : data），
    // 后端 @RequestParam MultipartFile 会因为收到的是 JSON 串而解析不到文件，
    // 报 400「销假时没有提交照片」。这里把 Content-Type 置空，
    // 让浏览器自己带 multipart/form-data; boundary=...。
    if (config.data instanceof FormData) {
      // delete 而不是赋 undefined：axios 合并默认头时 undefined 会被跳过，
      // 删掉才能真正去掉实例级的 application/json
      delete (config.headers as Record<string, unknown>)['Content-Type']
    }

    const isWhite = WHITE_LIST.some((url) => config.url?.includes(url) ?? false)

    if (!isWhite) {
      const token = tokenStorage.getToken()

      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      } else {
        ElMessage.warning('未登录或登录已过期，请重新登录')
        return Promise.reject(new Error('未登录或登录已过期，已重新跳转登录页'))
      }
    }

    return config
  },
  (error) => Promise.reject(error),
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const { data } = response
    if (data.code !== 200) {
      const message = data.msg || data.message || '请求失败，请稍后再试'
      ElMessage.error(message)
      const err = new Error(message)
      ;(err as Error & { handled?: boolean }).handled = true
      return Promise.reject(err)
    }
    return data.data as any
  },
  (error) => {
    if (error.response) {
      const { status } = error.response
      if (status === 401) {
        tokenStorage.removeToken()
        ElMessage.warning('登录已过期，请重新登录')
        window.location.href = '/login'
      } else if (status === 403) {
        ElMessage.error('没有权限访问该资源')
      } else if (status === 500) {
        ElMessage.error('服务器错误，请稍后再试')
      } else {
        ElMessage.error('请求失败，请稍后再试')
      }
      ;(error as Error & { handled?: boolean }).handled = true
    }
    return Promise.reject(error)
  },
)

function request<T = unknown>(config: AxiosRequestConfig): Promise<T> {
  return service.request(config) as Promise<T>
}

function get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return service.get(url, config) as Promise<T>
}

function post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return service.post(url, data, config) as Promise<T>
}

function put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
  return service.put(url, data, config) as Promise<T>
}

function del<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
  return service.delete(url, config) as Promise<T>
}

// 默认导出（包含所有方法）
export default {
  request,
  get,
  post,
  put,
  delete: del, // 将 delete 映射为 del
}

// 同时提供命名导出（按需导入）
export { request, get, post, put, del }
