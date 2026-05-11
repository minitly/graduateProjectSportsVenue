const DEFAULT_API_BASE_URL = 'http://localhost:9999/sportsVenue'

const rawApiBaseUrl = (import.meta.env.VITE_API_BASE_URL || DEFAULT_API_BASE_URL).trim()

export const API_BASE_URL = rawApiBaseUrl.replace(/\/+$/, '')

export function buildApiUrl(path) {
  const normalizedPath = String(path || '').replace(/^\/+/, '')
  return `${API_BASE_URL}/${normalizedPath}`
}
