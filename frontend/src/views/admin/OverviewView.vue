<script setup>
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api'

const loading = ref(false)
const data = ref({ taskCount: 0, conflictCount: 0, tasks: [] })

async function load() {
  loading.value = true
  try { data.value = await adminApi.overview() } catch (e) { /* ignore */ } finally { loading.value = false }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <div class="stat-row">
      <div class="stat"><div class="num">{{ data.taskCount }}</div><div class="lbl">任务总数</div></div>
      <div class="stat"><div class="num red">{{ data.conflictCount }}</div><div class="lbl">待裁定冲突总数</div></div>
    </div>

    <el-card shadow="never">
      <template #header><span class="title">任务总体情况</span></template>
      <el-table :data="data.tasks" empty-text="暂无任务">
        <el-table-column prop="name" label="任务名称" min-width="200" />
        <el-table-column prop="publisher" label="发布人" width="90" />
        <el-table-column prop="deadline" label="截止时间" width="160" />
        <el-table-column prop="due" label="应完成" width="80" />
        <el-table-column prop="submitted" label="已提交" width="80" />
        <el-table-column prop="unsubmitted" label="未提交" width="80" />
        <el-table-column prop="conflictCount" label="冲突" width="80" />
        <el-table-column prop="confirmedCount" label="已确认" width="80" />
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.stat-row { display: flex; gap: 24px; margin-bottom: 14px; }
.stat { background: #fff; border-radius: 8px; padding: 20px 28px; text-align: center; }
.num { font-size: 28px; font-weight: 700; }
.num.red { color: #f56c6c; }
.lbl { color: #909399; font-size: 12px; margin-top: 4px; }
.title { font-weight: 600; }
</style>
