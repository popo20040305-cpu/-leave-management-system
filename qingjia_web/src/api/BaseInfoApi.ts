import request from '@/utils/request'
import type { BaseInfoData, BaseInfoKey } from '@/type/BaseInfoType'

/**
 * 公共基础数据（字典）接口。
 *
 * 对应老项目 BaseController + DataInitializer 那套：老页面每次渲染时从 ServletContext
 * 里取 colleges / grades / sexTypes... 塞进 Model；前后端分离之后改成登录时调一次、
 * 存进 baseInfoStore 全局复用，各业务页的下拉框直接读它。
 *
 * 这个接口挂在 /api/** 下，由后端 bearerFilter 保护，必须带 token：
 * 学院列表是按登录用户过滤的（学生只看本班所属学院、教师只看本学院、
 * ROLE_TYPE_ALL 看全部），匿名请求拿不到正确结果。
 *
 * 原来这个文件写到 import 就断了，而且从 '@/type/college' 导入 College ——
 * 那个文件不存在，type-check 直接报 TS2307。College 类型已经补进
 * '@/type/BaseInfoType' 里的 CollegePojo。
 */

/** 一次拉全部字典，应用启动时调用 */
export function getBaseInfoApi(): Promise<BaseInfoData> {
  return request.get<BaseInfoData>('/api/base/all')
}

/**
 * 只拉指定字典。用于某个字典增删改之后的局部刷新，
 * 避免为了刷新一个学院列表把八个字典全查一遍。
 * 后端 GET /api/base/all?keys=colleges,sexTypes
 */
export function getBaseInfoByKeysApi(keys: BaseInfoKey[]): Promise<Partial<BaseInfoData>> {
  return request.get<Partial<BaseInfoData>>('/api/base/all', {
    params: { keys: keys.join(',') },
  })
}
