<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/store/auth'
import { authApi } from '@/api'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const form = reactive({ account: '', password: '' })
const loading = ref(false)

async function onLogin() {
  if (!form.account || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const data = await auth.login(form.account, form.password)
    if (data.single) {
      router.push(route.query.redirect || '/')
    } else {
      router.push('/select')
    }
  } catch (e) {
    // 错误信息已由拦截器提示
  } finally {
    loading.value = false
  }
}

const applyVisible = ref(false)
const applyForm = reactive({ name: '', unit: '', system: '', identity: '', note: '' })
const applying = ref(false)
async function onSubmitApply() {
  applying.value = true
  try {
    await authApi.apply(applyForm)
    ElMessage.success('申请已提交，等待审批')
    applyVisible.value = false
  } catch (e) {
    // 拦截器已提示
  } finally {
    applying.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-card">
      <div class="title">法律论证标注与教学支持系统</div>
      <div class="subtitle">当前指南版本 v1.2</div>
      <el-form label-position="top" @keyup.enter="onLogin">
        <el-form-item label="账号">
          <el-input v-model="form.account" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-button type="primary" class="w-full" :loading="loading" @click="onLogin">登录</el-button>
      </el-form>
      <div class="foot">
        <el-link type="primary" :underline="false" @click="applyVisible = true">申请账号</el-link>
        <span class="hint">演示账号：admin / liu / qian / wang 等，密码均为 123456</span>
      </div>
    </el-card>

    <el-dialog v-model="applyVisible" title="申请账号" width="460px">
      <el-form label-width="90px">
        <el-form-item label="姓名"><el-input v-model="applyForm.name" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="applyForm.unit" /></el-form-item>
        <el-form-item label="申请身份">
          <el-input v-model="applyForm.identity" placeholder="如：标注员 / 教师 / 学生" />
        </el-form-item>
        <el-form-item label="说明"><el-input v-model="applyForm.note" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" :loading="applying" @click="onSubmitApply">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.login-page { height: 100%; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #1f2d3d 0%, #409eff 100%); }
.login-card { width: 400px; padding: 12px 8px; }
.title { font-size: 18px; font-weight: 700; text-align: center; }
.subtitle { text-align: center; color: #909399; font-size: 13px; margin: 6px 0 18px; }
.w-full { width: 100%; }
.foot { margin-top: 14px; display: flex; justify-content: space-between; align-items: center; }
.hint { font-size: 12px; color: #c0c4cc; }
</style>
