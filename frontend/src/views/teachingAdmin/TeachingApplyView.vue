<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { teachingAdminApi } from '@/api'
import { IDENTITIES } from '@/utils/dict'

const loading = ref(false)
const list = ref([])
const reviewVisible = ref(false)
const current = ref(null)
const action = ref('APPROVE')

async function load() {
  loading.value = true
  try { list.value = (await teachingAdminApi.applications()).records || [] } catch (e) { /* ignore */ } finally { loading.value = false }
}

function openReview(row, act) { current.value = row; action.value = act; reviewVisible.value = true }

async function onReview() {
  await teachingAdminApi.review({ applicationId: current.value.id, action: action.value, identity: current.value.identity })
  current.value.status = action.value === 'APPROVE' ? 'APPROVED' : action.value === 'REJECT' ? 'REJECTED' : 'RETURNED'
  reviewVisible.value = false
  ElMessage.success('已处理')
  load()
}

onMounted(load)
</script>

<template>
  <el-card shadow="never" v-loading="loading">
    <template #header><span class="title">教学系统账号申请</span></template>
    <el-table :data="list" empty-text="暂无待审批申请">
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column prop="unit" label="单位" width="120" />
      <el-table-column label="申请身份" width="150">
        <template #default="{ row }">{{ IDENTITIES[row.identity] }}</template>
      </el-table-column>
      <el-table-column prop="submittedAt" label="申请时间" width="160" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button type="success" size="small" @click="openReview(row, 'APPROVE')">批准</el-button>
          <el-button type="warning" size="small" @click="openReview(row, 'RETURN')">退回</el-button>
          <el-button type="danger" size="small" @click="openReview(row, 'REJECT')">拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="reviewVisible" :title="action === 'APPROVE' ? '批准申请' : action === 'REJECT' ? '拒绝申请' : '退回补充'" width="420px">
    <template v-if="current"><p>确认处理 <b>{{ current.name }}</b> 的 {{ IDENTITIES[current.identity] }} 申请？</p></template>
    <template #footer>
      <el-button @click="reviewVisible = false">取消</el-button>
      <el-button type="primary" @click="onReview">确认</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>.title { font-weight: 600; }</style>
