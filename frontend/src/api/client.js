import axios from 'axios'

/**
 * Single axios instance used by the whole app.
 *
 * baseURL comes from VITE_API_URL, defaulting to '/api' which Vite's dev server
 * proxies to http://localhost:8080 (see vite.config.js). Using a relative URL
 * means the app works whether you open it through the proxy or deploy both
 * pieces behind one origin.
 */
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

// The JWT is kept in localStorage and attached to every request.
const TOKEN_KEY = 'nudge.t…oken'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

api.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * Any 401 from the server (expired or invalid token) clears the session and
 * sends the user back to the login page, so nobody is left staring at a
 * dashboard that silently fails.
 */
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    if (status === 401 && !error.config?.url?.includes('/auth/')) {
      setToken(null)
      window.dispatchEvent(new Event('nudge:signed-out'))
    }
    return Promise.reject(error)
  },
)

/**
 * Turns an axios error into a short human sentence for the UI.
 * Keeps field-level validation messages so the form can show them inline.
 */
export function readApiError(error) {
  const data = error?.response?.data

  if (data && typeof data === 'object') {
    const messages = []
    if (data.message) messages.push(data.message)
    if (data.errors && typeof data.errors === 'object') {
      Object.values(data.errors).forEach((msg) => messages.push(msg))
    }
    if (messages.length) return messages.join(' ')
  }

  if (error?.code === 'ERR_NETWORK') {
    return 'Cannot reach the server. Is the backend running on http://localhost:8080?'
  }

  if (error?.response) {
    return `Request failed (${error.response.status}). Please try again.`
  }

  return error?.message || 'Something went wrong.'
}

export default api
