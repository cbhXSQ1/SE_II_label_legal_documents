// 内置 Mock 数据库：种子数据复用原型，结构对齐后端《功能层对下接口》里的实体。
import { getSession, saveSession, clearSession } from '@/utils/session'
import { IDENTITIES, IDENTITY_SYSTEM, SYSTEMS, IDENTITY_DESC } from '@/utils/dict'

const D1_REASON =
  '本院认为，依法成立的合同，自成立时生效。当事人应当按照约定全面履行自己的义务。' +
  '当事人一方未支付价款、报酬、租金、利息，或者不履行其他金钱债务的，对方可以请求其支付。' +
  '本案中，原、被告之间系劳务合同关系，原告已依约提供劳务，被告应按照原、被告之间的约定给付劳务报酬。' +
  '根据原告提交的欠条、微信转账记录可以认定，被告尚欠原告劳务报酬11000元，被告应向原告支付该11000元。' +
  '原告主张另有劳务报酬600元，但其提供的证据不足以证明其主张，对其主张被告支付该600元劳务报酬的请求，本院不予支持。' +
  '综上所述，依照《中华人民共和国民法典》第五百零二条、第五百零九条、第五百七十九条规定，判决如下：'

const D1_PIECES = [
  '依法成立的合同，自成立时生效。',
  '当事人应当按照约定全面履行自己的义务。',
  '当事人一方未支付价款、报酬、租金、利息，或者不履行其他金钱债务的，对方可以请求其支付。',
  '本案中，原、被告之间系劳务合同关系，',
  '原告已依约提供劳务，',
  '被告应按照原、被告之间的约定给付劳务报酬。',
  '根据原告提交的欠条、微信转账记录可以认定，',
  '被告尚欠原告劳务报酬11000元，',
  '被告应向原告支付该11000元。',
  '原告主张另有劳务报酬600元，',
  '但其提供的证据不足以证明其主张，',
  '对其主张被告支付该600元劳务报酬的请求，',
  '本院不予支持。'
]

const D1_SEGMENTS = [
  { id: 'P1', text: '依法成立的合同，自成立时生效。', type: 'GM-L' },
  { id: 'P2', text: '当事人应当按照约定全面履行自己的义务。', type: 'GM-L' },
  { id: 'P3', text: '当事人一方未支付价款、报酬、租金、利息，或者不履行其他金钱债务的，对方可以请求其支付。', type: 'GM-L' },
  { id: 'P4', text: '本案中，原、被告之间系劳务合同关系，', type: 'SM' },
  { id: 'P5', text: '原告已依约提供劳务，', type: 'SF' },
  { id: 'P6', text: '被告应按照原、被告之间的约定给付劳务报酬。', type: 'SM' },
  { id: 'P7', text: '根据原告提交的欠条、微信转账记录可以认定，', type: 'SF' },
  { id: 'P8', text: '被告尚欠原告劳务报酬11000元，', type: 'SF' },
  { id: 'P9', text: '被告应向原告支付该11000元。', type: 'SM' },
  { id: 'P10', text: '原告主张另有劳务报酬600元，', type: 'SF' },
  { id: 'P11', text: '但其提供的证据不足以证明其主张，', type: 'SF' },
  { id: 'P12', text: '对其主张被告支付该600元劳务报酬的请求，', type: 'SM' },
  { id: 'P13', text: '本院不予支持。', type: 'SM' }
]

const D1_RELATIONS = [
  { id: 'R1', type: 'J', members: ['P4', 'P5'], formal: 'J(P4, P5)' },
  { id: 'R2', type: 'M', members: ['R1', 'P2'], formal: 'M(J(P4, P5), P2)' },
  { id: 'R3', type: 'S', members: ['R2', 'P6'], formal: 'S(M(J(P4, P5), P2), P6)' },
  { id: 'R4', type: 'S', members: ['P7', 'P8'], formal: 'S(P7, P8)' },
  { id: 'R5', type: 'J', members: ['P8', 'P6'], formal: 'J(P8, P6)' },
  { id: 'R6', type: 'M', members: ['R5', 'P3'], formal: 'M(J(P8, P6), P3)' },
  { id: 'R7', type: 'S', members: ['R6', 'P9'], formal: 'S(M(J(P8, P6), P3), P9)' },
  { id: 'R8', type: 'S', members: ['P10', 'P12'], formal: 'S(P10, P12)' },
  { id: 'R9', type: 'A', members: ['P11', 'P10'], formal: 'A(P11, P10)' },
  { id: 'R10', type: 'S', members: ['P11', 'P13'], formal: 'S(P11, P13)' }
]

