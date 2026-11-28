<script setup>
import { ref, onMounted } from 'vue'
import { teachingAdminApi } from '@/api'
import { IDENTITIES } from '@/utils/dict'

const loading = ref(false)
const list = ref([])

async function load() {
  loading.value = true
  try { list.value = (await teachingAdminApi.accounts()).records || [] } catch (e) { /* ignore */ } finally { loading.value = false }
}

onMounted(load)
</script>

<template>
  <el-card shadow="never" v-loading="loading">
    <template #header><span class="title">教学系统账号</span></template>
    <el-table :data="list" empty-text="暂无账号">
      <el-table-column prop="account" label="账号" width="140" />
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column label="身份" width="120">
        <template #default="{ row }">{{ IDENTITIES[row.identity] }}</template>
      </el-table-column>
      <el-table-column prop="className" label="班级" min-width="140" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'" size="small">{{ row.status === 'ENABLED' ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<style scoped>.title { font-weight: 600; }</style>
