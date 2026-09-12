import { formatRelative, formatWhen, isOverdue } from '../utils/reminderTime.js'

import { CheckIcon } from './icons.jsx'

/**
 * One reminder row.
 *
 * Purely presentational: it renders the props it is given and reports clicks
 * upward, which keeps the dashboard logic in one place.
 */
/** "in 25 min", "due now", "overdue 6 min" - the overdue case never doubles up. */
function statusLabel(reminder, overdue) {
  const relative = formatRelative(reminder)
  if (!overdue) return relative
  return relative === 'due now' ? 'overdue' : `overdue ${relative}`
}

export default function ReminderCard({ reminder, onToggle, onEdit, onDelete, busy }) {
  const overdue = !reminder.completed && isOverdue(reminder)

  const classes = [
    'reminder-card',
    `priority-${reminder.priority}`,
    reminder.completed ? 'is-completed' : '',
    overdue ? 'is-overdue' : '',
  ]
    .filter(Boolean)
    .join(' ')

  const toggleLabel = reminder.completed ? 'Mark as pending' : 'Mark as completed'

  return (
    <article className={`card ${classes}`}>
      <button
        type="button"
        className={`check ${reminder.completed ? 'done' : ''}`}
        onClick={() => onToggle(reminder)}
        disabled={busy}
        aria-label={toggleLabel}
        title={toggleLabel}
      >
        <CheckIcon size={12} />
      </button>

      <div className="reminder-main">
        <h3 className="reminder-title">{reminder.title}</h3>

        <div className="reminder-actions">
          <button type="button" className="btn btn-ghost" onClick={() => onEdit(reminder)} disabled={busy}>
            Edit
          </button>
          <button type="button" className="btn btn-danger" onClick={() => onDelete(reminder)} disabled={busy}>
            Delete
          </button>
        </div>

        {reminder.notes ? <p className="reminder-notes">{reminder.notes}</p> : null}

        <div className="reminder-meta">
          <span className={`badge ${reminder.priority}`}>{reminder.priority.toLowerCase()}</span>
          <span>{formatWhen(reminder)}</span>
          {reminder.category ? <span className="badge cat">{reminder.category}</span> : null}
          {reminder.completed ? (
            <span className="badge state-done">Completed</span>
          ) : (
            <span className={`badge ${overdue ? 'overdue' : 'state-pending'}`}>
              {statusLabel(reminder, overdue)}
            </span>
          )}
        </div>
      </div>
    </article>
  )
}
