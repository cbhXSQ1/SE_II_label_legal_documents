<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { taskApi, documentApi, userApi, guideApi } from '@/api'
import { TASK_STATUS, TASK_STATUS_TAG } from '@/utils/dict'

const router = useRouter()
const loading = ref(false)
const tasks = ref([])

const createVisible = ref(false)
const createForm = reactive({ name: '', description: '', deadline: '', guideVersion: '', documentId: '', annotatorIds: [], arbiterId: '', draft: false })
const guides = ref([])
const documents = ref([])
const annotators = ref([])
const arbiters = ref([])
const creating = ref(false)

async function load() {
  loading.value = true
  try { tasks.value = await taskApi.myTasks() } catch (e) { /* 拦截器已提示 */ } finally { loading.value = false }
}

async function openCreate() {
  const [g, d, a, b] = await Promise.all([
    guideApi.list(),
    documentApi.list(),
    userApi.list({ identity: 'ANNOTATOR' }),
    userApi.list({ identity: 'ARBITER' })
  ])
  guides.value = g.versions || []
  documents.value = d.records || []
  annotators.value = a.users || []
  arbiters.value = b.users || []
  Object.assign(createForm, {
    name: '', description: '', deadline: '',
    guideVersion: guides.value[0]?.version || '',
    documentId: '', annotatorIds: [], arbiterId: '', draft: false
  })
  createVisible.value = true
}

async function onCreate() {
  if (!createForm.name || !createForm.guideVersion || !createForm.documentId || createForm.annotatorIds.length < 2 || !createForm.arbiterId) {
    ElMessage.warning('请填写任务名称，并选择指南版本、文书、至少两名标注员和一名仲裁员')
    return
  }
  creating.value = true
  try {
    await taskApi.create(createForm)
    ElMessage.success(createForm.draft ? '草稿已保存' : '任务已发布')
    createVisible.value = false
    load()
  } catch (e) { /* 拦截器已提示 */ } finally { creating.value = false }
}

async function onPublish(t) {
  await taskApi.publish(t.id)
  ElMessage.success('已发布')
  load()
}

async function onWithdraw(t) {
  try { await ElMessageBox.confirm('确认撤回该任务？已提交的标注结果保留。', '撤回任务', { type: 'warning' }) } catch (e) { return }
  await taskApi.withdraw(t.id, { reason: '手动撤回' })
  ElMessage.success('已撤回')
  load()
}

function toProgress(t) {
  router.push({ path: '/task/progress', query: { taskId: t.id } })
}

onMounted(load)
</script>

<template>
  <el-card shadow="never" v-loading="loading">
    <template #header>
      <div class="head">
        <span class="title">任务列表</span>
        <el-button type="primary" @click="openCreate">新建任务</el-button>
      </div>
    </template>
    <el-table :data="tasks" empty-text="暂无任务">
      <el-table-column prop="name" label="任务名称" min-width="200" />
      <el-table-column prop="guide" label="指南版本" width="100" />
      <el-table-column prop="due" label="截止时间" width="160" />
      <el-table-column label="标注员" min-width="130">
        <template #default="{ row }">{{ (row.annotators || []).join('、') }}</template>
      </el-table-column>
      <el-table-column prop="arbiter" label="仲裁员" width="90" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="TASK_STATUS_TAG[row.status]" size="small">{{ TASK_STATUS[row.status] || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{ row }">
          <el-button v-if="row.status === 'DRAFT'" type="primary" size="small" @click="onPublish(row)">发布</el-button>
          <el-button v-if="row.status === 'PUBLISHED'" type="danger" size="small" @click="onWithdraw(row)">撤回</el-button>
          <el-button size="small" @click="toProgress(row)">进度</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="createVisible" title="新建任务" width="620px">
    <el-form label-width="100px">
      <el-form-item label="任务名称"><el-input v-model="createForm.name" /></el-form-item>
      <el-form-item label="任务说明"><el-input v-model="createForm.description" type="textarea" /></el-form-item>
      <el-form-item label="截止时间">
        <el-date-picker v-model="createForm.deadline" type="datetime" placeholder="选择截止时间" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%;" />
      </el-form-item>
      <el-form-item label="指南版本">
        <el-select v-model="createForm.guideVersion" style="width: 100%;">
          <el-option v-for="g in guides" :key="g.version" :label="g.version + '（' + g.scope + '）'" :value="g.version" />
        </el-select>
      </el-form-item>
      <el-form-item label="文书">
        <el-select v-model="createForm.documentId" filterable style="width: 100%;" placeholder="选择一份文书">
          <el-option v-for="d in documents" :key="d.id" :label="d.title" :value="d.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="标注员">
        <el-select v-model="createForm.annotatorIds" multiple collapse-tags style="width: 100%;" placeholder="至少选择两名标注员">
          <el-option v-for="a in annotators" :key="a.id" :label="a.name" :value="a.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="仲裁员">
        <el-select v-model="createForm.arbiterId" style="width: 100%;" placeholder="选择一名仲裁员">
          <el-option v-for="a in arbiters" :key="a.id" :label="a.name" :value="a.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="保存为草稿"><el-switch v-model="createForm.draft" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createVisible = false">取消</el-button>
      <el-button type="primary" :loading="creating" @click="onCreate">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; }
.title { font-weight: 600; }
</style>
