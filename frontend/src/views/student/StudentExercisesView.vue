<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { studentApi } from '@/api'

const router = useRouter()
const loading = ref(false)
const list = ref([])

async function load() {
  loading.value = true
  try { list.value = (await studentApi.exercises()).exercises || [] } catch (e) { /* ignore */ } finally { loading.value = false }
}
function enter(row) {
  router.push({ path: '/student/answer', query: { exerciseId: row.id } })
}
onMounted(load)
</script>

<template>
  <el-card shadow="never" v-loading="loading">
    <template #header><span class="title">练习任务（本班已发布）</span></template>
    <el-table :data="list" empty-text="暂无练习任务">
      <el-table-column prop="name" label="任务名称" min-width="200" />
      <el-table-column prop="deadline" label="截止时间" width="170" />
      <el-table-column label="允许重做" width="90">
        <template #default="{ row }">{{ row.allowRedo ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column prop="completion" label="完成状态" width="100" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="enter(row)">进入作答</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<style scoped>.title { font-weight: 600; }</style>