const D1_DIAGRAMS = [
  {
    id: 'dg1', title: '论证一（11000 元请求）',
    nodes: [
      { id: 'P2', x: 150, y: 110 }, { id: 'P4', x: 80, y: 300 }, { id: 'P5', x: 215, y: 300 },
      { id: 'P6', x: 410, y: 110 }, { id: 'P7', x: 80, y: 480 }, { id: 'P8', x: 225, y: 480 },
      { id: 'P3', x: 530, y: 300 }, { id: 'P9', x: 680, y: 300 }
    ],
    relNodes: [
      { id: 'n1', kind: 'plus', x: 145, y: 205 }, { id: 's1', kind: 'support', x: 280, y: 110 },
      { id: 's2', kind: 'support', x: 152, y: 480 }, { id: 'j2', kind: 'plus', x: 340, y: 260 },
      { id: 's3', kind: 'support', x: 605, y: 300 }
    ],
    edges: [
      { from: 'P4', to: 'n1' }, { from: 'P5', to: 'n1' }, { from: 'n1', to: 'P2', arrow: true },
      { from: 'P2', to: 's1' }, { from: 's1', to: 'P6', arrow: true },
      { from: 'P7', to: 's2' }, { from: 's2', to: 'P8', arrow: true },
      { from: 'P8', to: 'j2' }, { from: 'P6', to: 'j2' }, { from: 'j2', to: 'P3', arrow: true },
      { from: 'P3', to: 's3' }, { from: 's3', to: 'P9', arrow: true }
    ]
  },
  {
    id: 'dg2', title: '论证二（600 元请求）',
    nodes: [
      { id: 'P10', x: 120, y: 110 }, { id: 'P12', x: 400, y: 110 },
      { id: 'P11', x: 120, y: 340 }, { id: 'P13', x: 400, y: 340 }
    ],
    relNodes: [
      { id: 's4', kind: 'support', x: 260, y: 110 }, { id: 'a1', kind: 'attack', x: 120, y: 225 },
      { id: 's5', kind: 'support', x: 260, y: 340 }, { id: 'a2', kind: 'attack', x: 260, y: 225 }
    ],
    edges: [
      { from: 'P10', to: 's4' }, { from: 's4', to: 'P12', arrow: true },
      { from: 'P11', to: 'a1' }, { from: 'a1', to: 'P10', arrow: true },
      { from: 'P11', to: 's5' }, { from: 's5', to: 'P13', arrow: true },
      { from: 'P11', to: 'a2' }, { from: 'a2', to: 's4', arrow: true }
    ]
  }
]

