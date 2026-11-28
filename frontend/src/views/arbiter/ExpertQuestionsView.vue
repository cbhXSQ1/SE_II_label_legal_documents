<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { adjudicationApi } from '@/api'

const loading = ref(false)
const questions = ref([])

const replyVisible = ref(false)
const current = ref(null)
const replyForm = ref({ conclusion: '', reason: '', kind: '结论' })

async function load() {
  loading.value = true
  try { questions.value = (await adjudicationApi.expertQuestions()).records || [] } catch (e) { /* ignore */ } finally { loading.value = false }
}

function openReply(row) {
  current.value = row
  replyForm.value = { conclusion: '', reason: '', kind: '结论' }
  replyVisible.value = true
}

async function onReply() {
  const { conclusion, reason, kind } = replyForm.value
  await adjudicationApi.expertReply({ questionId: current.value.id, conclusion, reason, needMore: kind === '补充', unanswerable: kind === '无法判断' })
  current.value.status = 'REPLIED'
  replyVisible.value = false
  ElMessage.success('已回复')
}

onMounted(load)
</script>

<template>
  <el-card shadow="never" v-loading="loading">
    <template #header><span class="title">待回复问题</span></template>
    <el-table :data="questions" empty-text="暂无待回复问题">
      <el-table-column prop="taskId" label="任务" width="80" />
      <el-table-column prop="conflictLabel" label="冲突标签" width="110" />
      <el-table-column prop="guideClause" label="指南条款" min-width="180" />
      <el-table-column prop="annotatorResults" label="各标注员结果" min-width="220" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'PENDING' ? 'warning' : 'success'" size="small">{{ row.status === 'PENDING' ? '待回复' : '已回复' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" type="primary" size="small" @click="openReply(row)">回复</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="replyVisible" title="回复问题" width="520px">
    <template v-if="current">
      <p><b>冲突标签：</b>{{ current.conflictLabel }}</p>
      <p><b>指南条款：</b>{{ current.guideClause }}</p>
      <p><b>各标注员结果：</b>{{ current.annotatorResults }}</p>
      <el-form label-width="90px">
        <el-form-item label="回复类型">
          <el-radio-group v-model="replyForm.kind">
            <el-radio value="结论">结论</el-radio>
            <el-radio value="补充">需要补充信息</el-radio>
            <el-radio value="无法判断">无法判断</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="replyForm.kind === '结论'" label="结论"><el-input v-model="replyForm.conclusion" /></el-form-item>
        <el-form-item label="理由"><el-input v-model="replyForm.reason" type="textarea" :rows="3" /></el-form-item>
      </el-form>
    </template>
    <template #footer>
      <el-button @click="replyVisible = false">取消</el-button>
      <el-button type="primary" @click="onReply">回复</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.title { font-weight: 600; }
</style>
