import { useEffect, useMemo, useState } from 'react'
import { createReminder, updateReminder } from '../api/reminders.js'
import { readApiError } from '../api/client.js'
import { pad, todayInputValue } from '../utils/reminderTime.js'

export const PRIORITIES = [
  { value: 'LOW', label: 'Low' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'HIGH', label: 'High' },
]

/** Common categories - a datalist gives suggestions without forcing a choice. */
export const CATEGORY_SUGGESTIONS = ['Work', 'Study', 'Personal', 'Health', 'Bills', 'Errands']

function emptyDraft() {
  // Default to 10 minutes from now, so a fresh reminder rings shortly after
  // saving. The date is taken from `later` (not "today") to stay correct when
  // those 10 minutes roll past midnight.
  const later = new Date(Date.now() + 10 * 60000)
  return {
    title: '',
    notes: '',
    date: todayInputValue(later),
    time: `${pad(later.getHours())}:${pad(later.getMinutes())}`,
    priority: 'MEDIUM',
    category: '',
  }
}

function toDraft(reminder) {
  return {
    title: reminder.title ?? '',
    notes: reminder.notes ?? '',
    date: reminder.date ?? '',
    time: reminder.time ?? '',
    priority: reminder.priority ?? 'MEDIUM',
    category: reminder.category ?? '',
  }
}

/**
 * One form for both creating and editing: pass `reminder` to edit, omit it to
 * create. Field errors are checked here before the request goes out, and any
 * error the server sends back is displayed above the fields.
 */
export default function ReminderForm({ reminder, prefill, onSaved, onCancel }) {
  const isEdit = Boolean(reminder?.id)
  const [draft, setDraft] = useState(() => (reminder ? toDraft(reminder) : prefill ? toDraft(prefill) : emptyDraft()))
  const [errors, setErrors] = useState({})
  const [serverError, setServerError] = useState('')
  const [saving, setSaving] = useState(false)

  // Switch the fields when the form is reused for a different reminder.
  useEffect(() => {
    setDraft(reminder ? toDraft(reminder) : prefill ? toDraft(prefill) : emptyDraft())
    setErrors({})
    setServerError('')
  }, [reminder, prefill])

  const update = (field) => (event) => {
    setDraft((current) => ({ ...current, [field]: event.target.value }))
    setErrors((current) => (current[field] ? { ...current, [field]: undefined } : current))
  }

  const validate = useMemo(
    () => (values) => {
      const next = {}
      if (!values.title.trim()) next.title = 'Give the reminder a title.'
      else if (values.title.trim().length > 120) next.title = 'Keep the title under 120 characters.'
      if (!values.date) next.date = 'Pick a date.'
      if (!values.time) next.time = 'Pick a time.'
      if (values.notes.length > 500) next.notes = 'Notes must be under 500 characters.'
      if (values.category.length > 60) next.category = 'Category must be under 60 characters.'

      if (!next.date && !next.time) {
        const due = new Date(`${values.date}T${values.time.length === 5 ? values.time : `${values.time}:00`}`)
        if (Number.isNaN(due.getTime())) {
          next.time = 'That date and time could not be read.'
        } else if (!isEdit && due.getTime() <= Date.now()) {
          next.time = 'Choose a time in the future, or the alarm cannot ring.'
        }
      }
      return next
    },
    [isEdit],
  )

  async function handleSubmit(event) {
    event.preventDefault()
    const found = validate(draft)
    setErrors(found)
    if (Object.keys(found).length > 0) return

    setSaving(true)
    setServerError('')
    const payload = {
      title: draft.title.trim(),
      notes: draft.notes.trim() || null,
      date: draft.date,
      time: draft.time,
      priority: draft.priority,
      category: draft.category.trim() || null,
    }

    try {
      const saved = isEdit ? await updateReminder(reminder.id, payload) : await createReminder(payload)
      onSaved(saved, isEdit ? 'updated' : 'created')
      if (!isEdit) setDraft(emptyDraft())
    } catch (error) {
      setServerError(readApiError(error))
    } finally {
      setSaving(false)
    }
  }

  return (
    <form className="card form-panel" onSubmit={handleSubmit} noValidate>
      <div className="form-panel-head">
        <h2>{isEdit ? `Edit: ${reminder.title}` : 'New reminder'}</h2>
        {onCancel ? (
          <button type="button" className="btn btn-ghost" onClick={onCancel}>
            Cancel
          </button>
        ) : null}
      </div>

      {serverError ? (
        <div className="alert alert-error" role="alert">
          {serverError}
        </div>
      ) : null}

      <div className="field">
        <label htmlFor="title">Title *</label>
        <input
          id="title"
          className={`input ${errors.title ? 'invalid' : ''}`}
          value={draft.title}
          onChange={update('title')}
          placeholder="Submit assignment"
          maxLength={140}
          autoFocus={!isEdit}
        />
        {errors.title ? <span className="error-text">{errors.title}</span> : null}
      </div>

      <div className="field">
        <label htmlFor="notes">Notes</label>
        <textarea
          id="notes"
          className={`textarea ${errors.notes ? 'invalid' : ''}`}
          value={draft.notes}
          onChange={update('notes')}
          placeholder="Upload the final project before the deadline"
          rows={2}
        />
        {errors.notes ? <span className="error-text">{errors.notes}</span> : null}
      </div>

      <div className="form-row">
        <div className="field">
          <label htmlFor="date">Date *</label>
          <input
            id="date"
            type="date"
            className={`input ${errors.date ? 'invalid' : ''}`}
            value={draft.date}
            onChange={update('date')}
            min={isEdit ? undefined : todayInputValue()}
          />
          {errors.date ? <span className="error-text">{errors.date}</span> : null}
        </div>

        <div className="field">
          <label htmlFor="time">Time *</label>
          <input
            id="time"
            type="time"
            className={`input ${errors.time ? 'invalid' : ''}`}
            value={draft.time}
            onChange={update('time')}
            step={60}
          />
          {errors.time ? <span className="error-text">{errors.time}</span> : null}
        </div>
      </div>

      <div className="form-row">
        <div className="field">
          <label htmlFor="priority">Priority</label>
          <select id="priority" className="select" value={draft.priority} onChange={update('priority')}>
            {PRIORITIES.map((priority) => (
              <option key={priority.value} value={priority.value}>
                {priority.label}
              </option>
            ))}
          </select>
        </div>

        <div className="field">
          <label htmlFor="category">Category (optional)</label>
          <input
            id="category"
            className={`input ${errors.category ? 'invalid' : ''}`}
            value={draft.category}
            onChange={update('category')}
            placeholder="Work"
            list="category-options"
            maxLength={60}
          />
          <datalist id="category-options">
            {CATEGORY_SUGGESTIONS.map((category) => (
              <option key={category} value={category} />
            ))}
          </datalist>
          {errors.category ? <span className="error-text">{errors.category}</span> : null}
        </div>
      </div>

      <div className="form-actions">
        <button type="submit" className="btn btn-primary" disabled={saving}>
          {saving ? 'Saving...' : isEdit ? 'Save changes' : 'Add reminder'}
        </button>
      </div>
    </form>
  )
}
