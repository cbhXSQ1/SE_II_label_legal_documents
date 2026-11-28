<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { studentApi } from '@/api'

const route = useRoute()
const exerciseId = computed(() => route.query.exerciseId || '')
const loading = ref(false)
const exercise = ref(null)
const answers = reactive({})
const submitting = ref(false)

const OBJ = ['单选', '多选', '判断']

async function load() {
  if (!exerciseId.value) return
  loading.value = true
  try { exercise.value = await studentApi.exerciseDetail(exerciseId.value) } catch (e) { /* ignore */ } finally { loading.value = false }
}
async function onSubmit() {
  const unanswered = (exercise.value?.questions || []).filter((q) => answers[q.id] == null || answers[q.id] === '')
  if (unanswered.length > 0) { ElMessage.warning('还有未作答题目，请继续作答'); return }
  submitting.value = true
  try {
    const r = await studentApi.answerSubmit({ exerciseId: exerciseId.value, answers: Object.keys(answers).map((qid) => ({ questionId: qid, content: answers[qid] })), requestId: 'req-' + Date.now() })
    ElMessage.success('提交成功，得分 ' + r.score)
  } catch (e) { /* ignore */ } finally { submitting.value = false }
}
onMounted(load)
</script>

<template>
  <el-card shadow="never" v-loading="loading">
    <template #header><span class="title">作答：{{ exercise?.name || '' }}</span></template>
    <el-empty v-if="!exercise" description="请从练习任务进入作答" />
    <div v-else>
      <div v-for="q in exercise.questions" :key="q.id" class="q">
        <div class="stem">{{ q.id }}. {{ q.stem }}</div>
        <el-radio-group v-if="q.type === '单选'" v-model="answers[q.id]">
          <el-radio v-for="(o, i) in q.options" :key="i" :value="String(i)">{{ o }}</el-radio>
        </el-radio-group>
        <el-checkbox-group v-else-if="q.type === '多选'" v-model="answers[q.id]">
          <el-checkbox v-for="(o, i) in q.options" :key="i" :value="String(i)">{{ o }}</el-checkbox>
        </el-checkbox-group>
        <el-radio-group v-else-if="q.type === '判断'" v-model="answers[q.id]">
          <el-radio value="正确">正确</el-radio>
          <el-radio value="错误">错误</el-radio>
        </el-radio-group>
        <el-tag v-else type="primary" size="small">标注题（切割/要素/关系/图示）</el-tag>
      </div>
      <el-divider />
      <el-button type="primary" :loading="submitting" @click="onSubmit">提交</el-button>
    </div>
  </el-card>
</template>

<style scoped>.title { font-weight: 600; } .q { padding: 12px 0; border-bottom: 1px dashed #f0f0f0; } .stem { font-weight: 600; margin-bottom: 8px; }</style>
