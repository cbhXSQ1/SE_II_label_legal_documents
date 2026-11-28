<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { teacherApi } from '@/api'

const CATEGORIES = ['文本切割', '非命题要素', '命题要素', '关系类型', '嵌套结构', '论证图示']
const loading = ref(false)
const list = ref([])
const dialogVisible = ref(false)
const form = reactive({ id: '', name: '', category: '', description: '', difficulty: '中' })

async function load() {
  loading.value = true
  try { list.value = (await teacherApi.points()).records || [] } catch (e) { /* ignore */ } finally { loading.value = false }
}
function openCreate() { Object.assign(form, { id: '', name: '', category: '', description: '', difficulty: '中' }); dialogVisible.value = true }
function openEdit(row) { Object.assign(form, { id: row.id, name: row.name, category: row.category, description: row.description, difficulty: row.difficulty }); dialogVisible.value = true }
async function onSave() {
  if (!form.name) { ElMessage.warning('请填写知识点名称'); return }
  if (!form.category) { ElMessage.warning('请选择类别'); return }
  await teacherApi.savePoint(form)
  ElMessage.success('已保存')
  dialogVisible.value = false
  load()
}
async function onDelete(row) {
  try { await ElMessageBox.confirm('删除后关联的学习材料和题目保留，是否继续？', '删除知识点', { type: 'warning' }) } catch (e) { return }
  await teacherApi.deletePoint({ id: row.id })
  ElMessage.success('已删除')
  load()
}
onMounted(load)
</script>

<template>
  <el-card shadow="never" v-loading="loading">
    <template #header>
      <div class="head"><span class="title">知识点</span><el-button type="primary" @click="openCreate">新建知识点</el-button></div>
    </template>
    <el-table :data="list" empty-text="暂无知识点">
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column prop="category" label="类别" width="120" />
      <el-table-column prop="difficulty" label="难度" width="80" />
      <el-table-column prop="materialCount" label="关联材料" width="90" />
      <el-table-column prop="questionCount" label="关联题目" width="90" />
      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">修改</el-button>
          <el-button link type="danger" @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="form.id ? '修改知识点' : '新建知识点'" width="480px">
    <el-form label-width="80px">
      <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="类别">
        <el-select v-model="form.category" style="width: 100%;">
          <el-option v-for="c in CATEGORIES" :key="c" :label="c" :value="c" />
        </el-select>
      </el-form-item>
      <el-form-item label="难度">
        <el-radio-group v-model="form.difficulty">
          <el-radio value="易">易</el-radio><el-radio value="中">中</el-radio><el-radio value="难">难</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="说明"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="onSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>.title { font-weight: 600; } .head { display: flex; justify-content: space-between; align-items: center; }</style>
