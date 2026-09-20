import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({ baseURL: '/api', timeout: 15000 })

http.interceptors.response.use(
  (res) => res.data,
  (err) => {
    const status = err.response?.status
    const serverMessage = err.response?.data?.message
    const message = serverMessage
      || (status ? `请求失败（HTTP ${status}）` : '网络异常，请求未完成，请检查网络后重试')
    // 调用方在页面内做针对性回显（如 409 冲突）；同时保留全局提示兜底
    ElMessage.error(message)
    const wrapped = new Error(message)
    wrapped.status = status
    wrapped.serverMessage = serverMessage
    wrapped.isNetworkError = !status
    return Promise.reject(wrapped)
  }
)

export default http