// D2-D10 的切分段落（复用原型每份文书的 mock 标注数据）
const DOC_SEGMENTS = {
  D1: D1_SEGMENTS.map((s) => [s.text, s.type]),
  D2: [
    ['本案中，原、被告之间形成劳务合同关系，', 'SM'],
    ['原告已依约完成约定的劳务工作，', 'SF'],
    ['被告应当按约定支付相应劳务报酬。', 'SM'],
    ['被告抗辩原告的工作存在质量问题，', 'SF'],
    ['但未提供证据予以证明，', 'SF'],
    ['本院对该抗辩意见不予采纳。', 'SM']
  ],
  D3: [
    ['依法成立的合同受法律保护。', 'GM-L'],
    ['当事人一方不履行合同义务的，应当承担继续履行等违约责任。', 'GM-L'],
    ['本案中，被告拖欠原告劳务报酬的事实清楚，', 'SF'],
    ['双方结算确认欠付数额为 8600 元，', 'SF'],
    ['故被告应当支付原告劳务报酬 8600 元。', 'SM']
  ],
  D4: [
    ['当事人应当按照约定全面履行自己的义务。', 'GM-L'],
    ['买受人应当按照约定的数额支付价款。', 'GM-L'],
    ['被告收到货物后未按约定期限付款，', 'SF'],
    ['尚欠原告货款 25000 元。', 'SF'],
    ['被告应当支付上述货款并承担逾期付款的违约责任。', 'SM']
  ],
  D5: [
    ['当事人对自己提出的主张有责任提供证据。', 'GM-L'],
    ['原告提交的送货单与对账单可以相互印证，', 'SF'],
    ['能够证明被告尚欠货款 12000 元。', 'SF'],
    ['对原告要求支付该笔货款的请求，本院予以支持。', 'SM']
  ],
  D6: [
    ['出租人应当按照约定将租赁物交付承租人。', 'GM-L'],
    ['承租人应当按照约定的期限支付租金。', 'GM-L'],
    ['被告承租涉案房屋后连续两期未支付租金，', 'SF'],
    ['其行为已构成违约。', 'SM'],
    ['原告有权请求解除租赁合同。', 'SM']
  ],
  D7: [
    ['当事人协商一致，可以解除合同。', 'GM-L'],
    ['被告欠付租金且经原告催告后仍未支付，', 'SF'],
    ['双方租赁合同于催告期满之日解除。', 'SM']
  ],
  D8: [
    ['承租人无正当理由未支付租金的，', 'GM-L'],
    ['出租人可以要求承租人在合理期限内支付。', 'GM-L'],
    ['逾期不支付的，出租人可以解除合同。', 'GM-L'],
    ['被告逾期未付租金已明显超过合理期限。', 'SF']
  ],
  D9: [
    ['租赁期限届满，承租人应当返还租赁物。', 'GM-L'],
    ['原、被告之间的租赁合同已于上月到期，', 'SF'],
    ['被告仍占用涉案房屋拒不腾退。', 'SF'],
    ['被告应当腾退房屋并支付占用期间的使用费。', 'SM']
  ],
  D10: [
    ['依法成立的合同，自成立时生效。', 'GM-L'],
    ['本案中，双方之间存在买卖合同关系。', 'SM'],
    ['被告未按约定支付货款。', 'SF'],
    ['被告应当支付所欠货款。', 'SM']
  ]
}

function buildAnnotation(segPairs) {
  const pieces = segPairs.map((p, i) => ({ id: 's' + (i + 1), text: p[0] }))
  const segments = segPairs.map((p, i) => ({ id: 'P' + (i + 1), text: p[0], type: p[1] || '' }))
  return { pieces, segments, relations: [], diagram: null }
}

