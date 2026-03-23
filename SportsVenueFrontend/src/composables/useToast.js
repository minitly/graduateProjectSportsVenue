import { ref } from 'vue'

const toasts = ref([])

const pushToast = (message, type = 'success', duration = 2400) => {
  const id = Date.now() + Math.random()
  toasts.value = [...toasts.value, { id, message, type }]
  setTimeout(() => {
    toasts.value = toasts.value.filter((toast) => toast.id !== id)
  }, duration)
}

export const useToast = () => ({
  toasts,
  pushToast
})
