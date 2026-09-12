import { formatWhen } from '../utils/reminderTime.js'
import { BellIcon } from './icons.jsx'

/**
 * In-app alarm. Always shown when a reminder comes due, because the browser may
 * have denied desktop notifications. Each entry can be dismissed or marked done.
 */
export default function AlarmBanner({ dueList, onDismiss, onComplete, onDismissAll }) {
  if (dueList.length === 0) return null

  return (
    <section className="alarm-banner" role="alert" aria-live="assertive">
      <h3>
        <BellIcon size={15} />
        {dueList.length === 1 ? 'A reminder is due' : `${dueList.length} reminders are due`}
      </h3>

      {dueList.map((reminder) => (
        <div className="alarm-item" key={reminder.id}>
          <div>
            <strong>{reminder.title}</strong>
            <div className="when">{formatWhen(reminder)}</div>
          </div>
          <div className="reminder-actions">
            <button type="button" className="btn btn-primary" onClick={() => onComplete(reminder)}>
              Mark done
            </button>
            <button type="button" className="btn btn-ghost" onClick={() => onDismiss(reminder.id)}>
              Dismiss
            </button>
          </div>
        </div>
      ))}

      {dueList.length > 1 ? (
        <button type="button" className="link-button" onClick={onDismissAll}>
          Dismiss all
        </button>
      ) : null}
    </section>
  )
}
