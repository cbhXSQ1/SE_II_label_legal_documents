<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { teacherApi, guideApi } from '@/api'
import { QUESTION_TYPES } from '@/utils/dict'

const loading = ref(false)
const exercises = ref([])
const questions = ref([])
const activeTab = ref('exercises')

const exerciseVisible = ref(false)
const exerciseForm = reactive({ id: '', name: '', description: '', deadline: '', allowRedo: true, guideVersion: '', questionIds: [] })

const questionVisible = ref(false)
const questionForm = reactive({ id: '', type: '单选', stem: '', guideVersion: '', difficulty: '中', pointIds: [], options: ['', ''], answer: '' })

async function load() {
  loading.value = true
  try {
    const [e, q] = await Promise.all([teacherApi.exercises(), teacherApi.questions()])
    exercises.value = e.records || []
    questions.value = q.records || []
  } catch (err) { /* ignore */ } finally { loading.value = false }
}

async function openExercise() {
  const g = await guideApi.list().catch(() => ({ versions: [] }))
  Object.assign(exerciseForm, { id: '', name: '', description: '', deadline: '', allowRedo: true, guideVersion: g.versions[0]?.version || '', questionIds: [] })
  exerciseVisible.value = true
}
async function onSaveExercise() {
  if (!exerciseForm.name || exerciseForm.questionIds.length === 0) { ElMessage.warning('请填写任务名称并至少选择一道题目'); return }
  await teacherApi.saveExercise(exerciseForm)
  ElMessage.success('已发布')
  exerciseVisible.value = false
  load()
}

function openQuestion() {
  Object.assign(questionForm, { id: '', type: '单选', stem: '', guideVersion: 'v1.2', difficulty: '中', pointIds: [], options: ['', ''], answer: '' })
  questionVisible.value = true
}
async function onSaveQuestion() {
  if (!questionForm.stem) { ElMessage.warning('请填写题干'); return }
  await teacherApi.saveQuestion(questionForm)
  ElMessage.success('已保存')
  questionVisible.value = false
  load()
}

onMounted(load)
</script>

<template>
  <el-card shadow="never" v-loading="loading">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="练习任务" name="exercises">
        <div class="mb"><el-button type="primary" @click="openExercise">新建练习任务</el-button></div>
        <el-table :data="exercises" empty-text="暂无练习任务">
          <el-table-column prop="name" label="任务名称" min-width="180" />
          <el-table-column prop="guideVersion" label="指南版本" width="100" />
          <el-table-column prop="deadline" label="截止时间" width="160" />
          <el-table-column label="题目数" width="90">
            <template #default="{ row }">{{ (row.questionIds || []).length }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="题目池" name="questions">
        <div class="mb"><el-button type="primary" @click="openQuestion">新建题目</el-button></div>
        <el-table :data="questions" empty-text="暂无题目">
          <el-table-column prop="stem" label="题干" min-width="260" />
          <el-table-column label="题型" width="100">
            <template #default="{ row }">{{ (QUESTION_TYPES.find((t) => t.code === row.type) || {}).name || row.type }}</template>
          </el-table-column>
          <el-table-column prop="difficulty" label="难度" width="70" />
          <el-table-column prop="version" label="版本" width="70" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </el-card>

  <el-dialog v-model="exerciseVisible" title="新建练习任务" width="560px">
    <el-form label-width="90px">
      <el-form-item label="任务名称"><el-input v-model="exerciseForm.name" /></el-form-item>
      <el-form-item label="任务说明"><el-input v-model="exerciseForm.description" type="textarea" /></el-form-item>
      <el-form-item label="截止时间"><el-date-picker v-model="exerciseForm.deadline" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%;" /></el-form-item>
      <el-form-item label="允许重做"><el-switch v-model="exerciseForm.allowRedo" /></el-form-item>
      <el-form-item label="选择题目">
        <el-select v-model="exerciseForm.questionIds" multiple collapse-tags style="width: 100%;">
          <el-option v-for="q in questions" :key="q.id" :label="q.id + ' ' + q.stem" :value="q.id" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="exerciseVisible = false">取消</el-button>
      <el-button type="primary" @click="onSaveExercise">发布</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="questionVisible" title="新建题目" width="560px">
    <el-form label-width="90px">
      <el-form-item label="题型">
        <el-select v-model="questionForm.type" style="width: 100%;">
          <el-option v-for="t in QUESTION_TYPES" :key="t.code" :label="t.name" :value="t.code" />
        </el-select>
      </el-form-item>
      <el-form-item label="题干"><el-input v-model="questionForm.stem" type="textarea" :rows="3" /></el-form-item>
      <el-form-item label="难度">
        <el-radio-group v-model="questionForm.difficulty">
          <el-radio value="易">易</el-radio><el-radio value="中">中</el-radio><el-radio value="难">难</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="['单选', '多选', '判断'].includes(questionForm.type)" label="选项">
        <div class="opts">
          <el-input v-for="(o, i) in questionForm.options" :key="i" v-model="questionForm.options[i]" size="small" class="opt" :placeholder="'选项 ' + (i + 1)" />
        </div>
      </el-form-item>
      <el-form-item v-if="['单选', '多选', '判断'].includes(questionForm.type)" label="正确答案"><el-input v-model="questionForm.answer" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="questionVisible = false">取消</el-button>
      <el-button type="primary" @click="onSaveQuestion">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>.mb { margin-bottom: 12px; } .opts { width: 100%; } .opt { margin-bottom: 6px; }</style>
