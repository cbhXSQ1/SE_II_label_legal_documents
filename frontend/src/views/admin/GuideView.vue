<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adminApi } from '@/api'

const loading = ref(false)
const versions = ref([])
const latest = ref('')

const importVisible = ref(false)
const form = reactive({ version: '', scope: '' })
const parsed = ref(null)
const parsing = ref(false)
const saving = ref(false)

async function load() {
  loading.value = true
  try {
    const r = await adminApi.guide()
    versions.value = r.versions || []
    latest.value = r.latest || ''
  } catch (e) { /* ignore */ } finally { loading.value = false }
}

function openImport() {
  Object.assign(form, { version: '', scope: '' })
  parsed.value = null
  importVisible.value = true
}

async function onParse() {
  parsing.value = true
  try {
    const r = await adminApi.parseGuide({ fileId: 'mock-file-id' })
    parsed.value = r.content
    if (!form.version) form.version = latest.value || 'v1.2'
  } catch (e) { /* ignore */ } finally { parsing.value = false }
}

async function onSave() {
  if (!form.version || !form.scope) { ElMessage.warning('请填写版本号和适用范围'); return }
  saving.value = true
  try {
    await adminApi.importGuide({ fileId: 'mock-file-id', version: form.version, scope: form.scope, content: parsed.value })
    ElMessage.success('指南版本已保存')
    importVisible.value = false
    load()
  } catch (e) { /* ignore */ } finally { saving.value = false }
}

onMounted(load)
</script>

<template>
  <el-card shadow="never" v-loading="loading">
    <template #header>
      <div class="head"><span class="title">标注指南</span><el-button type="primary" @click="openImport">导入指南</el-button></div>
    </template>
    <el-table :data="versions" empty-text="暂无指南版本">
      <el-table-column prop="version" label="版本号" width="100" />
      <el-table-column prop="scope" label="适用范围" min-width="160" />
      <el-table-column prop="operator" label="操作人" width="100" />
      <el-table-column prop="importedAt" label="导入时间" width="170" />
      <el-table-column prop="status" label="状态" width="90" />
    </el-table>
  </el-card>

  <el-dialog v-model="importVisible" title="导入指南" width="680px">
    <el-form label-width="100px">
      <el-form-item label="版本号"><el-input v-model="form.version" placeholder="如 v1.3" /></el-form-item>
      <el-form-item label="适用范围"><el-input v-model="form.scope" placeholder="如：全部任务 / 仅教学" /></el-form-item>
      <el-form-item label="指南文件">
        <el-button :loading="parsing" @click="onParse">解析指南文件（读取标签/关系/图示规则）</el-button>
      </el-form-item>
    </el-form>
    <template v-if="parsed">
      <el-divider content-position="left">读出的内容（可修改）</el-divider>
      <el-form label-width="100px">
        <el-form-item label="非命题要素"><el-input v-model="parsed.nonProps" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="命题要素"><el-input v-model="parsed.props" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="关系类型"><el-input v-model="parsed.relations" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="图示规则"><el-input v-model="parsed.diagramRules" type="textarea" :rows="2" /></el-form-item>
      </el-form>
    </template>
    <template #footer>
      <el-button @click="importVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="onSave">保存版本</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>.title { font-weight: 600; } .head { display: flex; justify-content: space-between; align-items: center; }</style>
