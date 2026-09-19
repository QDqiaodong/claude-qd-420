<template>
  <div>
    <div class="bar">
      <h3>投喂记录</h3>
      <el-button type="primary" @click="openCreate">+ 登记投喂</el-button>
    </div>
    <div class="timeline">
      <el-timeline>
        <el-timeline-item
          v-for="(grp, idx) in groups"
          :key="idx"
          :timestamp="grp.date"
          placement="top"
          type="primary"
          :hollow="idx !== 0">
          <div class="feed-card" v-for="f in grp.items" :key="f.id">
            <el-tag size="small" effect="dark">{{ tankCode(f.tankId) }}</el-tag>
            <span class="food">{{ f.food }}</span>
            <span class="qty">{{ f.qty }} g</span>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-if="!groups.length" description="暂无投喂记录" />
    </div>

    <el-dialog v-model="vis" title="登记投喂">
      <el-form :model="form" label-width="80px">
        <el-form-item label="展缸">
          <el-select v-model="form.tankId" placeholder="选择展缸">
            <el-option v-for="t in tanks" :key="t.id" :label="t.code + '（' + t.state + '）'" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期"><el-date-picker v-model="form.feedDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="饲料"><el-input v-model="form.food" /></el-form-item>
        <el-form-item label="数量(g)"><el-input-number v-model="form.qty" :min="1" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="vis = false">取消</el-button>
        <el-button type="primary" @click="save">登记</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import http from '../api'

const feedings = ref([])
const tanks = ref([])
const vis = ref(false)
const form = ref({})

function tankCode(id) {
  const t = tanks.value.find(x => x.id === id)
  return t ? t.code : id
}
const groups = computed(() => {
  const map = {}
  for (const f of feedings.value) {
    (map[f.feedDate] = map[f.feedDate] || []).push(f)
  }
  return Object.keys(map).sort((a, b) => b.localeCompare(a))
    .map(date => ({ date, items: map[date] }))
})
async function load() {
  const [fs, ts] = await Promise.all([http.get('/feedings'), http.get('/tanks')])
  feedings.value = fs
  tanks.value = ts
}
function openCreate() { form.value = { qty: 10 }; vis.value = true }
async function save() {
  await http.post('/feedings', { tankId: form.value.tankId, feedDate: form.value.feedDate, food: form.value.food, qty: form.value.qty })
  vis.value = false
  await load()
}
onMounted(load)
</script>

<style scoped>
.bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.bar h3 { margin: 0; color: #00897b; }
.timeline { background: #fff; border: 1px solid #e0f2f1; border-radius: 14px; padding: 20px 24px; }
.feed-card { display: flex; align-items: center; gap: 10px; padding: 6px 0; }
.food { font-weight: 600; color: #004d40; }
.qty { color: #00897b; font-weight: 700; }
</style>
