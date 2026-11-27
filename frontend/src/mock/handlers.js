import { saveSession, clearSession } from '@/utils/session'
import { IDENTITY_SYSTEM } from '@/utils/dict'
import { describeIdentity, accountOptions, makeSession, requireSession } from './db'

const ok = (data, allowedActions = []) => ({ success: true, message: '成功', data, allowedActions })
const fail = (message) => ({ success: false, message, data: null, allowedActions: [] })

function now() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

// —— 登录与身份 ——
function login(ctx) {
  const { account, password } = ctx.body || {}
  const acc = ctx.db.accounts.find((a) => a.account === account)
  if (!acc || acc.password !== password) return fail('账号或密码错误')
  if (acc.status === 'DISABLED') return fail('账号已停用，请联系管理员')
  const options = accountOptions(acc)
  if (options.length === 0) return fail('没有可进入的系统，请联系管理员')

  const session = makeSession(acc, null, null)
  if (options.length === 1) {
    const opt = options[0]
    session.system = opt.system
    session.identity = opt.identity
    session.systemName = opt.systemName
    session.identityName = opt.identityName
  }
  ctx.db.session = session
  saveSession(session)
  return ok({
    single: options.length === 1,
    identity: options.length === 1 ? options[0] : null,
    options,
    ...session
  })
}

function select(ctx) {
  const r = requireSession(ctx)
  if (r.fail) return fail(r.message)
  const { system, identity } = ctx.body || {}
  const acc = r.account
  if (!acc.roles.includes(identity) || IDENTITY_SYSTEM[identity] !== system) {
    return fail('当前账号没有这个身份')
  }
  const opt = describeIdentity(identity)
  const session = { ...r.session, system, identity, systemName: opt.systemName, identityName: opt.identityName }
  ctx.db.session = session
  saveSession(session)
  return ok(session)
}

function logout(ctx) {
  ctx.db.session = null
  clearSession()
  return ok(null)
}

function apply(ctx) {
  return ok({ applicationId: 'a' + Date.now() })
}

function options(ctx) {
  const r = requireSession(ctx)
  if (r.fail) return fail(r.message)
  const acc = r.account
  return ok({
    name: acc.name,
    options: accountOptions(acc),
    currentSystem: r.session.system,
    currentIdentity: r.session.identity
  })
}

// —— 任务 ——
function myTasks(ctx) {
  const r = requireSession(ctx)
  if (r.fail) return fail(r.message)
  const s = r.session
  const tasks = ctx.db.tasks.filter((t) => {
    if (s.identity === 'TASK_MANAGER') return t.managerId === s.accountId
    if (s.identity === 'ANNOTATOR') return t.annotatorIds.includes(s.accountId)
    if (s.identity === 'ARBITER') return t.arbiterId === s.accountId
    return false
  })
  return ok(tasks.map((t) => ({
    id: t.id,
    name: t.name,
    guide: t.guide,
    due: t.due,
    status: t.status,
    teaching: t.teaching,
    annotators: t.annotators,
    arbiter: t.arbiter,
    documentId: t.documentId
  })))
}

// —— 文书 ——
function documentDetail(ctx, path) {
  const r = requireSession(ctx)
  if (r.fail) return fail(r.message)
  const doc = ctx.db.documents.find((d) => d.id === path.id)
  if (!doc) return fail('文书不存在')
  const reasonText = path.id === 'D1' ? ctx.db.D1.reasonText : ctx.db.docSegmentsOf(path.id).map((p) => p[0]).join('')
  return ok({ ...doc, reasonText, caseInfo: path.id === 'D1' ? ctx.db.D1.caseInfo : null })
}

// —— 标注 ——
function loadAnnotation(ctx) {
  const r = requireSession(ctx)
  if (r.fail) return fail(r.message)
  const { taskId, documentId } = ctx.params || {}
  const key = taskId + ':' + documentId
  const saved = ctx.db.annotations[key]
  let data
  if (saved) data = saved
  else if (documentId === 'D1') {
    data = { pieces: ctx.db.D1.pieces, segments: ctx.db.D1.segments, relations: ctx.db.D1.relations, diagram: ctx.db.D1.diagram }
  } else {
    data = ctx.db.buildAnnotation(ctx.db.docSegmentsOf(documentId))
  }
  return ok(JSON.parse(JSON.stringify(data)))
}

