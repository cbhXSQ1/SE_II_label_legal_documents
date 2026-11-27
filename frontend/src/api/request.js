import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getCredential } from '@/utils/session'
import { createMockAdapter } from '@/mock/adapter'
import { getDb } from '@/mock/db'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

const instance = axios.create({
  baseURL: '/app',
  timeout: 10000
})

// 内置 Mock：用一个自定义 adapter 拦截请求，返回统一 AppResult
if (USE_MOCK) {
  instance.defaults.adapter = createMockAdapter(getDb())
}

// 请求拦截：带上登录凭证
instance.interceptors.request.use((config) => {
  const cred = getCredential()
  if (cred) {
    config.headers = config.headers || {}
    config.headers.Authorization = cred
  }
  return config
})

// 响应拦截：解包统一 AppResult { success, message, data, allowedActions }
instance.interceptors.response.use(
  (res) => {
    const r = res.data
    if (r && typeof r.success === 'boolean') {
      if (!r.success) {
        ElMessage.error(r.message || '操作失败')
        return Promise.reject(new Error(r.message || '操作失败'))
      }
      return r.data
    }
    return res.data
  },
  (err) => {
    const msg = err?.response?.data?.message || err?.message || '网络连接失败，请重试'
    ElMessage.error(msg)
    return Promise.reject(err)
  }
)

export default instance
