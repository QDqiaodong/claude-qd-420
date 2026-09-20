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

    <el-dialog v-model="vis" title="新增巡检" :close-on-click-modal="false" @closed="onDialogClosed">
      <el-form :model="form" label-width="80px" @submit.prevent>
        <el-alert
          v-if="inlineError"
          :title="inlineError"
          type="error"
          show-icon
          :closable="true"
          class="inline-error"
          @close="inlineError = ''"
        />
        <el-form-item label="展缸" required>
          <el-select v-model="form.tankId" placeholder="选择展缸" @change="onTankOrDateChange">
            <el-option v-for="t in tanks" :key="t.id">
              <span>{{ t.code }}</span>
              <el-tag
                v-if="tankDateKeys.has(t.id + '@' + todayKey)"
                type="info"
                size="small"
                class="opt-tag"
              >今日已登记</el-tag>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="日期" required>
          <el-date-picker
            v-model="form.checkDate"
            type="date"
            value-format="YYYY-MM-DD"
            :disabled-date="isDateDisabled"
            :cell-class-name="dateCellClass"
            placeholder="选择巡检日期"
            @change="onTankOrDateChange"
          />
          <div class="hint" v-if="form.tankId">
            <template v-if="registeredDatesOfTank.length">
              该展缸已登记日期：<span class="registered-text">{{ registeredDatesOfTank.join('、') }}</span>，灰色日期不可重复登记
            </template>
            <template v-else>该展缸暂无历史巡检登记</template>
          </div>
        </el-form-item>
        <el-form-item label="结果" required>
          <el-select v-model="form.result" @change="onResultChange">
            <el-option label="正常" value="正常" />
            <el-option label="异常" value="异常" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" :required="form.result === '异常'">
          <el-input
            v-model="form.note"
            type="textarea"
            :rows="2"
            :placeholder="form.result === '异常' ? '异常必须填写备注' : '可选'"
          />
        </el-form-item>
        <el-alert
          v-if="selectedRegistered"
          title="该展缸当天已有巡检记录，不能重复登记，请更换日期"
          type="warning"
          show-icon
          class="inline-error"
          :closable="false"
        />
      </el-form>
      <template #footer>
        <el-button @click="vis = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          :disabled="selectedRegistered"
          @click="save"
        >提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import http from '../api'

const rows = ref([])
const tanks = ref([])
const vis = ref(false)
const submitting = ref(false)
const inlineError = ref('')
const form = ref(emptyForm())

function emptyForm() {
  return { tankId: null, checkDate: todayLocalKey(), result: '正常', note: '' }
}

// 仅用于前端禁选/展示；真正的“今天”以后端服务端时区判断为准
function pad(n) { return String(n).padStart(2, '0') }
function dateToKey(d) { return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) }
function todayLocalKey() { return dateToKey(new Date()) }
const todayKey = computed(() => todayLocalKey())

// 已登记的 展缸@日期 集合（含历史重复记录，全部展示出来）
const tankDateKeys = computed(() => {
  const set = new Set()
  for (const r of rows.value) {
    if (r.tankId != null && r.checkDate) set.add(r.tankId + '@' + r.checkDate)
  }
  return set
})

const registeredDatesOfTank = computed(() => {
  if (!form.value.tankId) return []
  const prefix = form.value.tankId + '@'
  return [...tankDateKeys.value]
    .filter(k => k.startsWith(prefix))
    .map(k => k.slice(prefix.length))
    .sort()
})

const selectedRegistered = computed(() =>
  !!(form.value.tankId && form.value.checkDate
    && tankDateKeys.value.has(form.value.tankId + '@' + form.value.checkDate))
)

function isDateDisabled(date) {
  // 未来日期禁选（按本地日历比较；接口端会再次按服务端时区校验）
  const key = dateToKey(date)
  if (key > todayKey.value) return true
  if (form.value.tankId
      && tankDateKeys.value.has(form.value.tankId + '@' + key)) return true
  return false
}

function dateCellClass(date) {
  if (!form.value.tankId) return ''
  return tankDateKeys.value.has(form.value.tankId + '@' + dateToKey(date))
    ? 'registered-day'
    : ''
}

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
function openCreate() {
  form.value = emptyForm()
  inlineError.value = ''
  vis.value = true
}
function onDialogClosed() {
  inlineError.value = ''
}
function onTankOrDateChange() {
  inlineError.value = ''
}
function onResultChange() {
  inlineError.value = ''
}

function validateLocally() {
  if (!form.value.tankId) return '请选择展缸'
  if (!form.value.checkDate) return '请选择巡检日期'
  if (form.value.checkDate > todayKey.value) return '巡检日期不能晚于今天，不允许登记未来日期'
  if (selectedRegistered.value) return '该展缸当天已有巡检记录，不能重复登记'
  if (!form.value.result) return '请选择巡检结果'
  if (form.value.result === '异常'
      && (!form.value.note || !form.value.note.trim())) {
    return '异常巡检必须填写非空备注，请说明异常情况'
  }
  return ''
}

function friendlyError(e) {
  const raw = e?.message || ''
  if (/network|timeout|failed to fetch|网络/i.test(raw)) {
    return '网络异常，提交未完成，记录没有保存，请检查网络后重试'
  }
  if (e?.status === 409) {
    return raw || '该展缸当天已被其他同事登记，请勿重复提交'
  }
  return raw || '提交失败，记录没有保存，请稍后重试'
}

async function save() {
  inlineError.value = ''
  const problem = validateLocally()
  if (problem) {
    inlineError.value = problem
    return
  }
  submitting.value = true
  try {
    await http.post('/maintenances', {
      tankId: form.value.tankId,
      checkDate: form.value.checkDate,
      result: form.value.result,
      note: form.value.note ? form.value.note.trim() : form.value.note
    })
    // 只有服务端确认成功后才关闭弹窗并刷新，避免把失败误报成成功
    vis.value = false
    await load()
  } catch (e) {
    inlineError.value = friendlyError(e)
    // 冲突后刷新已登记状态（可能是另一名饲养员刚刚登记成功）
    if (e?.status === 409) {
      try { await load() } catch (_) { /* 保留原始错误提示 */ }
    }
  } finally {
    submitting.value = false
  }
}
load()
</script>

<style scoped>
.bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.bar h3 { margin: 0; color: #00897b; }
.tbl { background: #fff; border-radius: 12px; }
:deep(.abnormal-row) { background: #fef0f0 !important; }
:deep(.abnormal-row:hover > td) { background: #fde2e2 !important; }
.inline-error { margin-bottom: 14px; }
.hint { font-size: 12px; color: #909399; line-height: 1.6; margin-top: 4px; }
.registered-text { color: #e6a23c; }
.opt-tag { margin-left: 8px; }
:deep(.el-date-editor .registered-day .el-date-table-cell__text) {
  text-decoration: underline;
  text-decoration-color: #e6a23c;
  text-underline-offset: 3px;
  color: #e6a23c;
  font-weight: 600;
}
</style>
