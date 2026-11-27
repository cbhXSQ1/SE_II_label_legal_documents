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
      { path: 'admin/apply', name: 'admin-apply', component: ph('账号申请') },
      { path: 'admin/accounts', name: 'admin-accounts', component: ph('账号列表') },
      { path: 'admin/guide', name: 'admin-guide', component: ph('标注指南') },
      { path: 'admin/overview', name: 'admin-overview', component: ph('总体情况') },

      // —— 任务管理 ——
      { path: 'task/documents', name: 'task-documents', component: () => import('@/views/task/DocumentListView.vue'), meta: { title: '文书列表' } },
      { path: 'task/list', name: 'task-list', component: () => import('@/views/task/TaskListView.vue'), meta: { title: '任务列表' } },
      { path: 'task/progress', name: 'task-progress', component: () => import('@/views/task/TaskProgressView.vue'), meta: { title: '任务进度' } },
      { path: 'export', name: 'export', component: () => import('@/views/export/ExportView.vue'), meta: { title: '导出' } },

      // —— 查看与裁定 ——
      { path: 'arbiter/compare', name: 'arbiter-compare', component: ph('查看并比对多人标注结果') },
      { path: 'arbiter/decide', name: 'arbiter-decide', component: ph('裁定冲突') },
      { path: 'arbiter/review', name: 'arbiter-review', component: ph('图示审查') },
      { path: 'arbiter/final', name: 'arbiter-final', component: ph('确认最终版本') },
      { path: 'expert/questions', name: 'expert-questions', component: ph('待回复问题') },

      // —— 教师 ——
      { path: 'teacher/documents', name: 'teacher-documents', component: ph('上传文书') },
      { path: 'teacher/points', name: 'teacher-points', component: ph('知识点') },
      { path: 'teacher/materials', name: 'teacher-materials', component: ph('学习材料') },
      { path: 'teacher/exercises', name: 'teacher-exercises', component: ph('练习任务') },
      { path: 'teacher/class-learning', name: 'teacher-class-learning', component: ph('班级学习情况') },
      { path: 'teacher/targeted', name: 'teacher-targeted', component: ph('针对性练习') },
      { path: 'teacher/feedback', name: 'teacher-feedback', component: ph('教师反馈') },

      // —— 学生 ——
      { path: 'student/materials', name: 'student-materials', component: ph('学习材料') },
      { path: 'student/exercises', name: 'student-exercises', component: ph('练习任务') },
      { path: 'student/answer', name: 'student-answer', component: ph('作答') },
      { path: 'student/records', name: 'student-records', component: ph('作答记录') },
      { path: 'student/learning', name: 'student-learning', component: ph('学习情况') },

      // —— 教学系统管理员 ——
      { path: 'teaching-admin/apply', name: 'teaching-admin-apply', component: ph('账号申请') },
      { path: 'teaching-admin/import', name: 'teaching-admin-import', component: ph('导入班级和学生名单') },
      { path: 'teaching-admin/classes', name: 'teaching-admin-classes', component: ph('班级管理') },
      { path: 'teaching-admin/accounts', name: 'teaching-admin-accounts', component: ph('账号管理') }
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
