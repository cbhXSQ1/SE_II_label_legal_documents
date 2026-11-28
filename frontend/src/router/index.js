import { createRouter, createWebHistory } from 'vue-router'
import { getCredential } from '@/utils/session'
import { useAuthStore } from '@/store/auth'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/login/LoginView.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/select',
    name: 'select',
    component: () => import('@/views/select/SelectView.vue'),
    meta: { title: '选择系统与身份' }
  },
  {
    path: '/',
    component: () => import('@/layouts/AppLayout.vue'),
    children: [
      { path: '', name: 'home', component: () => import('@/views/home/HomeView.vue'), meta: { title: '首页' } },
      { path: 'annotate', name: 'annotate', component: () => import('@/views/annotate/AnnotateView.vue'), meta: { title: '标注工作台' } },

      // —— 管理后台 ——
      { path: 'admin/apply', name: 'admin-apply', component: () => import('@/views/admin/ApplyView.vue'), meta: { title: '账号申请' } },
      { path: 'admin/accounts', name: 'admin-accounts', component: () => import('@/views/admin/AccountsView.vue'), meta: { title: '账号列表' } },
      { path: 'admin/guide', name: 'admin-guide', component: () => import('@/views/admin/GuideView.vue'), meta: { title: '标注指南' } },
      { path: 'admin/overview', name: 'admin-overview', component: () => import('@/views/admin/OverviewView.vue'), meta: { title: '总体情况' } },

      // —— 任务管理 ——
      { path: 'task/documents', name: 'task-documents', component: () => import('@/views/task/DocumentListView.vue'), meta: { title: '文书列表' } },
      { path: 'task/list', name: 'task-list', component: () => import('@/views/task/TaskListView.vue'), meta: { title: '任务列表' } },
      { path: 'task/progress', name: 'task-progress', component: () => import('@/views/task/TaskProgressView.vue'), meta: { title: '任务进度' } },
      { path: 'export', name: 'export', component: () => import('@/views/export/ExportView.vue'), meta: { title: '导出' } },

      // —— 查看与裁定 ——
      { path: 'arbiter/compare', name: 'arbiter-compare', component: () => import('@/views/arbiter/CompareView.vue'), meta: { title: '查看并比对多人标注结果' } },
      { path: 'arbiter/decide', name: 'arbiter-decide', component: () => import('@/views/arbiter/DecideView.vue'), meta: { title: '裁定冲突' } },
      { path: 'arbiter/review', name: 'arbiter-review', component: () => import('@/views/arbiter/ReviewView.vue'), meta: { title: '图示审查' } },
      { path: 'arbiter/final', name: 'arbiter-final', component: () => import('@/views/arbiter/FinalView.vue'), meta: { title: '确认最终版本' } },
      { path: 'expert/questions', name: 'expert-questions', component: () => import('@/views/arbiter/ExpertQuestionsView.vue'), meta: { title: '待回复问题' } },

      // —— 教师 ——
      { path: 'teacher/documents', name: 'teacher-documents', component: () => import('@/views/teacher/TeacherDocumentsView.vue'), meta: { title: '上传文书' } },
      { path: 'teacher/points', name: 'teacher-points', component: () => import('@/views/teacher/PointsView.vue'), meta: { title: '知识点' } },
      { path: 'teacher/materials', name: 'teacher-materials', component: () => import('@/views/teacher/MaterialsView.vue'), meta: { title: '学习材料' } },
      { path: 'teacher/exercises', name: 'teacher-exercises', component: () => import('@/views/teacher/ExercisesView.vue'), meta: { title: '练习任务' } },
      { path: 'teacher/class-learning', name: 'teacher-class-learning', component: () => import('@/views/teacher/ClassLearningView.vue'), meta: { title: '班级学习情况' } },
      { path: 'teacher/targeted', name: 'teacher-targeted', component: () => import('@/views/teacher/TargetedView.vue'), meta: { title: '针对性练习' } },
      { path: 'teacher/feedback', name: 'teacher-feedback', component: () => import('@/views/teacher/FeedbackView.vue'), meta: { title: '教师反馈' } },

      // —— 学生 ——
      { path: 'student/materials', name: 'student-materials', component: () => import('@/views/student/StudentMaterialsView.vue'), meta: { title: '学习材料' } },
      { path: 'student/exercises', name: 'student-exercises', component: () => import('@/views/student/StudentExercisesView.vue'), meta: { title: '练习任务' } },
      { path: 'student/answer', name: 'student-answer', component: () => import('@/views/student/AnswerView.vue'), meta: { title: '作答' } },
      { path: 'student/records', name: 'student-records', component: () => import('@/views/student/RecordsView.vue'), meta: { title: '作答记录' } },
      { path: 'student/learning', name: 'student-learning', component: () => import('@/views/student/LearningView.vue'), meta: { title: '学习情况' } },

      // —— 教学系统管理员 ——
      { path: 'teaching-admin/apply', name: 'teaching-admin-apply', component: () => import('@/views/teachingAdmin/TeachingApplyView.vue'), meta: { title: '账号申请' } },
      { path: 'teaching-admin/import', name: 'teaching-admin-import', component: () => import('@/views/teachingAdmin/ImportView.vue'), meta: { title: '导入班级和学生名单' } },
      { path: 'teaching-admin/classes', name: 'teaching-admin-classes', component: () => import('@/views/teachingAdmin/ClassesView.vue'), meta: { title: '班级管理' } },
      { path: 'teaching-admin/accounts', name: 'teaching-admin-accounts', component: () => import('@/views/teachingAdmin/TeachingAccountsView.vue'), meta: { title: '账号管理' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

function ph(title) {
  return () => import('@/views/common/PagePlaceholder.vue')
}

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const cred = getCredential()
  if (!to.meta.public && !cred) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  const auth = useAuthStore()
  // 已登录但未选身份，只能去选择页
  if (!to.meta.public && to.name !== 'select' && !auth.identity) {
    return { name: 'select' }
  }
  return true
})

router.afterEach((to) => {
  document.title = (to.meta.title ? to.meta.title + ' · ' : '') + '法律论证标注与教学支持系统'
})

export default router