const seed = {
  accounts: [
    { id: 'u1', account: 'admin', password: '123456', name: '李管理', unit: '课程组', status: 'ENABLED', roles: ['ADMIN', 'TEACHING_ADMIN'] },
    { id: 'u2', account: 'zhao', password: '123456', name: '赵老师', unit: '法学院', status: 'ENABLED', roles: ['TEACHER'] },
    { id: 'u3', account: 'liu', password: '123456', name: '刘标注', unit: '法学院', status: 'ENABLED', roles: ['ANNOTATOR'] },
    { id: 'u4', account: 'chen', password: '123456', name: '陈标注', unit: '法学院', status: 'ENABLED', roles: ['ANNOTATOR'] },
    { id: 'u5', account: 'zhangsan', password: '123456', name: '张三', unit: '法学院', status: 'ENABLED', roles: ['STUDENT', 'ANNOTATOR'] },
    { id: 'u6', account: 'wang', password: '123456', name: '王仲裁', unit: '课程组', status: 'ENABLED', roles: ['ARBITER'] },
    { id: 'u7', account: 'sun', password: '123456', name: '孙专家', unit: '法学院', status: 'ENABLED', roles: ['EXPERT'] },
    { id: 'u8', account: 'qian', password: '123456', name: '钱任务', unit: '课程组', status: 'ENABLED', roles: ['TASK_MANAGER'] }
  ],
  guide: {
    version: 'v1.2',
    importedAt: '2026-09-20 10:12',
    operator: '李管理',
    scope: '全部任务',
    parsed: {
      nonProps: ['争议焦点（IS）', '态度声明', '总结性陈述', '判决依据', '判决结果', '其他非论证成分'],
      props: ['SF', 'GF', 'SM', 'SM-C', 'GM-L', 'GM-I', 'GM-U', 'GM-M', 'GM-O'],
      relations: ['S 支持关系', 'A 反对关系', 'J 组合关系', 'M 匹配关系', 'I 同一关系']
    },
    history: [
      { version: 'v1.1', date: '2026-04-14', operator: '李管理', note: '初版试用' },
      { version: 'v1.2', date: '2026-09-18', operator: '李管理', note: '六类非论证成分、SM-C 调整、图示规范' }
    ]
  },
  tasks: [
    { id: 'T1', name: '劳务合同纠纷标注（第一期）', guide: 'v1.2', documentId: 'D1', annotators: ['刘标注', '陈标注'], annotatorIds: ['u3', 'u4'], arbiter: '王仲裁', arbiterId: 'u6', managerId: 'u8', due: '2026-10-15', status: 'PUBLISHED', teaching: false },
    { id: 'T2', name: '买卖合同纠纷标注', guide: 'v1.2', documentId: 'D4', annotators: ['刘标注', '陈标注'], annotatorIds: ['u3', 'u4'], arbiter: '王仲裁', arbiterId: 'u6', managerId: 'u8', due: '2026-09-30', status: 'STRUCTURED_PENDING', teaching: false },
    { id: 'T3', name: '租赁合同纠纷标注', guide: 'v1.1', documentId: 'D6', annotators: ['刘标注', '陈标注'], annotatorIds: ['u3', 'u4'], arbiter: '王仲裁', arbiterId: 'u6', managerId: 'u8', due: '2026-09-10', status: 'CONFIRMED', teaching: false },
    { id: 'T4', name: '教学练习：文书一标准样例', guide: 'v1.2', documentId: 'D10', annotators: ['张三'], annotatorIds: ['u5'], arbiter: '赵老师', arbiterId: 'u2', managerId: 'u2', due: '', status: 'PUBLISHED', teaching: true }
  ],
  documents: [
    { id: 'D1', taskId: 'T1', title: '余某、徐某劳务合同纠纷民事一审民事判决书', caseNo: '(2023)苏0106民初18909号', type: '原始判决书' },
    { id: 'D2', taskId: 'T1', title: '冯某与周某劳务合同纠纷民事一审民事判决书', caseNo: '(2023)苏0106民初19012号', type: '原始判决书' },
    { id: 'D3', taskId: 'T1', title: '许某诉某装饰公司劳务合同纠纷民事一审民事判决书', caseNo: '(2023)苏0106民初19107号', type: '原始判决书' },
    { id: 'D4', taskId: 'T2', title: '某商贸公司与某建材公司买卖合同纠纷民事一审民事判决书', caseNo: '(2023)苏0106民初20101号', type: '原始判决书' },
    { id: 'D5', taskId: 'T2', title: '钱某与某五金公司买卖合同纠纷民事一审民事判决书', caseNo: '(2023)苏0106民初20122号', type: '原始判决书' },
    { id: 'D6', taskId: 'T3', title: '郑某与王某房屋租赁合同纠纷民事一审民事判决书', caseNo: '(2023)苏0106民初30001号', type: '原始判决书' },
    { id: 'D7', taskId: 'T3', title: '某地产公司与某餐饮公司房屋租赁合同纠纷民事一审民事判决书', caseNo: '(2023)苏0106民初30015号', type: '原始判决书' },
    { id: 'D8', taskId: 'T3', title: '孙某与某物业公司房屋租赁合同纠纷民事一审民事判决书', caseNo: '(2023)苏0106民初30028号', type: '原始判决书' },
    { id: 'D9', taskId: 'T3', title: '李某与某某公司房屋租赁合同纠纷民事一审民事判决书', caseNo: '(2023)苏0106民初30044号', type: '原始判决书' },
    { id: 'D10', taskId: 'T4', title: '教学练习：文书一标准样例', caseNo: '教学样例', type: '原始判决书' }
  ],
  D1: {
    reasonText: D1_REASON,
    pieces: D1_PIECES.map((t, i) => ({ id: 's' + (i + 1), text: t })),
    segments: D1_SEGMENTS,
    relations: D1_RELATIONS,
    diagram: D1_DIAGRAMS,
    caseInfo: {
      caseNo: '(2023)苏0106民初18909号', court: '江苏省南京市鼓楼区人民法院', level: '基层法院',
      trial: '一审', date: '2023年12月28日', nature: '民事', cause: '劳务合同纠纷', resultType: '部分支持'
    }
  },
  adjudication: {
    versionA: '刘标注',
    versionB: '陈标注',
    diffs: [
      { id: 'd1', kind: '标签选择', target: 'P4', desc: 'P4 的要素类型不一致', a: 'SM', b: 'SF', resolved: false },
      { id: 'd2', kind: '标注边界', target: 'P7-P8', desc: '切割范围不一致：A 合并为一个要素，B 分为两个要素', a: '合并（1 个要素）', b: '拆分（2 个要素）', resolved: false },
      { id: 'd3', kind: '关系指向', target: 'R9', desc: '反对关系方向相反', a: 'A(P11, P10)', b: 'A(P10, P11)', resolved: false }
    ]
  },
  teaching: {
    quiz: [
      { q: '“应当理解为”最可能对应下列哪一标签？', options: ['GM-L 法律条文', 'GM-I 法律解释', 'GM-U 习惯与行业惯例'], answer: 1 },
      { q: '“本院不予采纳”属于哪一类非论证成分？', options: ['态度声明', '总结性陈述', '判决依据'], answer: 0 }
    ],
    studentDiffs: [
      { kind: '标签选择', desc: 'P4：标准为 SM，学生标为 SF', standard: 'SM', student: 'SF' },
      { kind: '标注边界', desc: 'P7–P8：标准为两个要素，学生合并为一个', standard: '拆分（P7、P8）', student: '合并（1 个）' },
      { kind: '关系指向', desc: 'R8：标准 S(P10, P12)，学生为 S(P12, P10)（方向相反）', standard: 'S(P10, P12)', student: 'S(P12, P10)' }
    ],
    comments: [
      { teacher: '赵老师', time: '2026-09-22 21:05', text: '整体结构正确；注意“道义概念（应/应当）是规范判断标志”，P4 属法律关系表述，应为 SM。' }
    ]
  }
}

