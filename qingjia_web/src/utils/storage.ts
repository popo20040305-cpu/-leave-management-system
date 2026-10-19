import type { LoginUserInfo } from '../type/LoginUserInfo'

const STORAGE_KEY = 'qingjia_app'

export const storage = {
        set<T>(key: string, value: T) {
            localStorage.setItem(`${STORAGE_KEY}_${key}`, JSON.stringify(value))
     },
     get<T>(key: string,defaultValue: T|null=null): T | null {
        try{
            const data = localStorage.getItem(`${STORAGE_KEY}_${key}`)
            return data? (JSON.parse(data) as T):defaultValue
        }catch{
            return defaultValue
        }
    },
    remove(key: string) {
        localStorage.removeItem(`${STORAGE_KEY}_${key}`)
    },
    clear() {
        localStorage.clear()
    }
}

export const tokenStorage = {
        getToken(): string | null {
            return storage.get<string>('token')
        },
        setToken(token: string) {
            storage.set('token', token)
        },
        removeToken() {
            storage.remove('token')
        },

}

    // 用户信息储存
    export const userStorage = {
        set(info: LoginUserInfo) {
            storage.set('userInfo', info)
        },
        get<T = LoginUserInfo>(): T | null {
            return storage.get<T>('userInfo')
        },
        remove() {
            storage.remove('userInfo')
        }

    }

