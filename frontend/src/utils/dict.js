// 领域字典：系统、身份、标签、关系类型 —— 与《指南 v1.2》及《需求规格说明》一致

export const SYSTEMS = {
  ADMIN: '管理后台',
  ANNOTATION: '普通标注系统',
  TEACHING: '教学系统'
}

export const IDENTITIES = {
  ADMIN: '管理员',
  TEACHING_ADMIN: '教学系统管理员',
  TASK_MANAGER: '任务管理者',
  TEACHER: '教师',
  ANNOTATOR: '标注员',
  STUDENT: '学生',
  ARBITER: '仲裁员',
  EXPERT: '领域专家'
}

// 身份所属系统
export const IDENTITY_SYSTEM = {
  ADMIN: 'ADMIN',
  TEACHING_ADMIN: 'TEACHING',
  TASK_MANAGER: 'ANNOTATION',
  ANNOTATOR: 'ANNOTATION',
  ARBITER: 'ANNOTATION',
  EXPERT: 'ANNOTATION',
  TEACHER: 'TEACHING',
  STUDENT: 'TEACHING'
}

export const IDENTITY_DESC = {
  ADMIN: '审批账号、修改角色、导入指南、查看两个系统总体情况',
  TEACHING_ADMIN: '审批教师/学生账号、导入班级名单、查看教学系统账号与班级',
  TASK_MANAGER: '上传文书、创建发布标注任务、查看进度、导出与再导入',
  ANNOTATOR: '阅读指南、切分文本、标注要素与关系、生成论证图示',
  ARBITER: '比对多人标注、裁定冲突、审查图示、确认最终版本',
  EXPERT: '回复仲裁员提交的领域专家问题',
  TEACHER: '知识点、学习材料、练习任务、班级学习情况、教师反馈',
  STUDENT: '学习材料、练习作答、查看差异与反馈、学习情况'
}

// 非命题要素类型（六类）
export const NON_PROP_TYPES = [
  { code: 'IS', name: '争议焦点' },
  { code: 'AS', name: '态度声明' },
  { code: 'SS', name: '总结性陈述' },
  { code: 'JG', name: '判决依据' },
  { code: 'JD', name: '判决结果' },
  { code: 'Non-O', name: '其他非论证成分' }
]
export const PROP_MARK = '命题'

// 命题基本类型 + 子类型
export const PROP_TYPES = [
  { code: 'SF', name: '个别事实判断' },
  { code: 'GF', name: '一般事实判断' },
  { code: 'SM', name: '个别规范判断' },
  { code: 'GM', name: '一般规范判断' }
]
export const GM_SUBTYPES = ['GM-L', 'GM-I', 'GM-U', 'GM-M', 'GM-O']
export const SM_SUBTYPES = ['SM', 'SM-C']
export const SUBTYPE_NAME = {
  'GM-L': '法律条文',
  'GM-I': '法律解释',
  'GM-U': '习惯与行业惯例',
  'GM-M': '道德与价值观念',
  'GM-O': '其他规范判断',
  'SM': '普通个别规范判断',
  'SM-C': '合同及合同解释'
}

// 关系类型
export const REL_TYPES = [
  { code: 'S', name: '支持关系', formal: 'S(pi, pj)' },
  { code: 'A', name: '反对关系', formal: 'A(po, pc)' },
  { code: 'J', name: '组合关系', formal: 'J(p1, p2, …, pn)' },
  { code: 'M', name: '匹配关系', formal: 'M(ps, pg)' },
  { code: 'I', name: '同一关系', formal: 'I(p1, p2, …, pn)' }
]

// 题型
export const QUESTION_TYPES = [
  { code: 'CUT', name: '切割题' },
  { code: 'ELEMENT', name: '要素标注题' },
  { code: 'RELATION', name: '关系标注题' },
  { code: 'COMPOSITE', name: '综合标注题' },
  { code: 'SINGLE', name: '单选题' },
  { code: 'MULTI', name: '多选题' },
  { code: 'JUDGE', name: '判断题' }
]

export function identityName(v) { return IDENTITIES[v] || v }
export function systemName(v) { return SYSTEMS[v] || v }
