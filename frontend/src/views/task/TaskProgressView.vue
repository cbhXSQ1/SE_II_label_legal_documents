<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { taskApi } from '@/api'

const route = useRoute()
const router = useRouter()
const taskId = computed(() => route.query.taskId || '')
const loading = ref(false)
const progress = ref(null)
const tasks = ref([])

async function load() {
  if (!taskId.value) {
    loading.value = true
    try { tasks.value = await taskApi.myTasks() } catch (e) { /* 拦截器已提示 */ } finally { loading.value = false }
    return
  }
  loading.value = true
  try { progress.value = await taskApi.progress(taskId.value) } catch (e) { /* 拦截器已提示 */ } finally { loading.value = false }
}

function pick(t) {
  router.push({ path: '/task/progress', query: { taskId: t.id } })
}

onMounted(load)
watch(taskId, load)
</script>

<template>
  <div>
    <!-- 未选任务：先选 -->
    <el-card v-if="!taskId" shadow="never" v-loading="loading">
      <template #header><span class="title">选择任务查看进度</span></template>
      <el-table :data="tasks" empty-text="暂无任务">
        <el-table-column prop="name" label="任务名称" min-width="220" />
        <el-table-column prop="guide" label="指南版本" width="100" />
        <el-table-column prop="due" label="截止时间" width="160" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="pick(row)">查看进度</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 进度 -->
    <div v-else v-loading="loading">
      <el-card shadow="never" class="mb">
        <template #header><span class="title">任务进度</span></template>
        <div class="stat-row">
          <div class="stat"><div class="num">{{ progress?.due ?? 0 }}</div><div class="lbl">应完成人数</div></div>
          <div class="stat"><div class="num green">{{ progress?.submitted ?? 0 }}</div><div class="lbl">已提交</div></div>
          <div class="stat"><div class="num red">{{ progress?.unsubmitted ?? 0 }}</div><div class="lbl">未提交</div></div>
          <div class="stat"><div class="num red">{{ progress?.conflictCount ?? 0 }}</div><div class="lbl">冲突条数</div></div>
          <div class="stat"><div class="num">{{ progress?.confirmedCount ?? 0 }}</div><div class="lbl">已确认</div></div>
        </div>
      </el-card>

      <el-card shadow="never">
        <template #header><span class="title">标注员提交情况</span></template>
        <el-table :data="progress?.annotators || []">
          <el-table-column prop="name" label="标注员" width="120" />
          <el-table-column prop="status" label="提交状态" width="120">
            <template #default="{ row }">
              <el-tag :type="row.status === '已提交' ? 'success' : 'info'" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="submittedAt" label="提交时间" min-width="160" />
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.title { font-weight: 600; }
.mb { margin-bottom: 14px; }
.stat-row { display: flex; gap: 24px; }
.stat { text-align: center; }
.num { font-size: 28px; font-weight: 700; }
.num.green { color: #67c23a; }
.num.red { color: #f56c6c; }
.lbl { color: #909399; font-size: 12px; }
</style>
