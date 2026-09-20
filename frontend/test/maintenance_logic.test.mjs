// 在 Node 中通过 Vite SSR 加载 Maintenance.vue 的 setup()，
// 对禁选/已登记/校验/提交失败回显等前端决策逻辑做真实执行验证。
import { JSDOM } from 'jsdom'
import { createServer } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath } from 'node:url'
import { writeFileSync } from 'node:fs'

const stubPath = fileURLToPath(new URL('./el-message-stub.mjs', import.meta.url))
writeFileSync(stubPath, 'const ElMessage = { error(){}, success(){}, warning(){} };\nexport { ElMessage };\nexport default { install(){} };\n')

const dom = new JSDOM('<!doctype html><html><body><div id="app"></div></body></html>', {
  url: 'http://localhost/'
})
globalThis.window = dom.window
globalThis.document = dom.window.document
globalThis.navigator = dom.window.navigator
globalThis.HTMLElement = dom.window.HTMLElement
globalThis.getComputedStyle = dom.window.getComputedStyle

let pass = 0, fail = 0
const ok = (m) => { console.log('PASS:', m); pass++ }
const bad = (m) => { console.log('FAIL:', m); fail++ }
const assert = (cond, m) => cond ? ok(m) : bad(m)

const vite = await createServer({
  configFile: false,
  root: '/workspace/claude-qd-420/frontend',
  plugins: [
    vue()
  ],
  resolve: {
    alias: { 'element-plus': stubPath }
  },
  server: { middlewareMode: true },
  logLevel: 'error'
})

const httpMod = await vite.ssrLoadModule('/src/api/index.js')
const http = httpMod.default
http.defaults.baseURL = 'http://127.0.0.1:18080/api'

const comp = await vite.ssrLoadModule('/src/views/Maintenance.vue')
// 不整棵渲染（模板依赖 Element Plus 指令）；改为在 app 上下文里直接执行 setup，
// 并手工提供 SFC 需要的 setupContext 与 SSR 上下文。
const { createSSRApp, getCurrentInstance } = await import('vue')
const { renderToString } = await import('vue/server-renderer')
let b
await new Promise((resolve) => {
  const app = createSSRApp({
    setup() {
      const inst = getCurrentInstance()
      inst.appContext.app
      b = comp.default.setup({}, { expose: () => {}, slots: {}, attrs: {}, emit: () => {} })
      resolve()
      return () => null
    }
  })
  renderToString(app, {}).catch(() => {})
})
assert(b && b.rows, '组件 setup 绑定可取回')

// 等待 setup 顶层 load() 完成
for (let i = 0; i < 50 && (!b.rows.value.length || !b.tanks.value.length); i++) {
  await new Promise(r => setTimeout(r, 100))
}
assert(b.rows.value.length >= 4, `巡检历史数据已加载（${b.rows.value.length} 条）`)
assert(b.tanks.value.length === 4, '展缸列表已加载（4 口）')

// 展缸 1 有 2026-09-15 / 09-16 的种子记录
b.form.value.tankId = 1
assert(b.registeredDatesOfTank.value.includes('2026-09-15'), '已登记日期集合含 09-15')
assert(b.registeredDatesOfTank.value.includes('2026-09-16'), '已登记日期集合含 09-16')

// 未来日期禁选
const future = new Date('2099-01-01T00:00:00')
const past = new Date('2020-01-01T00:00:00')
assert(b.isDateDisabled(future) === true, '未来日期被禁选')
assert(b.isDateDisabled(past) === false, '过去日期可选')

// 已登记日期禁选，同日期在别的展缸上不禁选
assert(b.isDateDisabled(new Date('2026-09-15T00:00:00')) === true, '该展缸已登记日期禁选')
b.form.value.tankId = 2
assert(b.isDateDisabled(new Date('2026-09-15T00:00:00')) === false, '同一日期其他展缸仍可选')

// 选中已登记组合 -> 冲突态可见且提交按钮禁用
b.form.value.tankId = 1
b.form.value.checkDate = '2026-09-15'
assert(b.selectedRegistered.value === true, '选中已登记组合时识别为重复登记')

