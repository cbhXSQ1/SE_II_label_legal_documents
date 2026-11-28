<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { teachingAdminApi } from '@/api'

const importing = ref(false)
const result = ref(null)

async function onImport() {
  importing.value = true
  try {
    result.value = await teachingAdminApi.import({ fileId: 'mock-file-id' })
    ElMessage.success('导入完成')
  } catch (e) { /* ignore */ } finally { importing.value = false }
}
</script>

<template>
  <el-card shadow="never">
    <template #header><span class="title">导入班级和学生名单</span></template>
    <el-alert type="info" :closable="false" show-icon title="请下载 Excel 模板，填写班级编号、班级名称、学期、负责教师工号、学生学号、学生姓名后上传。" class="mb" />
    <el-upload drag action="#" :auto-upload="false" :limit="1" class="mb">
      <el-icon><UploadFilled /></el-icon>
      <div class="el-upload__text">将填好的 Excel 拖到此处，或<em>点击上传</em></div>
    </el-upload>
    <el-button type="primary" :loading="importing" @click="onImport">开始导入</el-button>

    <template v-if="result">
      <el-divider content-position="left">导入结果</el-divider>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="班级数">{{ result.classCount }}</el-descriptions-item>
        <el-descriptions-item label="学生数">{{ result.studentCount }}</el-descriptions-item>
        <el-descriptions-item label="成功班级数">1</el-descriptions-item>
        <el-descriptions-item label="成功学生数">2</el-descriptions-item>
        <el-descriptions-item label="失败行">0</el-descriptions-item>
      </el-descriptions>
    </template>
  </el-card>
</template>

<style scoped>.title { font-weight: 600; } .mb { margin-bottom: 16px; }</style>
