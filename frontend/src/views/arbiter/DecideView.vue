<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { taskApi, adjudicationApi } from '@/api'

const route = useRoute()
const router = useRouter()
const taskId = computed(() => route.query.taskId || '')
const documentId = computed(() => route.query.documentId || '')

const pickerTasks = ref([])
const loading = ref(false)
const data = ref({ conflicts: [], decidedCount: 0, undecidedCount: 0 })

const decideVisible = ref(false)
const current = ref(null)
const method = ref('认可甲')

async function load() {
  if (!taskId.value || !documentId.value) {
    loading.value = true
    try { pickerTasks.value = await taskApi.myTasks() } catch (e) { /* ignore */ } finally { loading.value = false }
    return
  }
  loading.value = true
  try { data.value = await adjudicationApi.conflicts({ taskId: taskId.value, documentId: documentId.value, round: 'STRUCTURED' }) } catch (e) { /* ignore */ } finally { loading.value = false }
}

function openDecide(row) {
  current.value = row
  method.value = '认可甲'
  decideVisible.value = true
}

async function onDecide() {
  await adjudicationApi.decide({ taskId: taskId.value, documentId: documentId.value, round: 'STRUCTURED', conflictId: current.value.conflictId, differenceType: current.value.differenceType, differenceObject: current.value.differenceObject, valueA: current.value.valueA, valueB: current.value.valueB, method: method.value, result: method.value === '认可甲' ? current.value.valueA : method.value === '认可乙' ? current.value.valueB : '重新标注' })
  current.value.decided = true
  decideVisible.value = false
  data.value.decidedCount++
  data.value.undecidedCount--
  ElMessage.success('已裁定')
}

onMounted(load)
watch([taskId, documentId], load)
</script>

<template>
  <el-card v-if="!taskId" shadow="never" v-loading="loading">
    <template #header><span class="title">选择任务裁定冲突</span></template>
    <el-table :data="pickerTasks" empty-text="暂无任务">
      <el-table-column prop="name" label="任务名称" min-width="220" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="router.push({ path: '/arbiter/decide', query: { taskId: row.id, documentId: row.documentId } })">裁定</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-card v-else shadow="never" v-loading="loading">
    <template #header>
      <div class="head">
        <span class="title">裁定冲突</span>
        <span class="stat">已裁定 <b class="green">{{ data.decidedCount }}</b>　未裁定 <b class="red">{{ data.undecidedCount }}</b></span>
      </div>
    </template>
    <el-table :data="data.conflicts" empty-text="暂无待裁定冲突">
      <el-table-column prop="differenceType" label="差异类型" width="110" />
      <el-table-column prop="differenceObject" label="差异对象" width="90" />
      <el-table-column prop="valueA" label="甲的值" width="160" />
      <el-table-column prop="valueB" label="乙的值" width="160" />
      <el-table-column prop="note" label="判定说明" min-width="200" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button type="primary" size="small" :disabled="row.decided" @click="openDecide(row)">{{ row.decided ? '已裁定' : '裁定' }}</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="decideVisible" title="裁定冲突" width="520px">
    <template v-if="current">
      <p><b>差异对象：</b>{{ current.differenceObject }}</p>
      <p><b>甲的值：</b>{{ current.valueA }}</p>
      <p><b>乙的值：</b>{{ current.valueB }}</p>
      <el-form label-width="90px">
        <el-form-item label="裁定方式">
          <el-radio-group v-model="method">
            <el-radio value="认可甲">认可甲</el-radio>
            <el-radio value="认可乙">认可乙</el-radio>
            <el-radio value="重新标注">重新标注</el-radio>
            <el-radio value="提交专家">提交领域专家</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
    </template>
    <template #footer>
      <el-button @click="decideVisible = false">取消</el-button>
      <el-button type="primary" @click="onDecide">确认裁定</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.title { font-weight: 600; }
.head { display: flex; justify-content: space-between; align-items: center; }
.stat { color: #606266; }
.green { color: #67c23a; }
.red { color: #f56c6c; }
</style>
