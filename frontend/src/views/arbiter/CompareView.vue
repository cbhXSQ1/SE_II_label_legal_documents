<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { taskApi, adjudicationApi } from '@/api'
import { percent } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const taskId = computed(() => route.query.taskId || '')
const documentId = computed(() => route.query.documentId || '')

const pickerTasks = ref([])
const loading = ref(false)
const comparing = ref(false)
const annotatorA = ref('')
const annotatorB = ref('')
const result = ref(null)
const annotators = ref([])

async function load() {
  if (!taskId.value || !documentId.value) {
    loading.value = true
    try { pickerTasks.value = await taskApi.myTasks() } catch (e) { /* ignore */ } finally { loading.value = false }
    return
  }
  // 从任务列表取标注员名单
  const tasks = await taskApi.myTasks().catch(() => [])
  const t = tasks.find((x) => x.id === taskId.value)
  annotators.value = t?.annotators || ['刘标注', '陈标注']
  annotatorA.value = annotators.value[0] || ''
  annotatorB.value = annotators.value[1] || ''
}

async function onCompare() {
  if (!annotatorA.value || !annotatorB.value) return
  comparing.value = true
  try {
    result.value = await adjudicationApi.compare({
      taskId: taskId.value, documentId: documentId.value, round: 'STRUCTURED', annotatorA: annotatorA.value, annotatorB: annotatorB.value
    })
  } catch (e) { /* ignore */ } finally { comparing.value = false }
}

function toDecide() {
  router.push({ path: '/arbiter/decide', query: { taskId: taskId.value, documentId: documentId.value } })
}

onMounted(load)
watch([taskId, documentId], load)
</script>

<template>
  <el-card v-if="!taskId" shadow="never" v-loading="loading">
    <template #header><span class="title">选择任务进行比对</span></template>
    <el-table :data="pickerTasks" empty-text="暂无任务">
      <el-table-column prop="name" label="任务名称" min-width="220" />
      <el-table-column prop="guide" label="指南版本" width="100" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="router.push({ path: '/arbiter/compare', query: { taskId: row.id, documentId: row.documentId } })">比对</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <div v-else>
    <el-card shadow="never" class="mb">
      <template #header><span class="title">查看并比对多人标注结果</span></template>
      <div class="pair">
        <el-select v-model="annotatorA" style="width: 160px;">
          <el-option v-for="a in annotators" :key="a" :label="a" :value="a" />
        </el-select>
        <span class="vs">对</span>
        <el-select v-model="annotatorB" style="width: 160px;">
          <el-option v-for="a in annotators" :key="a" :label="a" :value="a" />
        </el-select>
        <el-button type="primary" :loading="comparing" @click="onCompare">比对</el-button>
        <el-button type="success" :disabled="!result" @click="toDecide">进入裁定冲突</el-button>
      </div>
      <div v-if="result" class="summary">
        <span>一致度 <b>{{ percent(result.agreement) }}</b></span>
        <span class="sep">差异度 <b class="red">{{ percent(result.difference) }}</b></span>
        <span class="sep">差异项 <b>{{ result.items.length }}</b> 条</span>
      </div>
    </el-card>

    <el-card v-if="result" shadow="never">
      <template #header><span class="title">差异明细（{{ result.versionA }} vs {{ result.versionB }}）</span></template>
      <el-table :data="result.items">
        <el-table-column prop="type" label="差异类型" width="110" />
        <el-table-column prop="object" label="差异对象" width="90" />
        <el-table-column prop="valueA" label="甲的值" width="160" />
        <el-table-column prop="valueB" label="乙的值" width="160" />
        <el-table-column prop="note" label="判定说明" min-width="220" />
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.title { font-weight: 600; }
.mb { margin-bottom: 14px; }
.pair { display: flex; align-items: center; gap: 10px; }
.vs { color: #909399; }
.summary { margin-top: 12px; color: #606266; }
.summary b { font-size: 18px; }
.summary .sep { margin-left: 20px; }
.red { color: #f56c6c; }
</style>