function saveDraft(ctx) {
  const r = requireSession(ctx)
  if (r.fail) return fail(r.message)
  const { taskId, documentId, pieces, segments, relations, diagram } = ctx.body || {}
  ctx.db.annotations[taskId + ':' + documentId] = { pieces, segments, relations, diagram }
  return ok({ savedAt: now() })
}

function submit(ctx) {
  const r = requireSession(ctx)
  if (r.fail) return fail(r.message)
  const { taskId, documentId, pieces, segments, relations, diagram } = ctx.body || {}
  ctx.db.annotations[taskId + ':' + documentId] = { pieces, segments, relations, diagram, submitted: true }
  return ok({ submittedAt: now() }, ['查看最终版本'])
}

// —— 裁定 ——
function compare(ctx) {
  const r = requireSession(ctx)
  if (r.fail) return fail(r.message)
  const diff = ctx.db.adjudication
  return ok({
    agreement: 97,
    difference: 3,
    versionA: diff.versionA,
    versionB: diff.versionB,
    items: diff.diffs
  }, ['进入裁定冲突', '切换标注员对'])
}

function decide(ctx) {
  const r = requireSession(ctx)
  if (r.fail) return fail(r.message)
  return ok(null, ['继续裁定', '查看未裁定项'])
}

// —— 任务管理 / 文书 / 导出 ——
const nameOf = (db, id) => {
  const a = db.accounts.find((x) => x.id === id)
  return a ? a.name : id
}

function documentList(ctx) {
  const r = requireSession(ctx)
  if (r.fail) return fail(r.message)
  const docs = ctx.db.documents.map((d) => ({ ...d }))
  return ok({ records: docs, total: docs.length, page: 1, size: 20 })
}

function userList(ctx) {
  const r = requireSession(ctx)
  if (r.fail) return fail(r.message)
  const identity = ctx.params.identity
  const users = []
  for (const a of ctx.db.accounts) {
    if (identity && !a.roles.includes(identity)) continue
    for (const role of a.roles) {
      if (identity && role !== identity) continue
      users.push({ id: a.id, name: a.name, identity: role })
    }
  }
  return ok({ users })
}

function guideList(ctx) {
  const r = requireSession(ctx)
  if (r.fail) return fail(r.message)
  const versions = [{ version: ctx.db.guide.version, importedAt: ctx.db.guide.importedAt, operator: ctx.db.guide.operator, scope: ctx.db.guide.scope }]
  return ok({ versions })
}

function uploadDocument(ctx) {
  const r = requireSession(ctx)
  if (r.fail) return fail(r.message)
  const { title, type, caseNo } = ctx.body || {}
  const id = 'D' + (ctx.db.documents.length + 1)
  const doc = { id, taskId: '', title: title || '未命名文书', caseNo: caseNo || '', type: type || '原始判决书' }
  ctx.db.documents.push(doc)
  return ok({ documentId: id, title: doc.title, caseNo: doc.caseNo, type: doc.type, reasonText: '', caseInfo: null, duplicated: false })
}

function createTask(ctx) {
  const r = requireSession(ctx)
  if (r.fail) return fail(r.message)
  const { name, description, deadline, guideVersion, documentId, annotatorIds, arbiterId, draft } = ctx.body || {}
  if (!annotatorIds || annotatorIds.length < 2) return fail('至少需要两名标注员')
  if (!arbiterId) return fail('请选择一名仲裁员')
  const id = 'T' + (ctx.db.tasks.length + 1)
  ctx.db.tasks.push({
    id, name: name || '未命名任务', guide: guideVersion || 'v1.2', documentId,
    annotators: annotatorIds.map((x) => nameOf(ctx.db, x)),
    annotatorIds, arbiter: nameOf(ctx.db, arbiterId), arbiterId,
    managerId: r.session.accountId, due: deadline || '',
    status: draft ? 'DRAFT' : 'PUBLISHED', teaching: false
  })
  return ok({ taskId: id })
}

