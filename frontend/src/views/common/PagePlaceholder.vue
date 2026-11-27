<script setup>
import { useRoute } from 'vue-router'
const route = useRoute()
</script>

<template>
  <div class="page-card" style="min-height: 60vh; display: flex; align-items: center; justify-content: center;">
    <el-result icon="info" :title="'「' + (route.meta.title || '该模块') + '」建设中'"
               sub-title="功能与页面结构已预留，正在按功能分解清单逐模块实现" />
  </div>
</template>
