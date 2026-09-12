import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { getToken } from '../api/client.js'
import * as authApi from '../api/auth.js'

const AuthContext = createContext(null)

const USER_KEY = '***'

function readStoredUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    // Corrupted storage should never break the app.
    return null
  }
}

/**
 * Holds the logged-in user for the whole app.
 *
 * The token itself lives in localStorage (see api/client.js); this context only
 * mirrors it as React state so components re-render when it changes.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(readStoredUser)
  const [isAuthenticated, setIsAuthenticated] = useState(Boolean(getToken()))

  useEffect(() => {
    if (user) {
      localStorage.setItem(USER_KEY, JSON.stringify(user))
    } else {
      localStorage.removeItem(USER_KEY)
    }
  }, [user])

  const applySession = useCallback((sessionUser) => {
    setUser(sessionUser)
    setIsAuthenticated(true)
  }, [])

  const clearSession = useCallback(() => {
    authApi.logout()
    setUser(null)
    setIsAuthenticated(false)
  }, [])

  // The axios interceptor fires this when the server says the token is dead.
  useEffect(() => {
    const onSignedOut = () => {
      setUser(null)
      setIsAuthenticated(false)
    }
    window.addEventListener('nudge:signed-out', onSignedOut)
    return () => window.removeEventListener('nudge:signed-out', onSignedOut)
  }, [])

  const value = useMemo(
    () => ({
      user,
      isAuthenticated,
      login: async (credentials) => applySession(await authApi.login(credentials)),
      signup: async (details) => applySession(await authApi.signup(details)),
      logout: clearSession,
    }),
    [user, isAuthenticated, applySession, clearSession],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used inside <AuthProvider>')
  }
  return context
}
