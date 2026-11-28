<script setup>
import { ref, onMounted } from 'vue'
import { studentApi } from '@/api'

const loading = ref(false)
const list = ref([])
const detailVisible = ref(false)
const detail = ref(null)

async function load() {
  loading.value = true
  try { list.value = (await studentApi.answers()).answers || [] } catch (e) { /* ignore */ } finally { loading.value = false }
}
async function openDetail(row) {
  detail.value = await studentApi.answerDetail(row.id)
  detailVisible.value = true
}
onMounted(load)
</script>

<template>
  <el-card shadow="never" v-loading="loading">
    <template #header><span class="title">作答记录</span></template>
    <el-table :data="list" empty-text="暂无作答记录">
      <el-table-column prop="exerciseId" label="任务" width="100" />
      <el-table-column prop="submittedAt" label="提交时间" width="170" />
      <el-table-column prop="score" label="得分" width="80" />
      <el-table-column prop="status" label="状态" width="110" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="openDetail(row)">查看差异</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="detailVisible" title="作答差异与反馈" width="560px">
    <template v-if="detail">
      <p><b>系统反馈：</b>{{ detail.systemFeedback || '—' }}</p>
      <p><b>教师反馈：</b>{{ detail.teacherFeedback ? detail.teacherFeedback.text : '教师还没有填写反馈' }}</p>
      <p><b>差异明细：</b></p>
      <el-empty v-if="!detail.differences?.length" description="无差异" />
    </template>
  </el-dialog>
</template>

<style scoped>.title { font-weight: 600; }</style>
