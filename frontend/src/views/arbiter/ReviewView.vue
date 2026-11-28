<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { taskApi, annotationApi, adjudicationApi } from '@/api'
import ArgumentDiagram from '@/components/ArgumentDiagram.vue'

const route = useRoute()
const router = useRouter()
const taskId = computed(() => route.query.taskId || '')
const documentId = computed(() => route.query.documentId || '')

const pickerTasks = ref([])
const loading = ref(false)
const relations = ref([])
const diagram = ref([])
const errorVisible = ref(false)
const errors = ref('')

async function load() {
  if (!taskId.value || !documentId.value) {
    loading.value = true
    try { pickerTasks.value = await taskApi.myTasks() } catch (e) { /* ignore */ } finally { loading.value = false }
    return
  }
  loading.value = true
  try {
    const ann = await annotationApi.load(taskId.value, documentId.value)
    relations.value = ann.relations || []
    diagram.value = ann.diagram || []
  } catch (e) { /* ignore */ } finally { loading.value = false }
}

async function onPass() {
  await adjudicationApi.diagramReview({ taskId: taskId.value, documentId: documentId.value, diagramId: 'dg1', passed: true, errors: [] })
  ElMessage.success('已通过审查')
}

function openReturn() { errorVisible.value = true }
async function onReturn() {
  await adjudicationApi.diagramReview({ taskId: taskId.value, documentId: documentId.value, diagramId: 'dg1', passed: false, errors: [{ position: '', type: '', note: errors.value }] })
  errorVisible.value = false
  ElMessage.success('已退回修改')
}

onMounted(load)
watch([taskId, documentId], load)
</script>

<template>
  <el-card v-if="!taskId" shadow="never" v-loading="loading">
    <template #header><span class="title">选择任务审查图示</span></template>
    <el-table :data="pickerTasks" empty-text="暂无任务">
      <el-table-column prop="name" label="任务名称" min-width="220" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="router.push({ path: '/arbiter/review', query: { taskId: row.id, documentId: row.documentId } })">审查</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <div v-else v-loading="loading">
    <el-card shadow="never" class="mb">
      <template #header>
        <div class="head">
          <span class="title">图示审查</span>
          <span>
            <el-button type="danger" @click="openReturn">退回修改</el-button>
            <el-button type="success" @click="onPass">通过</el-button>
          </span>
        </div>
      </template>
      <p class="hint">对照关系区检查命题节点、关系节点、箭头方向、符号与嵌套层级是否一致。</p>
    </el-card>

    <el-card shadow="never" class="mb">
      <template #header><span class="title">论证图示</span></template>
      <ArgumentDiagram :diagrams="diagram" />
    </el-card>

    <el-card shadow="never">
      <template #header><span class="title">关系区（{{ relations.length }} 条）</span></template>
      <div v-for="r in relations" :key="r.id" class="rel"><span class="rid">{{ r.id }}</span><code>{{ r.formal }}</code></div>
      <el-empty v-if="relations.length === 0" description="暂无关系" />
    </el-card>

    <el-dialog v-model="errorVisible" title="退回修改" width="460px">
      <el-input v-model="errors" type="textarea" :rows="4" placeholder="填写错误位置与说明" />
      <template #footer>
        <el-button @click="errorVisible = false">取消</el-button>
        <el-button type="danger" @click="onReturn">退回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.title { font-weight: 600; }
.mb { margin-bottom: 14px; }
.head { display: flex; justify-content: space-between; align-items: center; }
.hint { color: #909399; font-size: 12px; }
.rel { padding: 4px 0; border-bottom: 1px dashed #f0f0f0; }
.rid { font-weight: 700; color: #e6a23c; margin-right: 8px; }
</style>
