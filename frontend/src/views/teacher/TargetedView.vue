<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { teacherApi } from '@/api'

const loading = ref(false)
const points = ref([])
const questions = ref([])
const form = reactive({ name: '', description: '', deadline: '', questionIds: [] })

async function load() {
  loading.value = true
  try {
    const r = await teacherApi.targeted()
    points.value = r.points || []
    questions.value = r.questions || []
  } catch (e) { /* ignore */ } finally { loading.value = false }
}
async function onPublish() {
  if (form.questionIds.length === 0) { ElMessage.warning('请选择题目'); return }
  await teacherApi.saveExercise({ name: form.name || '针对性练习', description: form.description, deadline: form.deadline, allowRedo: true, guideVersion: 'v1.2', classId: 'c1', questionIds: form.questionIds })
  ElMessage.success('针对性练习已发布')
}
onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <el-card shadow="never" class="mb">
      <template #header><span class="title">薄弱知识点（按最好成绩升序）</span></template>
      <el-table :data="points" empty-text="当前知识点成绩都在薄弱阈值以上">
        <el-table-column prop="name" label="知识点" min-width="140" />
        <el-table-column prop="bestScore" label="最好成绩" width="120">
          <template #default="{ row }"><span class="red">{{ row.bestScore }}%</span></template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never">
      <template #header><span class="title">从本班题目池选择题目</span></template>
      <el-checkbox-group v-model="form.questionIds" class="qlist">
        <el-checkbox v-for="q in questions" :key="q.id" :value="q.id">{{ q.id }} · {{ q.stem }}</el-checkbox>
      </el-checkbox-group>
      <el-divider />
      <el-form inline>
        <el-form-item label="任务名称"><el-input v-model="form.name" placeholder="针对性练习" /></el-form-item>
        <el-form-item label="截止时间"><el-date-picker v-model="form.deadline" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item>
        <el-form-item><el-button type="primary" @click="onPublish">发布针对性练习</el-button></el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>.title { font-weight: 600; } .mb { margin-bottom: 14px; } .red { color: #f56c6c; } .qlist { display: flex; flex-direction: column; gap: 8px; }</style>
