<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { taskApi } from '@/api'
import { navFor } from '@/utils/nav'
import { TASK_STATUS, TASK_STATUS_TAG } from '@/utils/dict'

const auth = useAuthStore()
const router = useRouter()

const isTaskRole = ['ANNOTATOR', 'ARBITER', 'TASK_MANAGER'].includes(auth.identity)
const tasks = ref([])
const loading = ref(false)
const quickLinks = computed(() => navFor(auth.identity).filter((n) => n.path !== '/'))

function statusText(s) { return TASK_STATUS[s] || s }

function openTask(task) {
  if (auth.identity === 'ANNOTATOR') {
    router.push({ path: '/annotate', query: { taskId: task.id, documentId: task.documentId } })
  } else if (auth.identity === 'ARBITER') {
    router.push({ path: '/arbiter/compare', query: { taskId: task.id, documentId: task.documentId } })
  } else {
    router.push({ path: '/task/progress', query: { taskId: task.id } })
  }
}

onMounted(async () => {
  if (isTaskRole) {
    loading.value = true
    try { tasks.value = await taskApi.myTasks() } catch (e) { /* 拦截器已提示 */ }
    finally { loading.value = false }
  }
})
</script>

<template>
  <div>
    <div class="welcome">
      <div>
        <div class="hello">{{ auth.name }}，你好</div>
        <div class="sub">当前身份：{{ auth.systemName }} · {{ auth.identityName }}</div>
      </div>
    </div>

    <!-- 任务类角色：展示任务列表 -->
    <el-card v-if="isTaskRole" shadow="never" v-loading="loading">
      <template #header>
        <span style="font-weight:600">
          {{ auth.identity === 'ARBITER' ? '分配给我的任务' : auth.identity === 'TASK_MANAGER' ? '我创建的任务' : '分配给我的任务' }}
        </span>
      </template>
      <el-table :data="tasks" empty-text="暂无任务">
        <el-table-column prop="name" label="任务名称" min-width="200" />
        <el-table-column prop="guide" label="指南版本" width="100" />
        <el-table-column prop="due" label="截止时间" width="160" />
        <el-table-column label="标注员" min-width="130">
          <template #default="{ row }">{{ row.annotators.join('、') }}</template>
        </el-table-column>
        <el-table-column prop="arbiter" label="仲裁员" width="90" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="TASK_STATUS_TAG[row.status]" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button v-if="auth.identity === 'ANNOTATOR'" type="primary" size="small" @click="openTask(row)">进入标注</el-button>
            <el-button v-else-if="auth.identity === 'ARBITER'" type="primary" size="small" @click="openTask(row)">进入裁定</el-button>
            <el-button v-else type="primary" size="small" @click="openTask(row)">查看进度</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 其他角色：快捷入口 -->
    <div v-else class="quick-grid">
      <el-card v-for="n in quickLinks" :key="n.path" shadow="hover" class="quick-card" @click="router.push(n.path)">
        <div class="quick-title">{{ n.title }}</div>
      </el-card>
      <el-empty v-if="quickLinks.length === 0" description="暂无可用功能入口" />
    </div>
  </div>
</template>

<style scoped>
.welcome { background: #fff; border-radius: 8px; padding: 20px; margin-bottom: 16px; }
.hello { font-size: 20px; font-weight: 700; }
.sub { color: #909399; margin-top: 4px; }
.quick-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 14px; }
.quick-card { cursor: pointer; }
.quick-title { font-weight: 600; font-size: 15px; }
</style>
