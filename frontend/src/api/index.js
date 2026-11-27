import request from './request'

// ==================== 登录与身份 ====================
export const authApi = {
  login: (account, password) => request.post('/auth/login', { account, password }),
  select: (system, identity) => request.post('/auth/select', { system, identity }),
  logout: () => request.post('/auth/logout'),
  apply: (form) => request.post('/auth/apply', form),
  options: () => request.get('/auth/options')
}

// ==================== 管理后台 ====================
export const adminApi = {
  applications: (params) => request.get('/admin/applications', { params }),
  review: (payload) => request.post('/admin/applications/review', payload),
  accounts: (params) => request.get('/admin/accounts', { params }),
  updateRoles: (payload) => request.post('/admin/accounts/roles', payload),
  importGuide: (payload) => request.post('/admin/guide/import', payload),
  guide: () => request.get('/admin/guide'),
  overview: (params) => request.get('/admin/overview', { params })
}

// ==================== 文书 ====================
export const documentApi = {
  upload: (payload) => request.post('/document/upload', payload),
  detail: (documentId) => request.get(`/document/${documentId}`),
  saveCaseInfo: (documentId, payload) => request.post(`/document/${documentId}/case-info`, payload)
}

// ==================== 任务 ====================
export const taskApi = {
  create: (payload) => request.post('/task/create', payload),
  publish: (taskId) => request.post(`/task/${taskId}/publish`),
  update: (taskId, payload) => request.post(`/task/${taskId}/update`, payload),
  withdraw: (taskId, payload) => request.post(`/task/${taskId}/withdraw`, payload),
  myTasks: () => request.get('/task/my'),
  detail: (taskId) => request.get(`/task/${taskId}`),
  progress: (taskId) => request.get(`/task/${taskId}/progress`)
}

// ==================== 标注 ====================
export const annotationApi = {
  guide: (taskId) => request.get('/annotation/guide', { params: { taskId } }),
  confirmGuide: (payload) => request.post('/annotation/confirm-guide', payload),
  load: (taskId, documentId) => request.get('/annotation/load', { params: { taskId, documentId } }),
  saveDraft: (payload) => request.post('/annotation/draft', payload),
  submit: (payload) => request.post('/annotation/submit', payload)
}

// ==================== 裁定 ====================
export const adjudicationApi = {
  compare: (payload) => request.post('/adjudication/compare', payload),
  conflicts: (params) => request.get('/adjudication/conflicts', { params }),
  decide: (payload) => request.post('/adjudication/decide', payload),
  expertQuestion: (payload) => request.post('/adjudication/expert-question', payload),
  expertQuestions: (params) => request.get('/adjudication/expert-questions', { params }),
  expertReply: (payload) => request.post('/adjudication/expert-reply', payload),
  diagramReview: (payload) => request.post('/adjudication/diagram-review', payload),
  final: (params) => request.get('/adjudication/final', { params }),
  finalConfirm: (payload) => request.post('/adjudication/final-confirm', payload)
}

// ==================== 导出 ====================
export const exportApi = {
  tasks: () => request.get('/export/tasks'),
  create: (payload) => request.post('/export/create', payload),
  import: (payload) => request.post('/export/import', payload)
}

// ==================== 教师 ====================
export const teacherApi = {
  points: (params) => request.get('/teacher/points', { params }),
  savePoint: (payload) => request.post('/teacher/points', payload),
  deletePoint: (payload) => request.post('/teacher/points/delete', payload),
  materials: (params) => request.get('/teacher/materials', { params }),
  saveMaterial: (payload) => request.post('/teacher/materials', payload),
  materialStatus: (payload) => request.post('/teacher/materials/status', payload),
  questions: (params) => request.get('/teacher/questions', { params }),
  saveQuestion: (payload) => request.post('/teacher/questions', payload),
  exercises: (params) => request.get('/teacher/exercises', { params }),
  saveExercise: (payload) => request.post('/teacher/exercises', payload),
  classLearning: (params) => request.get('/teacher/class-learning', { params }),
  saveThreshold: (payload) => request.post('/teacher/threshold', payload),
  targeted: (params) => request.get('/teacher/targeted', { params }),
  feedback: (payload) => request.post('/teacher/feedback', payload)
}

// ==================== 学生 ====================
export const studentApi = {
  materials: () => request.get('/student/materials'),
  exercises: () => request.get('/student/exercises'),
  exerciseDetail: (exerciseId) => request.get(`/student/exercise/${exerciseId}`),
  answerDraft: (payload) => request.post('/student/answer/draft', payload),
  answerSubmit: (payload) => request.post('/student/answer/submit', payload),
  answers: (params) => request.get('/student/answers', { params }),
  answerDetail: (answerId) => request.get(`/student/answer/${answerId}`),
  learning: (params) => request.get('/student/learning', { params })
}

// ==================== 教学管理 ====================
export const teachingAdminApi = {
  import: (payload) => request.post('/teaching-admin/import', payload),
  importResult: (importId) => request.get(`/teaching-admin/import/${importId}/result`),
  classes: (params) => request.get('/teaching-admin/classes', { params }),
  accounts: (params) => request.get('/teaching-admin/accounts', { params }),
  applications: (params) => request.get('/teaching-admin/applications', { params }),
  review: (payload) => request.post('/teaching-admin/applications/review', payload)
}
