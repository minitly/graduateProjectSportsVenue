import axios from 'axios'
import { useAuthStore } from '../stores/auth'

const api = axios.create({
  baseURL: 'http://localhost:9999/sportsVenue',
  timeout: 15000
})

api.interceptors.request.use(
  (config) => {
    const auth = useAuthStore()
    if (auth.token) {
      config.headers.Authorization = `Bearer ${auth.token}`
    }
    console.log('[API Request]', config.method?.toUpperCase(), config.url, {
      params: config.params,
      data: config.data
    })
    return config
  },
  (error) => {
    console.error('[API Request Error]', error)
    return Promise.reject(error)
  }
)

api.interceptors.response.use(
  (response) => {
    console.log('[API Response]', response.config?.url, response.data)
    return response.data
  },
  (error) => {
    console.error('[API Response Error]', error)
    return Promise.reject(error)
  }
)

export default api
