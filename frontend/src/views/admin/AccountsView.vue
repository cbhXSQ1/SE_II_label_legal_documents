<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api'
import { IDENTITIES } from '@/utils/dict'

const loading = ref(false)
const list = ref([])
const roleVisible = ref(false)
const form = reactive({ accountId: '', name: '', roles: [], status: 'ENABLED' })
const ALL_ROLES = Object.keys(IDENTITIES)

async function load() {
  loading.value = true
  try { list.value = (await adminApi.accounts()).records || [] } catch (e) { /* ignore */ } finally { loading.value = false }
}

function openRole(row) {
  Object.assign(form, { accountId: row.id, name: row.name, roles: [...row.roles], status: row.status })
  roleVisible.value = true
}

async function onSave() {
  if (form.roles.length === 0) { ElMessage.warning('账号至少要有一个角色'); return }
  await adminApi.updateRoles({ accountId: form.accountId, roles: form.roles, status: form.status, reason: '手动调整' })
  roleVisible.value = false
  ElMessage.success('已保存')
  load()
}

onMounted(load)
</script>

<template>
  <el-card shadow="never" v-loading="loading">
    <template #header><span class="title">账号列表</span></template>
    <el-table :data="list">
      <el-table-column prop="account" label="账号" width="140" />
      <el-table-column prop="name" label="姓名" width="100" />
      <el-table-column prop="unit" label="单位" width="120" />
      <el-table-column label="角色" min-width="200">
        <template #default="{ row }">
          <el-tag v-for="r in row.roles" :key="r" size="small" class="mr">{{ IDENTITIES[r] || r }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ENABLED' ? 'success' : 'danger'" size="small">{{ row.status === 'ENABLED' ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="openRole(row)">角色和状态</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="roleVisible" :title="'角色和状态：' + form.name" width="480px">
    <el-form label-width="90px">
      <el-form-item label="角色">
        <el-checkbox-group v-model="form.roles">
          <el-checkbox v-for="r in ALL_ROLES" :key="r" :value="r">{{ IDENTITIES[r] }}</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      <el-form-item label="状态">
        <el-radio-group v-model="form.status">
          <el-radio value="ENABLED">启用</el-radio>
          <el-radio value="DISABLED">停用</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="roleVisible = false">取消</el-button>
      <el-button type="primary" @click="onSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>.title { font-weight: 600; } .mr { margin-right: 6px; }</style>
