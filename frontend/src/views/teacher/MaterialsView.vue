<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { teacherApi, guideApi } from '@/api'

const TYPES = ['指南讲解', '标准样例', '讲义', '微课']
const loading = ref(false)
const list = ref([])
const dialogVisible = ref(false)
const form = reactive({ id: '', title: '', type: '指南讲解', guideVersion: '', pointIds: [], classId: 'c1', publish: false })
const guides = ref([])

async function load() {
  loading.value = true
  try { list.value = (await teacherApi.materials()).records || [] } catch (e) { /* ignore */ } finally { loading.value = false }
}
async function openCreate() {
  const g = await guideApi.list().catch(() => ({ versions: [] }))
  guides.value = g.versions || []
  Object.assign(form, { id: '', title: '', type: '指南讲解', guideVersion: guides.value[0]?.version || '', pointIds: [], classId: 'c1', publish: false })
  dialogVisible.value = true
}
async function onSave() {
  if (!form.title || !form.guideVersion) { ElMessage.warning('请填写标题并选择指南版本'); return }
  await teacherApi.saveMaterial(form)
  ElMessage.success('已保存')
  dialogVisible.value = false
  load()
}
async function onStatus(row, status) {
  await teacherApi.materialStatus({ id: row.id, status })
  ElMessage.success(status === '已下架' ? '已下架' : '已发布')
  load()
}
onMounted(load)
</script>

<template>
  <el-card shadow="never" v-loading="loading">
    <template #header>
      <div class="head"><span class="title">学习材料</span><el-button type="primary" @click="openCreate">新建学习材料</el-button></div>
    </template>
    <el-table :data="list" empty-text="暂无学习材料">
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="type" label="类型" width="100" />
      <el-table-column prop="guideVersion" label="指南版本" width="100" />
      <el-table-column prop="status" label="状态" width="90" />
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button v-if="row.status !== '已发布'" link type="success" @click="onStatus(row, '已发布')">发布</el-button>
          <el-button v-if="row.status === '已发布'" link type="warning" @click="onStatus(row, '已下架')">下架</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="dialogVisible" title="新建学习材料" width="520px">
    <el-form label-width="90px">
      <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
      <el-form-item label="材料类型">
        <el-select v-model="form.type" style="width: 100%;">
          <el-option v-for="t in TYPES" :key="t" :label="t" :value="t" />
        </el-select>
      </el-form-item>
      <el-form-item label="指南版本">
        <el-select v-model="form.guideVersion" style="width: 100%;">
          <el-option v-for="g in guides" :key="g.version" :label="g.version" :value="g.version" />
        </el-select>
      </el-form-item>
      <el-form-item label="发布"><el-switch v-model="form.publish" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="onSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>.title { font-weight: 600; } .head { display: flex; justify-content: space-between; align-items: center; }</style>
