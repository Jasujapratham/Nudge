import { useAuth } from '../context/AuthContext.jsx'

/**
 * App bar: brand, alarm sound switch, and logout.
 *
 * The "enable desktop notifications" action lives in the dashboard toolbar next
 * to the alarm explanation, where there is room for it on a phone.
 */
export default function Header({ soundOn, onToggleSound }) {
  const { user, logout } = useAuth()
  const initials = (user?.name || user?.email || '?')
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0].toUpperCase())
    .join('')

  return (
    <header className="topbar">
      <div className="topbar-inner">
        <div className="topbar-brand">
          <span className="wordmark">Nudge</span>
          <span className="tagline">Small reminders. Better days.</span>
        </div>

        <div className="topbar-user">
          <button
            type="button"
            className="btn btn-ghost"
            onClick={onToggleSound}
            aria-pressed={soundOn}
            title={soundOn ? 'Stop the alarm beeping' : 'Let the alarm beep'}
          >
            Sound: {soundOn ? 'on' : 'off'}
          </button>

          <span className="avatar" aria-hidden="true">
            {initials}
          </span>
          <span className="user-name">{user?.name || 'You'}</span>

          <button type="button" className="btn btn-ghost" onClick={logout}>
            Log out
          </button>
        </div>
      </div>
    </header>
  )
}
