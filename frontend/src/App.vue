<template>
  <el-container class="app">
    <el-aside width="158px" class="side">
      <div class="brand"><span class="logo">🐠</span> 水族馆</div>
      <div class="nav">
        <div class="icons">
          <div v-for="m in modules" :key="'i-'+m.path"
               class="icon-cell" :class="{ active: active === m.path }" @click="go(m.path)">
            {{ m.icon }}
          </div>
        </div>
        <div class="labels">
          <div v-for="m in modules" :key="'l-'+m.path"
               class="label-cell" :class="{ active: active === m.path }" @click="go(m.path)">
            {{ m.label }}
          </div>
        </div>
      </div>
    </el-aside>
    <el-main class="main"><router-view /></el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const modules = [
  { path: '/tanks', label: '展缸', icon: '🐠' },
  { path: '/creatures', label: '生物', icon: '🐟' },
  { path: '/feedings', label: '投喂', icon: '🍤' },
  { path: '/maintenances', label: '维生巡检', icon: '🔧' }
]
const route = useRoute()
const router = useRouter()
const active = computed(() => route.path)
function go(p) { router.push(p) }
</script>

<style>
html, body, #app { margin: 0; height: 100%; }
.app { height: 100vh; }
.side {
  background: var(--el-color-primary);
  color: #fff;
  display: flex;
  flex-direction: column;
}
.brand {
  font-weight: 700;
  font-size: 17px;
  padding: 16px 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}
.logo { font-size: 20px; }
.nav {
  flex: 1;
  display: flex;
  padding: 8px 0;
}
.icons, .labels {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.icons { padding-left: 14px; }
.labels { flex: 1; padding-right: 12px; }
.icon-cell, .label-cell {
  height: 56px;
  display: flex;
  align-items: center;
  border-radius: 8px;
  cursor: pointer;
  transition: background .15s;
}
.icon-cell {
  width: 44px;
  justify-content: center;
  font-size: 24px;
}
.label-cell {
  padding-left: 10px;
  font-size: 15px;
  font-weight: 600;
}
.icon-cell:hover, .label-cell:hover { background: rgba(255,255,255,0.15); }
.icon-cell.active, .label-cell.active {
  background: #fff;
  color: var(--el-color-primary);
}
.main { background: #f5f7fa; padding: 20px; }
</style>
