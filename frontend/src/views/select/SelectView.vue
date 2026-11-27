<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import { authApi } from '@/api'
import { SYSTEMS } from '@/utils/dict'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(true)
const data = ref({ name: '', options: [], currentSystem: null, currentIdentity: null })

const grouped = computed(() => {
  const map = {}
  for (const opt of data.value.options) {
    if (!map[opt.system]) map[opt.system] = []
    map[opt.system].push(opt)
  }
  return Object.keys(SYSTEMS).filter((s) => map[s]).map((s) => ({ system: s, name: SYSTEMS[s], list: map[s] }))
})

async function enter(system, identity) {
  await auth.select(system, identity)
  router.push('/')
}

onMounted(async () => {
  try {
    data.value = await authApi.options()
  } catch (e) {
    // 未登录等
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="select-page">
    <div class="wrap">
      <div class="head">
        <div class="hello">{{ data.name }}，请选择要进入的系统与身份</div>
        <el-button text @click="auth.logout().then(() => router.push('/login'))">退出登录</el-button>
      </div>
      <div v-loading="loading">
        <div v-for="g in grouped" :key="g.system" class="sys-block">
          <div class="sys-title">{{ g.name }}</div>
          <div class="cards">
            <div v-for="opt in g.list" :key="opt.identity" class="card"
                 :class="{ active: data.currentSystem === opt.system && data.currentIdentity === opt.identity }">
              <div class="card-title">{{ opt.identityName }}</div>
              <div class="card-desc">{{ opt.description }}</div>
              <el-button type="primary" size="small" @click="enter(opt.system, opt.identity)">进入</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.select-page { min-height: 100%; background: #f5f7fa; padding: 32px; }
.wrap { max-width: 900px; margin: 0 auto; }
.head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.hello { font-size: 20px; font-weight: 700; }
.sys-block { margin-bottom: 24px; }
.sys-title { font-size: 15px; color: #909399; margin-bottom: 10px; }
.cards { display: flex; flex-wrap: wrap; gap: 14px; }
.card { background: #fff; border: 1px solid #e4e7ed; border-radius: 8px; padding: 16px; width: 210px; display: flex; flex-direction: column; gap: 8px; }
.card.active { border-color: #409eff; box-shadow: 0 0 0 2px rgba(64,158,255,.15); }
.card-title { font-weight: 700; font-size: 15px; }
.card-desc { color: #606266; font-size: 12px; min-height: 34px; line-height: 1.5; }
</style>
