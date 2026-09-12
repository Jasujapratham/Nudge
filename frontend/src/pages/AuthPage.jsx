import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext.jsx'
import { readApiError } from '../api/client.js'

/**
 * Login and signup share this component - the same fields, validation and
 * error handling, only the labels and the API call differ. Keeping it in one
 * file avoids two nearly identical forms drifting apart.
 */
export default function AuthPage({ mode }) {
  const isSignup = mode === 'signup'
  const { login, signup } = useAuth()
  const navigate = useNavigate()

  const [form, setForm] = useState({ name: '', email: '', password: '' })
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [busy, setBusy] = useState(false)

  const update = (field) => (event) => {
    setForm((current) => ({ ...current, [field]: event.target.value }))
    setErrors((current) => (current[field] ? { ...current, [field]: undefined } : current))
  }

  function validate() {
    const next = {}
    if (isSignup && form.name.trim().length < 2) next.name = 'Enter your name (at least 2 characters).'
    if (!/^\S+@\S+\.\S+$/.test(form.email.trim())) next.email = 'Enter a valid email address.'
    if (form.password.length < 8) next.password = 'Password must be at least 8 characters.'
    return next
  }

  async function handleSubmit(event) {
    event.preventDefault()
    const found = validate()
    setErrors(found)
    if (Object.keys(found).length > 0) return

    setBusy(true)
    setServerError('')
    try {
      const credentials = { email: form.email.trim(), password: form.password }
      if (isSignup) {
        await signup({ ...credentials, name: form.name.trim() })
      } else {
        await login(credentials)
      }
      // Landing on '/' shows the dashboard; ProtectedRoute lets us through
      // because the token is already stored.
      navigate('/', { replace: true })
    } catch (error) {
      setServerError(readApiError(error))
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="auth-shell">
      <div className="card auth-card">
        <div className="auth-brand">
          <div className="wordmark">Nudge</div>
          <p className="tagline">Small reminders. Better days.</p>
        </div>

        <h1>{isSignup ? 'Create your account' : 'Welcome back'}</h1>
        <p className="sub">
          {isSignup
            ? 'One account, all your reminders in one place.'
            : 'Log in to see your reminders and alarms.'}
        </p>

        {serverError ? (
          <div className="alert alert-error" role="alert">
            {serverError}
          </div>
        ) : null}

        <form onSubmit={handleSubmit} noValidate>
          {isSignup ? (
            <div className="field">
              <label htmlFor="name">Name</label>
              <input
                id="name"
                className={`input ${errors.name ? 'invalid' : ''}`}
                value={form.name}
                onChange={update('name')}
                placeholder="Your name"
                autoComplete="name"
              />
              {errors.name ? <span className="error-text">{errors.name}</span> : null}
            </div>
          ) : null}

          <div className="field">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              className={`input ${errors.email ? 'invalid' : ''}`}
              value={form.email}
              onChange={update('email')}
              placeholder="you@example.com"
              autoComplete="email"
              autoFocus={!isSignup}
            />
            {errors.email ? <span className="error-text">{errors.email}</span> : null}
          </div>

          <div className="field">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              className={`input ${errors.password ? 'invalid' : ''}`}
              value={form.password}
              onChange={update('password')}
              placeholder={isSignup ? 'At least 8 characters' : 'Your password'}
              autoComplete={isSignup ? 'new-password' : 'current-password'}
            />
            {errors.password ? <span className="error-text">{errors.password}</span> : null}
          </div>

          <button type="submit" className="btn btn-primary btn-block" disabled={busy}>
            {busy ? 'Please wait...' : isSignup ? 'Create account' : 'Log in'}
          </button>
        </form>

        <p className="auth-switch">
          {isSignup ? (
            <>
              Already have an account? <Link to="/login">Log in</Link>
            </>
          ) : (
            <>
              New here? <Link to="/signup">Create an account</Link>
            </>
          )}
        </p>
      </div>
    </main>
  )
}
