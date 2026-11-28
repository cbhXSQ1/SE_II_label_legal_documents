<script setup>
import { ref, onMounted } from 'vue'
import { studentApi } from '@/api'

const loading = ref(false)
const stats = ref([])
const answers = ref([])

async function load() {
  loading.value = true
  try {
    const r = await studentApi.learning()
    stats.value = r.stats || []
    answers.value = r.answers || []
  } catch (e) { /* ignore */ } finally { loading.value = false }
}
onMounted(load)
</script>

<template>
  <el-card shadow="never" v-loading="loading">
    <template #header><span class="title">我的学习情况</span></template>
    <el-table :data="stats" empty-text="还没有学习记录">
      <el-table-column prop="pointId" label="知识点" width="120" />
      <el-table-column prop="practiceCount" label="练习次数" width="100" />
      <el-table-column prop="bestScore" label="最好成绩" width="100" />
      <el-table-column prop="recentScore" label="最近成绩" width="100" />
      <el-table-column prop="completion" label="完成状态" width="120" />
    </el-table>
  </el-card>
</template>

<style scoped>.title { font-weight: 600; }</style>