function publishTask(ctx, path) {
  const r = requireSession(ctx)
  if (r.fail) return fail(r.message)
  const t = ctx.db.tasks.find((x) => x.id === path.id)
  if (!t) return fail('任务不存在')
  t.status = 'PUBLISHED'
  return ok(null)
}

function withdrawTask(ctx, path) {
  const r = requireSession(ctx)
  if (r.fail) return fail(r.message)
  const t = ctx.db.tasks.find((x) => x.id === path.id)
  if (!t) return fail('任务不存在')
  t.status = 'WITHDRAWN'
  return ok(null)
}

function taskProgress(ctx, path) {
  const r = requireSession(ctx)
  if (r.fail) return fail(r.message)
  const t = ctx.db.tasks.find((x) => x.id === path.id)
  if (!t) return fail('任务不存在')
  const total = (t.annotatorIds || []).length
  const submitted = t.status === 'CONFIRMED' || t.status === 'STRUCTURED_PENDING' ? total : 0
  return ok({
    due: total,
    submitted,
    unsubmitted: total - submitted,
    conflictCount: t.status === 'STRUCTURED_PENDING' ? 3 : 0,
    confirmedCount: t.status === 'CONFIRMED' ? 1 : 0,
    annotators: (t.annotators || []).map((n, i) => ({
      id: (t.annotatorIds || [])[i], name: n,
      submittedAt: submitted ? '2026-09-28 14:20' : '',
      status: submitted ? '已提交' : '未提交'
    }))
  })
}

function exportTasks(ctx) {
  const r = requireSession(ctx)
  if (r.fail) return fail(r.message)
  const tasks = ctx.db.tasks.filter((t) => t.status === 'CONFIRMED' && t.managerId === r.session.accountId)
  return ok({ tasks: tasks.map((t) => ({ id: t.id, name: t.name, confirmedAt: '2026-09-15 10:00', annotatorCount: (t.annotators || []).length, guideVersion: t.guide })) })
}

function exportCreate(ctx) {
  const r = requireSession(ctx)
  if (r.fail) return fail(r.message)
  const { fileName } = ctx.body || {}
  return ok({ exportId: 'e' + Date.now(), downloadUrl: '/mock/' + (fileName || 'export') + '.zip' })
}

// —— 路由表 ——
const routes = [
  ['post', '/auth/login', login],
  ['post', '/auth/select', select],
  ['post', '/auth/logout', logout],
  ['post', '/auth/apply', apply],
  ['get', '/auth/options', options],
  ['get', '/user/list', userList],
  ['get', '/guide/list', guideList],
  ['get', '/task/my', myTasks],
  ['get', '/task/:id/progress', taskProgress],
  ['post', '/task/create', createTask],
  ['post', '/task/:id/publish', publishTask],
  ['post', '/task/:id/withdraw', withdrawTask],
  ['get', '/document/list', documentList],
  ['post', '/document/upload', uploadDocument],
  ['get', '/document/:id', documentDetail],
  ['get', '/annotation/load', loadAnnotation],
  ['post', '/annotation/draft', saveDraft],
  ['post', '/annotation/submit', submit],
  ['post', '/adjudication/compare', compare],
  ['post', '/adjudication/decide', decide],
  ['get', '/export/tasks', exportTasks],
  ['post', '/export/create', exportCreate]
]

export function matchRoute(method, url) {
  for (const [m, pattern, handler] of routes) {
    if (m !== method) continue
    const keys = []
    const regex = new RegExp(
      '^' + pattern.replace(/:[^/]+/g, (k) => { keys.push(k.slice(1)); return '([^/]+)' }) + '$'
    )
    const match = url.match(regex)
    if (match) {
      const pathParams = {}
      keys.forEach((k, i) => { pathParams[k] = decodeURIComponent(match[i + 1]) })
      return (ctx) => handler(ctx, pathParams)
    }
  }
  return null
}
