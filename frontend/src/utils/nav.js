// 各身份对应的侧边导航
export function navFor(identity) {
  switch (identity) {
    case 'ADMIN':
      return [
        { path: '/admin/apply', title: '账号申请' },
        { path: '/admin/accounts', title: '账号列表' },
        { path: '/admin/guide', title: '标注指南' },
        { path: '/admin/overview', title: '总体情况' }
      ]
    case 'TEACHING_ADMIN':
      return [
        { path: '/teaching-admin/apply', title: '账号申请' },
        { path: '/teaching-admin/import', title: '导入班级名单' },
        { path: '/teaching-admin/classes', title: '班级管理' },
        { path: '/teaching-admin/accounts', title: '账号管理' }
      ]
    case 'TASK_MANAGER':
      return [
        { path: '/', title: '任务总览' },
        { path: '/task/documents', title: '文书列表' },
        { path: '/task/list', title: '任务列表' },
        { path: '/task/progress', title: '任务进度' },
        { path: '/export', title: '导出' }
      ]
    case 'ANNOTATOR':
      return [
        { path: '/', title: '我的任务' },
        { path: '/annotate', title: '标注工作台' }
      ]
    case 'ARBITER':
      return [
        { path: '/', title: '我的任务' },
        { path: '/arbiter/compare', title: '查看并比对' },
        { path: '/arbiter/decide', title: '裁定冲突' },
        { path: '/arbiter/review', title: '图示审查' },
        { path: '/arbiter/final', title: '最终版本' }
      ]
    case 'EXPERT':
      return [{ path: '/expert/questions', title: '待回复问题' }]
    case 'TEACHER':
      return [
        { path: '/teacher/documents', title: '上传文书' },
        { path: '/teacher/points', title: '知识点' },
        { path: '/teacher/materials', title: '学习材料' },
        { path: '/teacher/exercises', title: '练习任务' },
        { path: '/teacher/class-learning', title: '班级学习情况' },
        { path: '/teacher/targeted', title: '针对性练习' },
        { path: '/teacher/feedback', title: '教师反馈' }
      ]
    case 'STUDENT':
      return [
        { path: '/student/materials', title: '学习材料' },
        { path: '/student/exercises', title: '练习任务' },
        { path: '/student/records', title: '作答记录' },
        { path: '/student/learning', title: '学习情况' }
      ]
    default:
      return []
  }
}
