import api, { setToken } from './client.js'

/**
 * Signup and login. The backend answers with { token, tokenType, user }.
 * Storing the token is done here so the caller never has to think about it.
 */

export async function signup({ name, email, password }) {
  const response = await api.post('/auth/signup', { name, email, password })
  setToken(response.data.token)
  return response.data.user
}

export async function login({ email, password }) {
  const response = await api.post('/auth/login', { email, password })
  setToken(response.data.token)
  return response.data.user
}

/**
 * Logging out is local-only: a JWT stays valid until it expires, so "logout"
 * means forgetting the token on this device. Documented here because it is the
 * honest answer if an interviewer asks.
 */
export function logout() {
  setToken(null)
}
