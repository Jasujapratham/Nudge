import { useCallback, useEffect, useMemo, useState } from 'react'
import { deleteReminder, listReminders, setReminderCompleted } from '../api/reminders.js'
import { readApiError } from '../api/client.js'
import { useAuth } from '../context/AuthContext.jsx'
import { useReminderAlarm } from '../hooks/useReminderAlarm.js'
import ReminderCard from '../components/ReminderCard.jsx'
import ReminderForm from '../components/ReminderForm.jsx'
import AlarmBanner from '../components/AlarmBanner.jsx'
import EmptyState from '../components/EmptyState.jsx'
import Header from '../components/Header.jsx'
import AIReminderPanel from '../components/AIReminderPanel.jsx'

const FILTERS = [
  { key: 'all', label: 'All' },
  { key: 'pending', label: 'Pending' },
  { key: 'completed', label: 'Completed' },
]

export default function DashboardPage() {
  const { user } = useAuth()

  const [reminders, setReminders] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [actionError, setActionError] = useState('')
  const [busyId, setBusyId] = useState(null)
  const [filter, setFilter] = useState('all')
  const [showForm, setShowForm] = useState(false)
  const [editing, setEditing] = useState(null)
  const [aiPrefill, setAiPrefill] = useState(null)

  const {
    dueList,
    dismissDue,
    clearAllDue,
    requestPermission,
    permission,
    notificationsSupported,
    soundOn,
    setSoundOn,
  } = useReminderAlarm(reminders, user?.id)

  const load = useCallback(
    async ({ quiet = false } = {}) => {
      if (!quiet) setLoading(true)
      try {
        const data = await listReminders()
        // Already sorted by the backend (date, then time); sort again so the
        // list stays ordered after local edits too.
        setReminders(
          [...data].sort(
            (a, b) => `${a.date}T${a.time}`.localeCompare(`${b.date}T${b.time}`) || a.id - b.id,
          ),
        )
        setLoadError('')
      } catch (error) {
        setLoadError(readApiError(error))
      } finally {
        setLoading(false)
      }
    },
    [],
  )

  useEffect(() => {
    load()
  }, [load])

  // Keep the alarm in step with the data every 30s, without a spinner.
  useEffect(() => {
    const timer = window.setInterval(() => load({ quiet: true }), 30000)
    return () => window.clearInterval(timer)
  }, [load])

  const counts = useMemo(
    () => ({
      all: reminders.length,
      pending: reminders.filter((r) => !r.completed).length,
      completed: reminders.filter((r) => r.completed).length,
    }),
    [reminders],
  )

  const visible = useMemo(() => {
    if (filter === 'pending') return reminders.filter((r) => !r.completed)
    if (filter === 'completed') return reminders.filter((r) => r.completed)
    return reminders
  }, [reminders, filter])

  async function handleToggle(reminder) {
    setBusyId(reminder.id)
    setActionError('')
    try {
      await setReminderCompleted(reminder.id, !reminder.completed)
      await load({ quiet: true })
    } catch (error) {
      setActionError(readApiError(error))
    } finally {
      setBusyId(null)
    }
  }

  async function handleDelete(reminder) {
    // A plain confirm dialog is enough here and needs no extra dependency.
    const ok = window.confirm(`Delete "${reminder.title}"? This cannot be undone.`)
    if (!ok) return

    setBusyId(reminder.id)
    setActionError('')
    try {
      await deleteReminder(reminder.id)
      setReminders((current) => current.filter((r) => r.id !== reminder.id))
      dismissDue(reminder.id)
      if (editing?.id === reminder.id) setEditing(null)
    } catch (error) {
      setActionError(readApiError(error))
    } finally {
      setBusyId(null)
    }
  }

  function handleSaved(saved, kind) {
    if (kind === 'updated') {
      setEditing(null)
      setShowForm(false)
    } else {
      // Put the new reminder in straight away, then re-read so the order and
      // the alarm are guaranteed to match the server.
      setReminders((current) =>
        [...current.filter((r) => r.id !== saved.id), saved].sort(
          (a, b) => `${a.date}T${a.time}`.localeCompare(`${b.date}T${b.time}`) || a.id - b.id,
        ),
      )
    }
    load({ quiet: true })
  }

  return (
    <>
      <Header soundOn={soundOn} onToggleSound={() => setSoundOn((value) => !value)} />

      <main className="page">
        <AlarmBanner
          dueList={dueList}
          onDismiss={dismissDue}
          onDismissAll={clearAllDue}
          onComplete={async (reminder) => {
            await handleToggle(reminder)
            dismissDue(reminder.id)
          }}
        />

        {loadError ? (
          <div className="alert alert-error" role="alert">
            {loadError}{' '}
            <button type="button" className="link-button" onClick={() => load()}>
              Try again
            </button>
          </div>
        ) : null}

        <section className="section-head">
          <h2>
            Hello, {user?.name?.split(' ')[0] || 'there'}
            <span className="count">
              {' '}
              - {counts.pending} pending, {counts.completed} completed
            </span>
          </h2>
          <button
            type="button"
            className="btn btn-primary"
            onClick={() => {
              setEditing(null)
              setAiPrefill(null)
              setShowForm((open) => !open)
            }}
          >
            {showForm && !editing ? 'Close form' : '+ New reminder'}
          </button>
        </section>

        {actionError ? (
          <div className="alert alert-error" role="alert">
            {actionError}
          </div>
        ) : null}

        <AIReminderPanel
          onUse={(parsed) => {
            setEditing(null)
            setAiPrefill({
              title: parsed.title || '',
              notes: parsed.notes || '',
              date: parsed.date || '',
              time: parsed.time || '',
              priority: parsed.priority || 'MEDIUM',
              category: parsed.category || '',
            })
            setShowForm(true)
          }}
        />

        {showForm || editing ? (
          <ReminderForm
            reminder={editing}
            prefill={aiPrefill}
            onSaved={handleSaved}
            onCancel={() => {
              setShowForm(false)
              setEditing(null)
              setAiPrefill(null)
            }}
          />
        ) : null}

        <div className="toolbar">
          <div className="filters" role="tablist" aria-label="Filter reminders">
            {FILTERS.map(({ key, label }) => (
              <button
                key={key}
                type="button"
                role="tab"
                aria-selected={filter === key}
                className={filter === key ? 'active' : ''}
                onClick={() => setFilter(key)}
              >
                {label}
                <span className="pill">{counts[key]}</span>
              </button>
            ))}
          </div>
          <div className="alarm-controls">
            {notificationsSupported && permission === 'default' ? (
              <button type="button" className="btn btn-ghost" onClick={requestPermission}>
                Enable desktop notifications
              </button>
            ) : null}
            {notificationsSupported && permission === 'granted' ? (
              <span className="tagline">Desktop notifications are on.</span>
            ) : null}
            {notificationsSupported && permission === 'denied' ? (
              <span className="tagline" title="Allow notifications in your browser settings for desktop alerts.">
                Browser notifications are blocked, so alarms show in-app.
              </span>
            ) : null}
            {!notificationsSupported ? (
              <span className="tagline">This browser has no notification API, so alarms show in-app.</span>
            ) : null}
          </div>
        </div>

        {loading ? (
          <div className="card skeleton">
            <span className="spinner" aria-hidden="true" />
            Loading your reminders...
          </div>
        ) : visible.length === 0 ? (
          <EmptyState filter={filter} onCreate={() => setShowForm(true)} />
        ) : (
          <div className="reminder-list">
            {visible.map((reminder) => (
              <ReminderCard
                key={reminder.id}
                reminder={reminder}
                busy={busyId === reminder.id}
                onToggle={handleToggle}
                onEdit={(item) => {
                  setEditing(item)
                  setShowForm(false)
                }}
                onDelete={handleDelete}
              />
            ))}
          </div>
        )}

        <p className="footnote">
          Nudge rings while this tab is open. A closed tab cannot show an alarm, because the browser
          does not run the app in the background.
        </p>
      </main>
    </>
  )
}
