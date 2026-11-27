<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { documentApi } from '@/api'

const loading = ref(false)
const docs = ref([])

const uploadVisible = ref(false)
const uploadForm = reactive({ title: '', caseNo: '', type: '原始判决书' })
const uploading = ref(false)

async function load() {
  loading.value = true
  try {
    const r = await documentApi.list()
    docs.value = r.records || []
  } catch (e) { /* 拦截器已提示 */ } finally { loading.value = false }
}

function openUpload() {
  Object.assign(uploadForm, { title: '', caseNo: '', type: '原始判决书' })
  uploadVisible.value = true
}

async function onUpload() {
  if (!uploadForm.title) { ElMessage.warning('请填写文书标题'); return }
  uploading.value = true
  try {
    await documentApi.upload(uploadForm)
    ElMessage.success('文书已保存')
    uploadVisible.value = false
    load()
  } catch (e) { /* 拦截器已提示 */ } finally { uploading.value = false }
}

onMounted(load)
</script>

<template>
  <el-card shadow="never" v-loading="loading">
    <template #header>
      <div class="head">
        <span class="title">文书列表</span>
        <el-button type="primary" @click="openUpload">上传文书</el-button>
      </div>
    </template>
    <el-table :data="docs" empty-text="暂无文书">
      <el-table-column prop="title" label="文书标题" min-width="260" />
      <el-table-column prop="caseNo" label="案号 / 名称" min-width="180" />
      <el-table-column prop="type" label="文书类型" width="120" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link type="primary" size="small">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="uploadVisible" title="上传文书" width="520px">
    <el-form label-width="90px">
      <el-form-item label="文书类型">
        <el-radio-group v-model="uploadForm.type">
          <el-radio-button value="原始判决书">原始判决书</el-radio-button>
          <el-radio-button value="指导性案例">指导性案例</el-radio-button>
          <el-radio-button value="参考案例">参考案例</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="文书标题"><el-input v-model="uploadForm.title" placeholder="如：余某、徐某劳务合同纠纷民事一审民事判决书" /></el-form-item>
      <el-form-item label="案号"><el-input v-model="uploadForm.caseNo" placeholder="如：(2023)苏0106民初18909号" /></el-form-item>
      <el-form-item label="文书文件">
        <el-upload drag action="#" :auto-upload="false" :limit="1">
          <el-icon><UploadFilled /></el-icon>
          <div class="el-upload__text">将文书文件拖到此处，或<em>点击上传</em></div>
          <template #tip><div class="el-upload__tip">支持 Word / PDF / 纯文本（演示环境不上传真实文件）</div></template>
        </el-upload>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="uploadVisible = false">取消</el-button>
      <el-button type="primary" :loading="uploading" @click="onUpload">保存文书</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.head { display: flex; justify-content: space-between; align-items: center; }
.title { font-weight: 600; }
</style>
