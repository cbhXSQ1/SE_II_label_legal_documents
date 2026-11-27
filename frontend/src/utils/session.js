// 会话：客户端凭证 + 当前身份。前端 auth store 与内置 Mock 共用这一份。
const KEY = 'seii.session'

export function getSession() {
  try {
    const v = localStorage.getItem(KEY)
    return v ? JSON.parse(v) : null
  } catch (e) {
    return null
  }
}

export function saveSession(s) {
  localStorage.setItem(KEY, JSON.stringify(s))
}

export function clearSession() {
  localStorage.removeItem(KEY)
}

export function getCredential() {
  const s = getSession()
  return s ? s.credential || null : null
}
