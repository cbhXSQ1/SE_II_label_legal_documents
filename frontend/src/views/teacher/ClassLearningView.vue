<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { teacherApi } from '@/api'

const loading = ref(false)
const students = ref([])
const threshold = ref(60)

async function load() {
  loading.value = true
  try {
    const r = await teacherApi.classLearning()
    students.value = r.students || []
    threshold.value = r.threshold || 60
  } catch (e) { /* ignore */ } finally { loading.value = false }
}
async function onSaveThreshold() {
  await teacherApi.saveThreshold({ classId: 'c1', threshold: threshold.value })
  ElMessage.success('薄弱阈值已保存')
}
onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <el-card shadow="never" class="mb">
      <template #header><span class="title">班级学习情况</span></template>
      <div class="bar">
        <span>薄弱阈值（%）</span>
        <el-input-number v-model="threshold" :min="0" :max="100" />
        <el-button type="primary" @click="onSaveThreshold">保存</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header><span class="title">学生列表</span></template>
      <el-table :data="students" empty-text="暂无学生">
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="materials" label="材料完成" width="100" />
        <el-table-column prop="exercises" label="练习完成" width="100" />
        <el-table-column prop="scores" label="平均分" width="100" />
        <el-table-column label="薄弱知识点" min-width="160">
          <template #default="{ row }"><el-tag v-for="w in row.weakPoints" :key="w" size="small" type="danger" class="mr">{{ w }}</el-tag></template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>.title { font-weight: 600; } .mb { margin-bottom: 14px; } .bar { display: flex; align-items: center; gap: 12px; } .mr { margin-right: 6px; }</style>
