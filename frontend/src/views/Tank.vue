<template>
  <div>
    <div class="bar">
      <h3>展缸台账</h3>
      <el-button type="primary" @click="openCreate">+ 新增展缸</el-button>
    </div>
    <div class="cards">
      <div class="card" v-for="t in tanks" :key="t.id">
        <div class="head">
          <span class="code">{{ t.code }}</span>
          <el-tag :color="stateMeta(t.state).color" effect="dark" size="small" class="statetag">{{ t.state }}</el-tag>
        </div>
        <div class="metrics">
          <div class="metric">
            <div class="num" :class="{ over: t.creatureCount > t.capacity }">{{ t.creatureCount }}</div>
            <div class="cap">/ {{ t.capacity }} 生物</div>
          </div>
          <el-progress type="circle" :width="62" :percentage="pct(t)" :color="stateMeta(t.state).color" />
        </div>
        <div class="actions">
          <el-button v-if="t.state === '正常'" size="small" @click="act(t.id, 'maintenance')">进入维护</el-button>
          <el-button v-if="t.state === '维护'" size="small" type="success" @click="act(t.id, 'finish')">结束维护</el-button>
          <el-button v-if="t.state !== '停用'" size="small" type="danger" @click="act(t.id, 'disable')">停用</el-button>
          <el-button size="small" link @click="openCap(t)">改容量</el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="vis" :title="form.id ? '修改容量' : '新增展缸'">
      <el-form :model="form" label-width="80px">
        <template v-if="!form.id">
          <el-form-item label="编号"><el-input v-model="form.code" /></el-form-item>
          <el-form-item label="容量"><el-input-number v-model="form.capacity" :min="1" /></el-form-item>
        </template>
        <el-form-item v-else label="容量"><el-input-number v-model="form.capacity" :min="1" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="vis = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import http from '../api'

const tanks = ref([])
const vis = ref(false)
const form = ref({})

function stateMeta(s) {
  if (s === '正常') return { color: '#00897b' }
  if (s === '维护') return { color: '#e6a23c' }
  return { color: '#909399' }
}
function pct(t) {
  if (!t.capacity) return 0
  return Math.min(100, Math.round((t.creatureCount / t.capacity) * 100))
}
async function load() { tanks.value = await http.get('/tanks') }
function openCreate() { form.value = { capacity: 4 }; vis.value = true }
function openCap(t) { form.value = { id: t.id, capacity: t.capacity }; vis.value = true }
async function act(id, op) {
  await http.put('/tanks/' + id + '/' + op)
  await load()
}
async function save() {
  if (form.value.id) await http.put('/tanks/' + form.value.id + '/capacity', { capacity: form.value.capacity })
  else await http.post('/tanks', { code: form.value.code, capacity: form.value.capacity })
  vis.value = false
  await load()
}
onMounted(load)
</script>

<style scoped>
.bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.bar h3 { margin: 0; color: #00897b; }
.cards { display: flex; flex-wrap: wrap; gap: 16px; }
.card { width: 280px; border: 1px solid #e0f2f1; border-radius: 14px; padding: 16px; background: #fff; box-shadow: 0 2px 10px rgba(0,137,123,.06); }
.head { display: flex; align-items: center; justify-content: space-between; }
.code { font-weight: 700; font-size: 16px; color: #004d40; }
.statetag { border: none; color: #fff; font-weight: 600; }
.metrics { display: flex; align-items: center; justify-content: space-between; margin: 14px 4px; }
.num { font-size: 26px; font-weight: 800; color: #00897b; }
.num.over { color: #f56c6c; }
.cap { color: #909399; font-size: 13px; }
.actions { display: flex; flex-wrap: wrap; gap: 6px; }
</style>
