<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { documentApi, annotationApi, taskApi } from '@/api'
import { useAuthStore } from '@/store/auth'
import { NON_PROP_TYPES, PROP_TYPES, GM_SUBTYPES, SM_SUBTYPES, SUBTYPE_NAME, REL_TYPES } from '@/utils/dict'
import ArgumentDiagram from '@/components/ArgumentDiagram.vue'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const taskId = computed(() => route.query.taskId || '')
const documentId = computed(() => route.query.documentId || '')
const needPicker = computed(() => !taskId.value || !documentId.value)

const loading = ref(false)
const doc = ref(null)
const pieces = ref([])
const segments = ref([])
const relations = ref([])
const diagram = ref([])
const origDiagram = ref([])
const activeTab = ref('cut')
const activeCollapse = ref(['origin'])
const submitted = ref(false)

// 选择任务
const pickerTasks = ref([])
const pickerLoading = ref(false)

const PROP_CODES = new Set(['SF', 'GF', 'SM', 'GM', 'SM-C', 'GM-L', 'GM-I', 'GM-U', 'GM-M', 'GM-O'])
const isProp = (t) => PROP_CODES.has(t)

function baseType(t) {
  if (!t) return ''
  if (['SF', 'GF'].includes(t)) return t
  if (t === 'SM' || t === 'SM-C') return 'SM'
  if (t.startsWith('GM')) return 'GM'
  return t
}
function subtypeOptions(t) {
  const b = baseType(t)
  if (b === 'GM') return GM_SUBTYPES
  if (b === 'SM') return SM_SUBTYPES
  return []
}
function typeName(t) {
  if (NON_PROP_TYPES.some((x) => x.code === t)) return NON_PROP_TYPES.find((x) => x.code === t).name
  if (PROP_TYPES.some((x) => x.code === t)) return PROP_TYPES.find((x) => x.code === t).name
  if (SUBTYPE_NAME[t]) return SUBTYPE_NAME[t]
  return t || '未标'
}

