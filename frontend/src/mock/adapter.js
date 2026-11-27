import { matchRoute } from './handlers'

// 自定义 axios adapter：拦截请求、路由到 mock handler、返回统一 AppResult
export function createMockAdapter(db) {
  return function adapter(config) {
    return new Promise((resolve, reject) => {
      // 模拟网络延迟
      setTimeout(() => {
        try {
          const method = (config.method || 'get').toLowerCase()
          const url = (config.url || '').split('?')[0]
          const body = config.data ? JSON.parse(config.data) : {}
          const params = config.params || {}
          const ctx = {
            method,
            url,
            body,
            params,
            headers: config.headers || {},
            db
          }
          const handler = matchRoute(method, url)
          let result
          if (!handler) {
            result = { success: false, message: `接口不存在：${method.toUpperCase()} ${url}`, data: null, allowedActions: [] }
          } else {
            result = handler(ctx)
          }
          resolve({ data: result, status: 200, statusText: 'OK', headers: {}, config })
        } catch (e) {
          reject(e)
        }
      }, 180)
    })
  }
}
