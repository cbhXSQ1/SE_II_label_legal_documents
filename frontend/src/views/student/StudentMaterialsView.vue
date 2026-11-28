<script setup>
import { ref, onMounted } from 'vue'
import { studentApi } from '@/api'

const loading = ref(false)
const list = ref([])

async function load() {
  loading.value = true
  try { list.value = (await studentApi.materials()).materials || [] } catch (e) { /* ignore */ } finally { loading.value = false }
}
onMounted(load)
</script>

<template>
  <el-card shadow="never" v-loading="loading">
    <template #header><span class="title">学习材料（本班已发布）</span></template>
    <el-table :data="list" empty-text="暂无学习材料">
      <el-table-column prop="title" label="标题" min-width="200" />
      <el-table-column prop="type" label="类型" width="100" />
      <el-table-column prop="guideVersion" label="指南版本" width="100" />
      <el-table-column prop="publishedAt" label="发布时间" width="170" />
      <el-table-column prop="completion" label="完成状态" width="100" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button type="primary" size="small">打开学习</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<style scoped>.title { font-weight: 600; }</style>
