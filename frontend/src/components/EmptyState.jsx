import { InboxIcon } from './icons.jsx'

/** Friendly placeholder for an empty list - never fake sample data. */
export default function EmptyState({ filter, onCreate }) {
  const copy = {
    all: {
      heading: 'No reminders yet',
      body: 'Add your first one below and Nudge will ring when it is due.',
    },
    pending: {
      heading: 'Nothing pending',
      body: 'Every reminder is done. Enjoy the quiet.',
    },
    completed: {
      heading: 'Nothing completed yet',
      body: 'Tick a reminder off and it will show up here.',
    },
  }[filter]

  return (
    <div className="card empty-state">
      <div className="icon">
        <InboxIcon />
      </div>
      <h3>{copy.heading}</h3>
      <p>{copy.body}</p>
      {filter === 'all' ? (
        <p style={{ marginTop: 16 }}>
          <button type="button" className="btn btn-primary" onClick={onCreate}>
            Add a reminder
          </button>
        </p>
      ) : null}
    </div>
  )
}
