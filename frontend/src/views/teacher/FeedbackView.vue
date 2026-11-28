<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { teacherApi } from '@/api'

const loading = ref(false)
const list = ref([])
const dialogVisible = ref(false)
const current = ref(null)
const feedback = ref('')

async function load() {
  loading.value = true
  // 简化：直接用班级学习情况里的作答记录（demo 用静态一条）
  list.value = [
    { id: 'a1', student: '张三', task: '第一次理解确认练习', submittedAt: '2026-09-22 20:18', score: 87.5, status: '已批改' }
  ]
  loading.value = false
}
function openFeedback(row) { current.value = row; feedback.value = ''; dialogVisible.value = true }
async function onSave() {
  if (!feedback.value) { ElMessage.warning('请填写反馈内容'); return }
  await teacherApi.feedback({ answerId: current.value.id, feedback: feedback.value })
  current.value.status = '已批改'
  dialogVisible.value = false
  ElMessage.success('已保存反馈')
}
onMounted(load)
</script>

<template>
  <el-card shadow="never" v-loading="loading">
    <template #header><span class="title">教师反馈（本班作答记录）</span></template>
    <el-table :data="list" empty-text="暂无作答记录">
      <el-table-column prop="student" label="学生" width="100" />
      <el-table-column prop="task" label="任务" min-width="180" />
      <el-table-column prop="submittedAt" label="提交时间" width="170" />
      <el-table-column prop="score" label="得分" width="80" />
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="openFeedback(row)">批改/反馈</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="'反馈：' + (current?.student || '')" width="480px">
    <el-input v-model="feedback" type="textarea" :rows="5" placeholder="填写反馈内容" />
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="onSave">保存反馈</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>.title { font-weight: 600; }</style>