// 异常 + 空备注：save 必须在对话框内提示且保留内容（用一个未登记日期避免与重复校验互相干扰）
b.vis.value = true
b.form.value.tankId = 2
b.form.value.checkDate = '2026-08-11'
b.form.value.result = '异常'
b.form.value.note = '   '
b.inlineError.value = ''
await b.save()
assert(b.inlineError.value.includes('异常巡检必须填写非空备注'), `异常空备注有明确内联提示: "${b.inlineError.value}"`)
assert(b.vis.value === true, '校验失败时对话框保持打开（内容保留）')
assert(b.form.value.note === '   ' && b.form.value.result === '异常', '已填内容被保留')
assert(b.submitting.value === false, '校验失败后提交状态复位')

// 本地未来日期拦截
b.form.value.tankId = 2
b.form.value.checkDate = '2099-01-01'
b.form.value.result = '正常'
b.inlineError.value = ''
await b.save()
assert(/未来/.test(b.inlineError.value), `前端拒绝未来日期: "${b.inlineError.value}"`)
assert(b.vis.value === true, '未来日期被拒后对话框不关闭')

// 重复登记（已被他人/历史登记）：内联冲突提示、不关窗
b.form.value.tankId = 1
b.form.value.checkDate = '2026-09-15'
b.inlineError.value = ''
await b.save()
assert(/已有巡检记录|已被其他同事登记/.test(b.inlineError.value), `重复登记给出可理解冲突提示: "${b.inlineError.value}"`)
assert(b.vis.value === true, '冲突后对话框保持打开')

// 服务端并发兜底：用一个从未登记的历史日期，正常异常+备注应成功
const uniq = '2026-08-' + String(10 + Math.floor(Math.random() * 15))
b.form.value.tankId = 2
b.form.value.checkDate = uniq
b.form.value.result = '异常'
b.form.value.note = '前端逻辑测试-异常记录'
b.inlineError.value = ''
const beforeCount = b.rows.value.length
await b.save()
assert(b.inlineError.value === '', `合法异常登记无错误提示（实际: "${b.inlineError.value}"）`)
assert(b.vis.value === false, '服务端确认成功后才关闭对话框')
assert(b.rows.value.length === beforeCount + 1, '成功后列表刷新，多了一条')
assert(b.rows.value.some(r => r.note === '前端逻辑测试-异常记录'), '新异常记录出现在列表中')

// 网络失败不能误导成成功：把 baseURL 指向不可达端口，提交应内联报错且不关窗
http.defaults.baseURL = 'http://127.0.0.1:9/api'
http.defaults.baseURL = 'http://127.0.0.1:9/api'
// 直接观察拦截器返回的错误对象（不经过组件 DOM 路径）
let probeErr
try {
  await http.get('/maintenances')
} catch (e) { probeErr = e }
assert(probeErr && probeErr.isNetworkError === true, `网络失败被识别为网络错误: ${probeErr?.message}`)
b.vis.value = true
b.form.value.tankId = 3
b.form.value.checkDate = '2026-08-29'
b.form.value.result = '正常'
b.form.value.note = ''
b.inlineError.value = ''
await b.save()
assert(/网络/.test(b.inlineError.value), `网络失败给出网络类提示: "${b.inlineError.value}"`)
assert(b.vis.value === true, '网络失败后对话框保持打开，未误报成功')

// 409 冲突走内联（由 axios 包装的 status 判定）：mock http.post reject {status:409,message}
const origPost = http.post
http.post = () => Promise.reject(Object.assign(new Error('展缸 T03 在 2026-08-29 的巡检记录刚刚已被其他同事登记，请勿重复提交'), { status: 409 }))
b.inlineError.value = ''
await b.save()
assert(b.inlineError.value.includes('已被其他同事登记'), `409 冲突信息在对话框内回显: "${b.inlineError.value}"`)
assert(b.vis.value === true, '409 后对话框保持打开，已填内容不丢')
http.post = origPost
http.defaults.baseURL = 'http://127.0.0.1:18080/api'

await vite.close()
console.log(`\n前端逻辑测试: PASS=${pass} FAIL=${fail}`)
process.exit(fail === 0 ? 0 : 1)
