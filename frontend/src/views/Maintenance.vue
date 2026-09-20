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

    <el-dialog v-model="vis" title="新增巡检" @closed="onDialogClosed">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" @submit.prevent>
        <el-form-item label="日期" prop="checkDate">
          <el-date-picker
            v-model="form.checkDate"
            type="date"
            value-format="YYYY-MM-DD"
            :disabled-date="disableFutureDate"
            :clearable="false"
            placeholder="选择巡检日期（不可选未来日期）"
            style="width: 100%" />
        </el-form-item>
        <el-form-item label="展缸" prop="tankId">
          <el-select v-model="form.tankId" placeholder="选择展缸" style="width: 100%">
            <el-option
              v-for="t in tanks"
              :key="t.id"
              :label="registeredTankIds.has(t.id) ? t.code + '（当日已登记）' : t.code"
              :value="t.id"
              :disabled="registeredTankIds.has(t.id)" />
          </el-select>
          <div v-if="form.checkDate && registeredTankIds.size" class="hint">
            标为「当日已登记」的展缸在 {{ form.checkDate }} 已有巡检记录，不能重复登记
          </div>
        </el-form-item>
        <el-form-item label="结果" prop="result">
          <el-select v-model="form.result" style="width: 100%">
            <el-option label="正常" value="正常" />
            <el-option label="异常" value="异常" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="note">
          <el-input
            v-model="form.note"
            type="textarea"
            :rows="3"
            :placeholder="form.result === '异常' ? '异常巡检必须填写非空备注' : '可选'" />
        </el-form-item>
        <el-alert
          v-if="submitError"
          :title="submitError"
          type="error"
          show-icon
          :closable="false"
          class="submit-error" />
      </el-form>
      <template #footer>
        <el-button @click="vis = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="save">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import http from '../api'

const rows = ref([])
const tanks = ref([])
const vis = ref(false)
const submitting = ref(false)
const submitError = ref('')
const formRef = ref()
const form = ref(emptyForm())

function emptyForm() {
  return { checkDate: todayStr(), tankId: null, result: '正常', note: '' }
}
function todayStr() {
  const d = new Date()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}
// 页面端拦截未来日期；最终是否为未来仍由服务端按时区判定
function disableFutureDate(d) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return d.getTime() > today.getTime()
}

const registeredTankIds = computed(() => {
  const ids = new Set()
  if (!form.value.checkDate) return ids
  for (const r of rows.value) {
    if (r.checkDate === form.value.checkDate) ids.add(r.tankId)
  }
  return ids
})

const rules = computed(() => ({
  checkDate: [{ required: true, message: '请选择巡检日期', trigger: 'change' }],
  tankId: [{ required: true, message: '请选择展缸', trigger: 'change' }],
  result: [{ required: true, message: '请选择巡检结果', trigger: 'change' }],
  note: [{
    trigger: ['blur', 'change'],
    validator: (_rule, value, callback) => {
      if (form.value.result === '异常' && (!value || !value.trim())) {
        callback(new Error('异常巡检必须填写非空备注'))
      } else {
        callback()
      }
    }
  }]
}))

function tankCode(id) {
  const t = tanks.value.find(x => x.id === id)
  return t ? t.code : id
}
watch(() => form.value.result, (r) => {
  // 切回「正常」后清空备注上的异常红字；切到「异常」且已填内容时即时消错
  if (r === '正常' || (form.value.note || '').trim()) {
    formRef.value?.validateField?.('note').catch(() => {})
  }
})
function rowClass({ row }) {
  return row.result === '异常' ? 'abnormal-row' : ''
}
async function load() {
  const [ms, ts] = await Promise.all([http.get('/maintenances'), http.get('/tanks')])
  rows.value = ms
  tanks.value = ts
}
function openCreate() {
  form.value = emptyForm()
  submitError.value = ''
  vis.value = true
}
function onDialogClosed() {
  submitError.value = ''
  submitting.value = false
  formRef.value?.clearValidate?.()
}
async function save() {
  if (submitting.value) return
  submitError.value = ''
  try {
    await formRef.value.validate()
  } catch {
    // 未通过的字段已在对话框内红字提示，已填内容原样保留
    return
  }
  // 前端再兜一次：已登记组合不允许提交（正常情况下选项已禁用）
  if (registeredTankIds.value.has(form.value.tankId)) {
    submitError.value = `该展缸在 ${form.value.checkDate} 已登记巡检，同一展缸同一天只能登记一次`
    return
  }
  submitting.value = true
  try {
    await http.post('/maintenances', {
      tankId: form.value.tankId,
      checkDate: form.value.checkDate,
      result: form.value.result,
      note: form.value.note
    })
  } catch (e) {
    // 网络失败 / 并发 409 / 服务端校验失败：对话框保持打开、内容保留，明确提示未落账
    submitError.value = e?.message
      ? `提交未成功：${e.message}。请按提示调整后重试，已填内容已保留`
      : '提交未成功：网络异常，请稍后重试，已填内容已保留'
    submitting.value = false
    return
  }
  vis.value = false
  submitting.value = false
  await load()
}
onMounted(load)
</script>

<style scoped>
.bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.bar h3 { margin: 0; color: #00897b; }
.tbl { background: #fff; border-radius: 12px; }
.hint { color: #e6a23c; font-size: 12px; line-height: 1.4; margin-top: 4px; }
.submit-error { margin-top: 4px; }
:deep(.abnormal-row) { background: #fef0f0 !important; }
:deep(.abnormal-row:hover > td) { background: #fde2e2 !important; }
</style>
