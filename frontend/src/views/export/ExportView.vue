<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { exportApi } from '@/api'

const loading = ref(false)
const tasks = ref([])

const exportVisible = ref(false)
const exportForm = reactive({ taskId: '', content: '标注数据', dataFormat: 'Excel', diagramFormat: 'PNG', fileName: '' })
const exporting = ref(false)

async function load() {
  loading.value = true
  try {
    const r = await exportApi.tasks()
    tasks.value = r.tasks || []
  } catch (e) { /* 拦截器已提示 */ } finally { loading.value = false }
}

function openExport(t) {
  Object.assign(exportForm, { taskId: t.id, content: '标注数据', dataFormat: 'Excel', diagramFormat: 'PNG', fileName: t.id + '_标注导出' })
  exportVisible.value = true
}

async function onCreate() {
  if (!exportForm.fileName) { ElMessage.warning('请填写文件名'); return }
  exporting.value = true
  try {
    const r = await exportApi.create(exportForm)
    ElMessage.success('导出成功：' + r.downloadUrl)
    exportVisible.value = false
  } catch (e) { /* 拦截器已提示 */ } finally { exporting.value = false }
}

onMounted(load)
</script>

<template>
  <el-card shadow="never" v-loading="loading">
    <template #header>
      <div class="head">
        <span class="title">导出（仅显示已确认最终版本的任务）</span>
      </div>
    </template>
    <el-table :data="tasks" empty-text="没有可导出的任务（需先确认最终版本）">
      <el-table-column prop="name" label="任务名称" min-width="220" />
      <el-table-column prop="guideVersion" label="指南版本" width="100" />
      <el-table-column prop="confirmedAt" label="确认时间" width="170" />
      <el-table-column prop="annotatorCount" label="标注员人数" width="100" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="openExport(row)">导出</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="exportVisible" title="导出标注数据与图示" width="520px">
    <el-form label-width="100px">
      <el-form-item label="导出内容">
        <el-radio-group v-model="exportForm.content">
          <el-radio value="标注数据">标注数据</el-radio>
          <el-radio value="论证图示">论证图示</el-radio>
          <el-radio value="两者">两者</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="exportForm.content !== '论证图示'" label="数据格式">
        <el-radio-group v-model="exportForm.dataFormat">
          <el-radio value="Excel">Excel</el-radio>
          <el-radio value="JSON">JSON</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="exportForm.content !== '标注数据'" label="图示格式">
        <el-radio-group v-model="exportForm.diagramFormat">
          <el-radio value="PNG">PNG</el-radio>
          <el-radio value="JPG">JPG</el-radio>
          <el-radio value="SVG">SVG</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="文件名"><el-input v-model="exportForm.fileName" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="exportVisible = false">取消</el-button>
      <el-button type="primary" :loading="exporting" @click="onCreate">导出</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; }
.title { font-weight: 600; }
</style>
