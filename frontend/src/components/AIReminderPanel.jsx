import { useState } from 'react'
import { parseReminderWithAI } from '../api/ai.js'
import { readApiError } from '../api/client.js'

export default function AIReminderPanel({ onUse }) {
  const [text, setText] = useState('')
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    if (!text.trim()) return
    setLoading(true)
    setError('')
    setResult(null)
    try {
      setResult(await parseReminderWithAI(text.trim()))
    } catch (requestError) {
      setError(readApiError(requestError))
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="card form-panel ai-panel">
      <div className="form-panel-head">
        <div>
          <h2>✨ Create with Nudge AI</h2>
          <p className="muted">Tell Nudge what you need to remember in plain English.</p>
        </div>
      </div>
      <form onSubmit={handleSubmit}>
        <div className="field">
          <label htmlFor="ai-reminder-text">Your reminder</label>
          <textarea
            id="ai-reminder-text"
            className="textarea"
            rows={2}
            value={text}
            onChange={(event) => setText(event.target.value)}
            placeholder="Remind me to submit my assignment tomorrow at 9 AM"
          />
        </div>
        <button type="submit" className="btn btn-primary" disabled={loading || !text.trim()}>
          {loading ? 'Thinking...' : 'Understand reminder'}
        </button>
      </form>
      {error ? <div className="alert alert-error" role="alert">{error}</div> : null}
      {result ? (
        <div className="ai-result" aria-live="polite">
          {result.needsClarification ? (
            <p>{result.clarificationQuestion || 'Please provide a date and time.'}</p>
          ) : (
            <>
              <strong>{result.title}</strong>
              <p>{result.date} at {result.time}</p>
              <p className="muted">Priority: {result.priority || 'MEDIUM'}{result.category ? ` · ${result.category}` : ''}</p>
              <button type="button" className="btn btn-secondary" onClick={() => onUse(result)}>
                Use this in reminder form
              </button>
            </>
          )}
        </div>
      ) : null}
    </section>
  )
}