// 每次请求临时保存的标注修改（内存态，刷新后回到种子）
const annotations = {}

function docSegmentsOf(docId) {
  return DOC_SEGMENTS[docId] || []
}

const db = {
  accounts: seed.accounts,
  guide: seed.guide,
  tasks: seed.tasks,
  documents: seed.documents,
  D1: seed.D1,
  adjudication: seed.adjudication,
  teaching: seed.teaching,
  annotations,
  session: getSession() || null,
  docSegmentsOf,
  buildAnnotation
}

// —— 身份与会话辅助 ——

export function describeIdentity(identity) {
  return {
    system: IDENTITY_SYSTEM[identity],
    systemName: SYSTEMS[IDENTITY_SYSTEM[identity]],
    identity,
    identityName: IDENTITIES[identity],
    description: IDENTITY_DESC[identity]
  }
}

export function accountOptions(account) {
  return account.roles.map((r) => describeIdentity(r))
}

export function makeSession(account, system, identity) {
  return {
    credential: 'mock-' + Date.now() + '-' + Math.random().toString(36).slice(2, 8),
    accountId: account.id,
    name: account.name,
    unit: account.unit,
    system: system || null,
    identity: identity || null,
    systemName: system ? SYSTEMS[system] : '',
    identityName: identity ? IDENTITIES[identity] : ''
  }
}

export function currentAccount(dbState) {
  const s = dbState.session
  if (!s) return null
  return dbState.accounts.find((a) => a.id === s.accountId) || null
}

export function requireSession(ctx) {
  const s = ctx.db.session
  const cred = (ctx.headers && ctx.headers.Authorization) || ''
  if (!s || !cred || cred !== s.credential) {
    return { fail: true, message: '登录已失效，请重新登录' }
  }
  return { fail: false, session: s, account: currentAccount(ctx.db) }
}

let _db = null
export function getDb() {
  if (!_db) _db = db
  return _db
}
