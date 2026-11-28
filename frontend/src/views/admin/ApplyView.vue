<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api'
import { IDENTITIES, SYSTEMS } from '@/utils/dict'

const loading = ref(false)
const list = ref([])
const reviewVisible = ref(false)
const current = ref(null)
const action = ref('APPROVE')
const note = ref('')

async function load() {
  loading.value = true
  try { list.value = (await adminApi.applications()).records || [] } catch (e) { /* ignore */ } finally { loading.value = false }
}

function openReview(row, act) {
  current.value = row
  action.value = act
  note.value = ''
  reviewVisible.value = true
}

async function onReview() {
  await adminApi.review({ applicationId: current.value.id, action: action.value, note: note.value })
  current.value.status = action.value === 'APPROVE' ? 'APPROVED' : action.value === 'REJECT' ? 'REJECTED' : 'RETURNED'
  reviewVisible.value = false
  ElMessage.success('已处理')
  load()
}

onMounted(load)
</script>

<template>
  <el-card shadow="never" v-loading="loading">
    <template #header><span class="title">账号申请</span></template>
    <el-table :data="list" empty-text="暂无待审批申请">
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column prop="unit" label="单位" width="120" />
      <el-table-column label="申请的系统 / 身份" min-width="180">
        <template #default="{ row }">{{ SYSTEMS[row.system] }} · {{ IDENTITIES[row.identity] }}</template>
      </el-table-column>
      <el-table-column prop="submittedAt" label="申请时间" width="160" />
      <el-table-column label="操作" width="210">
        <template #default="{ row }">
          <el-button type="success" size="small" @click="openReview(row, 'APPROVE')">批准</el-button>
          <el-button type="warning" size="small" @click="openReview(row, 'RETURN')">退回补充</el-button>
          <el-button type="danger" size="small" @click="openReview(row, 'REJECT')">拒绝</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="reviewVisible" :title="action === 'APPROVE' ? '批准申请' : action === 'REJECT' ? '拒绝申请' : '退回补充'" width="460px">
    <template v-if="current">
      <p><b>申请人：</b>{{ current.name }}（{{ IDENTITIES[current.identity] }}）</p>
      <p><b>说明：</b>{{ current.note }}</p>
      <el-input v-model="note" type="textarea" :rows="3" placeholder="审批说明" />
    </template>
    <template #footer>
      <el-button @click="reviewVisible = false">取消</el-button>
      <el-button type="primary" @click="onReview">确认</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>.title { font-weight: 600; }</style>
