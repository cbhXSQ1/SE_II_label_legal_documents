<script setup>
import { ref, onMounted } from 'vue'
import { teachingAdminApi } from '@/api'

const loading = ref(false)
const list = ref([])

async function load() {
  loading.value = true
  try { list.value = (await teachingAdminApi.classes()).records || [] } catch (e) { /* ignore */ } finally { loading.value = false }
}

onMounted(load)
</script>

<template>
  <el-card shadow="never" v-loading="loading">
    <template #header><span class="title">班级管理</span></template>
    <el-table :data="list" empty-text="暂无班级">
      <el-table-column prop="code" label="班级编号" width="140" />
      <el-table-column prop="name" label="班级名称" min-width="160" />
      <el-table-column prop="term" label="学期" width="140" />
      <el-table-column prop="teacher" label="负责教师" width="110" />
      <el-table-column prop="studentCount" label="学生人数" width="100" />
    </el-table>
  </el-card>
</template>

<style scoped>.title { font-weight: 600; }</style>
