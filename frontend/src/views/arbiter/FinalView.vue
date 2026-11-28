<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { taskApi, adjudicationApi } from '@/api'

const route = useRoute()
const router = useRouter()
const taskId = computed(() => route.query.taskId || '')
const documentId = computed(() => route.query.documentId || '')

const pickerTasks = ref([])
const loading = ref(false)
const final = ref(null)

async function load() {
  if (!taskId.value || !documentId.value) {
    loading.value = true
    try { pickerTasks.value = await taskApi.myTasks() } catch (e) { /* ignore */ } finally { loading.value = false }
    return
  }
  loading.value = true
  try { final.value = await adjudicationApi.final({ taskId: taskId.value, documentId: documentId.value }) } catch (e) { /* ignore */ } finally { loading.value = false }
}

async function onConfirm() {
  try { await ElMessageBox.confirm('确认最终版本后保持只读，是否继续？', '确认最终版本', { type: 'warning' }) } catch (e) { return }
  await adjudicationApi.finalConfirm({ taskId: taskId.value, documentId: documentId.value })
  ElMessage.success('已确认最终版本')
  load()
}

onMounted(load)
watch([taskId, documentId], load)
</script>

<template>
  <el-card v-if="!taskId" shadow="never" v-loading="loading">
    <template #header><span class="title">选择任务确认最终版本</span></template>
    <el-table :data="pickerTasks" empty-text="暂无任务">
      <el-table-column prop="name" label="任务名称" min-width="220" />
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="router.push({ path: '/arbiter/final', query: { taskId: row.id, documentId: row.documentId } })">查看最终版本</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-card v-else shadow="never" v-loading="loading">
    <template #header>
      <div class="head">
        <span class="title">确认最终标注版本</span>
        <el-button type="success" @click="onConfirm">确认最终版本</el-button>
      </div>
    </template>
    <template v-if="final">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="任务">{{ final.taskId }}</el-descriptions-item>
        <el-descriptions-item label="文书">{{ final.documentId }}</el-descriptions-item>
        <el-descriptions-item label="指南版本">{{ final.guideVersion }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ final.status === 'CONFIRMED' ? '已确认' : '待确认' }}</el-descriptions-item>
        <el-descriptions-item label="确认人">{{ final.confirmedBy || '—' }}</el-descriptions-item>
        <el-descriptions-item label="确认时间">{{ final.confirmedAt || '—' }}</el-descriptions-item>
      </el-descriptions>
      <p class="hint">确认前请确保：所有冲突已裁定、图示审查已通过、统一切分版本已确认。</p>
    </template>
    <el-empty v-else description="该任务尚无最终版本（请先完成切分/结构化轮裁定与图示审查）" />
  </el-card>
</template>

<style scoped>
.title { font-weight: 600; }
.head { display: flex; justify-content: space-between; align-items: center; }
.hint { color: #909399; font-size: 12px; margin-top: 12px; }
</style>
