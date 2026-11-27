<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { navFor } from '@/utils/nav'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const nav = computed(() => navFor(auth.identity))
const activePath = computed(() => (route.path === '/' ? '/' : route.path))

const ICONS = {
  账号申请: 'UserFilled', 账号列表: 'User', 标注指南: 'Document', 总体情况: 'DataAnalysis',
  任务总览: 'DataBoard', 文书列表: 'Files', 任务列表: 'List', 任务进度: 'TrendCharts', 导出: 'Download',
  我的任务: 'List', 标注工作台: 'EditPen', 查看并比对: 'View', 裁定冲突: 'ScaleToOriginal',
  图示审查: 'Picture', 最终版本: 'CircleCheck', 待回复问题: 'QuestionFilled',
  上传文书: 'Upload', 知识点: 'Collection', 学习材料: 'Reading', 练习任务: 'Tickets',
  班级学习情况: 'PieChart', 针对性练习: 'Aim', 教师反馈: 'ChatDotRound',
  学习材料: 'Reading', 练习任务: 'Tickets', 作答记录: 'DocumentChecked', 学习情况: 'PieChart',
  导入班级名单: 'UploadFilled', 班级管理: 'School', 账号管理: 'Management'
}
function iconFor(item) {
  return ICONS[item.title] || 'Document'
}

function onReSelect() {
  router.push('/select')
}
async function onLogout() {
  await auth.logout()
  router.push('/login')
}
</script>

<template>
  <el-container class="h-100">
    <el-aside width="232px" class="aside">
      <div class="logo">法律论证标注<br />与教学支持系统</div>
      <el-menu :default-active="activePath" router class="menu">
        <el-menu-item v-for="item in nav" :key="item.path" :index="item.path">
          <el-icon><component :is="iconFor(item)" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="sys-name">
          <el-tag size="small" type="primary">{{ auth.systemName }}</el-tag>
          <span class="id-name">{{ auth.identityName }}</span>
        </div>
        <el-dropdown trigger="click">
          <span class="user-name">
            <el-icon><UserFilled /></el-icon>{{ auth.name }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="onReSelect">重新选择身份</el-dropdown-item>
              <el-dropdown-item divided @click="onLogout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.aside { background: #1f2d3d; color: #fff; }
.logo { color: #fff; font-size: 15px; font-weight: 700; padding: 18px 16px; line-height: 1.4; border-bottom: 1px solid rgba(255,255,255,.08); }
.menu { border-right: none; background: transparent; }
.menu :deep(.el-menu-item) { color: #c0c4cc; }
.menu :deep(.el-menu-item.is-active) { color: #fff; background: #409eff; }
.menu :deep(.el-menu-item:hover) { background: #2b3a4d; color: #fff; }
.header { display: flex; align-items: center; justify-content: space-between; background: #fff; border-bottom: 1px solid #e4e7ed; }
.sys-name { display: flex; align-items: center; gap: 8px; font-size: 14px; }
.id-name { font-weight: 600; }
.user-name { display: inline-flex; align-items: center; gap: 4px; cursor: pointer; color: #303133; }
.main { background: #f5f7fa; padding: 16px; overflow: auto; }
</style>
