<template>
  <div>
    <div class="bar">
      <h3>生物档案</h3>
      <el-button type="primary" @click="openCreate">+ 入缸生物</el-button>
    </div>
    <div class="wall">
      <div class="cell" v-for="c in creatures" :key="c.id">
        <div class="avatar" :style="{ background: avatarColor(c) }">
          {{ (c.name || '?').charAt(0) }}
          <span class="dot" :style="{ background: c.status === '在展' ? '#00897b' : '#c0c4cc' }"></span>
        </div>
        <div class="name">{{ c.name }}</div>
        <div class="sub">{{ c.species }} · {{ tankCode(c.tankId) }}</div>
        <div class="ops">
          <el-button v-if="c.status === '在展'" size="small" link type="danger" @click="setStatus(c.id, '下架')">下架</el-button>
          <el-button v-else size="small" link type="success" @click="setStatus(c.id, '在展')">在展</el-button>
          <el-button size="small" link type="info" @click="remove(c.id)">移除</el-button>
        </div>
      </div>
    </div>

    <el-dialog v-model="vis" title="入缸生物">
      <el-form :model="form" label-width="80px">
        <el-form-item label="归属展缸">
          <el-select v-model="form.tankId" placeholder="选择展缸">
            <el-option v-for="t in tanks" :key="t.id" :label="t.code + '（' + t.state + '）'" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="品种"><el-input v-model="form.species" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="vis = false">取消</el-button>
        <el-button type="primary" @click="save">入缸</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import http from '../api'

const creatures = ref([])
const tanks = ref([])
const vis = ref(false)
const form = ref({})

const palette = ['#00897b', '#26a69a', '#4db6ac', '#ff8a65', '#7e57c2']
function avatarColor(c) {
  const s = (c.name || '') + (c.id || 0)
  let h = 0
  for (const ch of s) h = (h * 31 + ch.charCodeAt(0)) % palette.length
  return palette[h]
}
function tankCode(id) {
  const t = tanks.value.find(x => x.id === id)
  return t ? t.code : id
}
async function load() {
  const [cs, ts] = await Promise.all([http.get('/creatures'), http.get('/tanks')])
  creatures.value = cs
  tanks.value = ts
}
function openCreate() { form.value = { tankId: '' }; vis.value = true }
async function setStatus(id, status) { await http.put('/creatures/' + id + '/status', { status }); await load() }
async function remove(id) { await http.delete('/creatures/' + id); await load() }
async function save() {
  await http.post('/creatures', { tankId: form.value.tankId, species: form.value.species, name: form.value.name, status: '在展' })
  vis.value = false
  await load()
}
onMounted(load)
</script>

<style scoped>
.bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.bar h3 { margin: 0; color: #00897b; }
.wall { display: flex; flex-wrap: wrap; gap: 14px; }
.cell { width: 132px; border: 1px solid #e0f2f1; border-radius: 14px; padding: 14px; background: #fff; text-align: center; }
.avatar {
  position: relative; width: 56px; height: 56px; border-radius: 50%;
  margin: 0 auto 8px; color: #fff; font-size: 22px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
}
.dot { position: absolute; right: 0; bottom: 0; width: 14px; height: 14px; border-radius: 50%; border: 2px solid #fff; }
.name { font-weight: 600; color: #004d40; }
.sub { color: #909399; font-size: 12px; margin: 4px 0 8px; }
.ops { display: flex; justify-content: center; gap: 4px; }
</style>
