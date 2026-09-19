<template>
  <div>
    <div class="bar">
      <h3>维生巡检</h3>
      <el-button type="primary" @click="openCreate">+ 新增巡检</el-button>
    </div>
    <el-table :data="rows" border class="tbl" :row-class-name="rowClass">
      <el-table-column prop="checkDate" label="日期" width="120" />
      <el-table-column label="展缸" width="100">
        <template #default="{ row }">{{ tankCode(row.tankId) }}</template>
      </el-table-column>
      <el-table-column label="结果" width="100">
        <template #default="{ row }">
          <el-tag :type="row.result === '异常' ? 'danger' : 'success'" size="small">{{ row.result }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="note" label="备注" />
    </el-table>

    <el-dialog v-model="vis" title="新增巡检">
      <el-form :model="form" label-width="80px">
        <el-form-item label="展缸">
          <el-select v-model="form.tankId" placeholder="选择展缸">
            <el-option v-for="t in tanks" :key="t.id" :label="t.code" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期"><el-date-picker v-model="form.checkDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="结果">
          <el-select v-model="form.result">
            <el-option label="正常" value="正常" />
            <el-option label="异常" value="异常" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.note" :placeholder="form.result === '异常' ? '异常必须填写备注' : '可选'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="vis = false">取消</el-button>
        <el-button type="primary" @click="save">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import http from '../api'

const rows = ref([])
const tanks = ref([])
const vis = ref(false)
const form = ref({ result: '正常' })

function tankCode(id) {
  const t = tanks.value.find(x => x.id === id)
  return t ? t.code : id
}
function rowClass({ row }) {
  return row.result === '异常' ? 'abnormal-row' : ''
}
async function load() {
  const [ms, ts] = await Promise.all([http.get('/maintenances'), http.get('/tanks')])
  rows.value = ms
  tanks.value = ts
}
function openCreate() { form.value = { result: '正常' }; vis.value = true }
async function save() {
  if (form.value.result === '异常' && (!form.value.note || !form.value.note.trim())) return
  await http.post('/maintenances', {
    tankId: form.value.tankId, checkDate: form.value.checkDate,
    result: form.value.result, note: form.value.note
  })
  vis.value = false
  await load()
}
onMounted(load)
</script>

<style scoped>
.bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.bar h3 { margin: 0; color: #00897b; }
.tbl { background: #fff; border-radius: 12px; }
:deep(.abnormal-row) { background: #fef0f0 !important; }
:deep(.abnormal-row:hover > td) { background: #fde2e2 !important; }
</style>