async function load() {
  if (needPicker.value) {
    pickerLoading.value = true
    try { pickerTasks.value = await taskApi.myTasks() } catch (e) { /* ignore */ }
    finally { pickerLoading.value = false }
    return
  }
  loading.value = true
  try {
    const [docRes, annRes] = await Promise.all([
      documentApi.detail(documentId.value),
      annotationApi.load(taskId.value, documentId.value)
    ])
    doc.value = docRes
    pieces.value = annRes.pieces || []
    segments.value = annRes.segments || []
    relations.value = annRes.relations || []
    diagram.value = annRes.diagram || []
    origDiagram.value = JSON.parse(JSON.stringify(annRes.diagram || []))
    submitted.value = !!annRes.submitted
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

onMounted(load)
watch([taskId, documentId], load)

function enterTask(task) {
  router.push({ path: '/annotate', query: { taskId: task.id, documentId: task.documentId } })
}

// —— 切分 ——
const selected = ref([])
function toggleSelect(id) {
  const i = selected.value.indexOf(id)
  if (i >= 0) selected.value.splice(i, 1)
  else selected.value.push(id)
}
function renumber() {
  segments.value.forEach((s, i) => { s.id = 'P' + (i + 1) })
}
function splitClauses(text) {
  const out = []
  let cur = ''
  for (const ch of text) {
    cur += ch
    if (['，', '；', '、'].includes(ch)) { out.push(cur); cur = '' }
  }
  if (cur) out.push(cur)
  return out
}
function mergeSegments() {
  if (selected.value.length !== 2) { ElMessage.warning('请选择两个相邻段落进行合并'); return }
  const idx = selected.value.map((id) => segments.value.findIndex((s) => s.id === id)).sort((a, b) => a - b)
  if (idx[1] - idx[0] !== 1) { ElMessage.warning('请选择相邻的两个段落'); return }
  const a = segments.value[idx[0]]
  const b = segments.value[idx[1]]
  a.text = a.text + b.text
  a.type = ''
  segments.value.splice(idx[1], 1)
  renumber()
  selected.value = []
  ElMessage.warning('段落已重排，请检查已建立的关系是否受影响')
}
function splitSegment() {
  if (selected.value.length !== 1) { ElMessage.warning('请选择一个段落进行拆分'); return }
  const seg = segments.value.find((s) => s.id === selected.value[0])
  const parts = splitClauses(seg.text)
  if (parts.length < 2) { ElMessage.warning('该段落没有可拆分的子句'); return }
  const idx = segments.value.findIndex((s) => s.id === seg.id)
  const news = parts.map((t) => ({ id: '', text: t, type: '' }))
  segments.value.splice(idx, 1, ...news)
  renumber()
  selected.value = []
  ElMessage.warning('段落已重排，请检查已建立的关系是否受影响')
}

// —— 要素 ——
function onBaseChange(seg, base) {
  if (!base) { seg.type = ''; return }
  if (base === 'GM') seg.type = 'GM-L'
  else if (base === 'SM') seg.type = 'SM'
  else seg.type = base
}

// —— 关系 ——
const propositions = computed(() => segments.value.filter((s) => isProp(s.type)))
const relForm = reactive({ type: 'S', src: '', dst: '', multi: [] })
function pNum(id) { return parseInt(String(id).replace(/\D/g, ''), 10) || 0 }
function buildFormal() {
  const f = relForm
  if (['S', 'A', 'M'].includes(f.type)) return `${f.type}(${f.src}, ${f.dst})`
  const sorted = [...f.multi].sort((a, b) => pNum(a) - pNum(b))
  return `${f.type}(${sorted.join(', ')})`
}
function addRelation() {
  const f = relForm
  if (['S', 'A', 'M'].includes(f.type) && (!f.src || !f.dst)) { ElMessage.warning('请选择两个命题'); return }
  if (['J', 'I'].includes(f.type) && f.multi.length < 2) { ElMessage.warning('请至少选择两个命题'); return }
  const members = ['J', 'I'].includes(f.type) ? [...f.multi].sort((a, b) => pNum(a) - pNum(b)) : [f.src, f.dst]
  relations.value.push({ id: 'R' + (relations.value.length + 1), type: f.type, members, formal: buildFormal() })
  relForm.src = ''; relForm.dst = ''; relForm.multi = []
}
function deleteRelation(id) {
  relations.value = relations.value.filter((r) => r.id !== id)
  relations.value.forEach((r, i) => { r.id = 'R' + (i + 1) })
  ElMessage.warning('已删除并重新编号，请检查嵌套关系的影响')
}

// —— 图示 ——
function autoLayout(rels, segs) {
  const propSet = new Set()
  const expand = (id) => {
    if (id.startsWith('P')) { propSet.add(id); return [id] }
    const r = rels.find((x) => x.id === id)
    if (r) return r.members.flatMap((m) => expand(m))
    return []
  }
  rels.forEach((r) => r.members.forEach((m) => expand(m)))
  const props = [...propSet].sort((a, b) => pNum(a) - pNum(b))
  const COLS = 3
  const nodes = props.map((p, i) => ({ id: p, x: 130 + (i % COLS) * 230, y: 120 + Math.floor(i / COLS) * 150 }))
  const posMap = Object.fromEntries(nodes.map((n) => [n.id, n]))
  const relNodes = []
  const edges = []
  rels.forEach((r, i) => {
    const members = r.members.map((m) => expand(m)[0]).filter((m) => posMap[m])
    if (members.length === 0) return
    const cx = members.reduce((s, m) => s + posMap[m].x, 0) / members.length
    const cy = members.reduce((s, m) => s + posMap[m].y, 0) / members.length
    const rid = 'rn' + (i + 1)
    const kind = r.type === 'S' ? 'support' : r.type === 'A' ? 'attack' : 'plus'
    relNodes.push({ id: rid, kind, x: Math.round(cx), y: Math.round(cy) - 42 })
    members.forEach((m) => edges.push({ from: m, to: rid }))
    if (['S', 'A', 'M'].includes(r.type) && members.length >= 2) edges.push({ from: rid, to: members[1], arrow: true })
  })
  return [{ id: 'auto', title: '论证图示（自动布局）', nodes, relNodes, edges }]
}
async function regenerate() {
  try {
    await ElMessageBox.confirm('重新生成会覆盖当前手工布局，是否继续？', '提示', { type: 'warning' })
  } catch (e) { return }
  diagram.value = autoLayout(relations.value, segments.value)
  ElMessage.success('已重新生成')
}

// —— 提交 / 保存 ——
async function saveDraft() {
  await annotationApi.saveDraft({ taskId: taskId.value, documentId: documentId.value, round: 'STRUCTURED', pieces: pieces.value, segments: segments.value, relations: relations.value, diagram: diagram.value })
  ElMessage.success('草稿已保存')
}
async function submit() {
  try {
    await ElMessageBox.confirm('提交后保持只读，是否继续提交？', '提交结构化标注', { type: 'warning' })
  } catch (e) { return }
  await annotationApi.submit({ taskId: taskId.value, documentId: documentId.value, round: 'STRUCTURED', requestId: 'req-' + Date.now(), pieces: pieces.value, segments: segments.value, relations: relations.value, diagram: diagram.value })
  submitted.value = true
  ElMessage.success('已提交')
}

// —— 指南 ——
const guideVisible = ref(false)
</script>

<template>
  <!-- 未指定任务时：选择任务 -->
  <div v-if="needPicker" class="page-card" v-loading="pickerLoading">
    <div style="font-weight:600; margin-bottom:12px;">请选择要标注的任务</div>
    <el-table :data="pickerTasks" empty-text="没有分配给你的任务">
      <el-table-column prop="name" label="任务名称" min-width="220" />
      <el-table-column prop="guide" label="指南版本" width="100" />
      <el-table-column prop="due" label="截止时间" width="160" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="enterTask(row)">进入标注</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>

  <!-- 工作台 -->
  <div v-else class="workbench" v-loading="loading">
    <div class="topbar">
      <div class="doc-title">
        <span>{{ doc?.title }}</span>
        <el-tag v-if="doc?.caseNo" size="small" type="info">{{ doc.caseNo }}</el-tag>
      </div>
      <div class="actions">
        <el-button @click="guideVisible = true">指南 v1.2</el-button>
        <el-button @click="saveDraft">保存草稿</el-button>
        <el-button type="primary" :disabled="submitted" @click="submit">{{ submitted ? '已提交（只读）' : '提交结构化标注' }}</el-button>
      </div>
    </div>

    <div class="three-col">
      <!-- 左：原文 + 要素列表 -->
      <div class="col left">
        <el-collapse v-model="activeCollapse">
          <el-collapse-item title="裁判理由原文" name="origin">
            <div class="reason-text scroll">{{ doc?.reasonText }}</div>
          </el-collapse-item>
        </el-collapse>
        <div class="seg-title">要素列表（{{ segments.length }}）</div>
        <div class="seg-list scroll">
          <div v-for="s in segments" :key="s.id" class="seg-row">
            <span class="seg-id">{{ s.id }}</span>
            <el-tag v-if="s.type" size="small" :type="isProp(s.type) ? 'primary' : 'success'">{{ s.type }}</el-tag>
            <span v-else class="untag">未标</span>
            <span class="seg-text">{{ s.text }}</span>
          </div>
        </div>
      </div>

      <!-- 中：标注操作 -->
      <div class="col mid">
        <el-tabs v-model="activeTab">
          <!-- 切分 -->
          <el-tab-pane label="切分" name="cut">
            <div class="tab-tip">已按原文顺序编号，可合并相邻段落或按逗号/分号拆分；点击段落选中。</div>
            <div class="seg-list scroll" style="max-height: 60vh;">
              <div v-for="s in segments" :key="s.id" class="cut-row"
                   :class="{ selected: selected.includes(s.id) }" @click="toggleSelect(s.id)">
                <span class="seg-id">{{ s.id }}</span>
                <span class="seg-text">{{ s.text }}</span>
                <span class="count">{{ s.text.length }} 字</span>
              </div>
            </div>
            <div class="bar">
              <el-button size="small" @click="mergeSegments">合并相邻</el-button>
              <el-button size="small" @click="splitSegment">拆分</el-button>
              <el-button size="small" type="primary" @click="ElMessage.success('切分已提交（演示）')">提交切分</el-button>
            </div>
          </el-tab-pane>

          <!-- 要素 -->
          <el-tab-pane label="要素" name="elements">
            <div class="tab-tip">先标非命题要素或「命题」，标为命题的段落再选择命题类型与子类型。</div>
            <div class="seg-list scroll" style="max-height: 62vh;">
              <div v-for="s in segments" :key="s.id" class="elem-row">
                <span class="seg-id">{{ s.id }}</span>
                <span class="seg-text">{{ s.text }}</span>
                <el-select :model-value="baseType(s.type)" placeholder="类型" size="small" style="width: 120px;" @change="(v) => onBaseChange(s, v)">
                  <el-option-group label="非命题要素">
                    <el-option v-for="t in NON_PROP_TYPES" :key="t.code" :label="t.code + ' ' + t.name" :value="t.code" />
                  </el-option-group>
                  <el-option-group label="命题要素">
                    <el-option v-for="t in PROP_TYPES" :key="t.code" :label="t.code + ' ' + t.name" :value="t.code" />
                  </el-option-group>
                </el-select>
                <el-select v-if="subtypeOptions(s.type).length" :model-value="s.type" size="small" style="width: 160px;"
                           @change="(v) => { s.type = v }">
                  <el-option v-for="st in subtypeOptions(s.type)" :key="st" :label="st + ' ' + (SUBTYPE_NAME[st] || '')" :value="st" />
                </el-select>
              </div>
            </div>
          </el-tab-pane>

          <!-- 关系 -->
          <el-tab-pane label="关系" name="relations">
            <div class="tab-tip">按支持 → 组合 → 匹配，同时标同一与反对的顺序建立关系。</div>
            <div class="rel-form">
              <el-radio-group v-model="relForm.type" size="small">
                <el-radio-button v-for="r in REL_TYPES" :key="r.code" :value="r.code">{{ r.code }}</el-radio-button>
              </el-radio-group>
              <template v-if="['S', 'A', 'M'].includes(relForm.type)">
                <el-select v-model="relForm.src" placeholder="命题一" size="small" style="width: 120px;">
                  <el-option v-for="p in propositions" :key="p.id" :label="p.id" :value="p.id" />
                </el-select>
                <el-select v-model="relForm.dst" placeholder="命题二" size="small" style="width: 120px;">
                  <el-option v-for="p in propositions" :key="p.id" :label="p.id" :value="p.id" />
                </el-select>
              </template>
              <el-select v-else v-model="relForm.multi" multiple collapse-tags placeholder="选择命题（≥2）" size="small" style="width: 260px;">
                <el-option v-for="p in propositions" :key="p.id" :label="p.id" :value="p.id" />
              </el-select>
              <el-button type="primary" size="small" @click="addRelation">添加关系</el-button>
            </div>
            <div v-if="propositions.length === 0" class="hint">暂无命题：请先在「要素」页把段落标为命题类型。</div>
            <div class="rel-list scroll" style="max-height: 48vh;">
              <div v-for="r in relations" :key="r.id" class="rel-row">
                <span class="rid">{{ r.id }}</span>
                <code>{{ r.formal }}</code>
                <el-tag size="small">{{ r.type }}</el-tag>
                <el-button link type="danger" size="small" @click="deleteRelation(r.id)">删除</el-button>
              </div>
            </div>
          </el-tab-pane>

          <!-- 图示 -->
          <el-tab-pane label="图示" name="diagram">
            <div class="tab-tip">按关系区生成论证图示，支持拖动节点与缩放。</div>
            <el-button size="small" type="primary" style="margin-bottom:8px;" @click="regenerate">重新生成</el-button>
            <ArgumentDiagram :diagrams="diagram" />
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 右：论证图示 -->
      <div class="col right">
        <div class="seg-title">论证图示</div>
        <ArgumentDiagram :diagrams="diagram" />
      </div>
    </div>

    <el-dialog v-model="guideVisible" title="标注指南（v1.2）" width="560px">
      <p><b>适用范围：</b>全部任务</p>
      <p><b>非命题要素：</b>争议焦点 IS、态度声明 AS、总结性陈述 SS、判决依据 JG、判决结果 JD、其他 Non-O</p>
      <p><b>命题要素：</b>SF 个别事实、GF 一般事实、SM 个别规范、GM 一般规范；GM 分 GM-L/I/U/M/O，SM 分 SM / SM-C</p>
      <p><b>关系：</b>S 支持、A 反对、J 组合、M 匹配、I 同一</p>
      <el-checkbox>我已阅读并理解</el-checkbox>
      <template #footer>
        <el-button type="primary" @click="guideVisible = false; ElMessage.success('已记录阅读确认')">确认理解</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.workbench { display: flex; flex-direction: column; height: 100%; }
.topbar { display: flex; justify-content: space-between; align-items: center; background: #fff; border-radius: 8px; padding: 10px 16px; margin-bottom: 12px; }
.doc-title { font-weight: 600; display: flex; align-items: center; gap: 8px; }
.actions { display: flex; gap: 8px; }
.three-col { flex: 1; display: grid; grid-template-columns: 300px 1fr 1.1fr; gap: 12px; min-height: 0; }
.col { background: #fff; border-radius: 8px; padding: 12px; overflow: hidden; display: flex; flex-direction: column; }
.seg-title { font-weight: 600; margin: 10px 0 8px; }
.scroll { overflow: auto; }
.reason-text { max-height: 200px; font-size: 13px; color: #606266; }
.seg-list { flex: 1; min-height: 0; }
.seg-row, .cut-row, .elem-row { display: flex; align-items: center; gap: 8px; padding: 4px 6px; border-radius: 4px; }
.seg-row { border-bottom: 1px dashed #f0f0f0; }
.cut-row { cursor: pointer; border-bottom: 1px solid #f0f0f0; }
.cut-row.selected { background: #d9ecff; }
.elem-row { border-bottom: 1px solid #f0f0f0; }
.seg-id { font-weight: 700; color: #409eff; min-width: 32px; }
.untag { color: #c0c4cc; font-size: 12px; }
.seg-text { flex: 1; min-width: 0; word-break: break-all; font-size: 13px; }
.count { color: #c0c4cc; font-size: 12px; white-space: nowrap; }
.tab-tip { color: #909399; font-size: 12px; margin-bottom: 8px; }
.bar { display: flex; gap: 8px; margin-top: 10px; }
.rel-form { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-bottom: 10px; }
.rel-row { display: flex; align-items: center; gap: 8px; padding: 4px 0; border-bottom: 1px dashed #f0f0f0; }
.rid { font-weight: 700; color: #e6a23c; }
.hint { color: #e6a23c; font-size: 12px; padding: 8px 0; }
</style>
