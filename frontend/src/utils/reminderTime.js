/**
 * Date/time helpers shared by the dashboard and the alarm hook.
 *
 * The backend sends "date": "2026-09-20" and "time": "18:30". We build the
 * JavaScript Date from those parts explicitly instead of calling
 * new Date(reminder.dueAt), because a string without a timezone offset can be
 * parsed inconsistently. Building it from numbers always means "local time",
 * which is exactly what a reminder should be.
 */

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
const DAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']

export function toDueDate(reminder) {
  if (!reminder?.date || !reminder?.time) return null
  const [year, month, day] = String(reminder.date).split('-').map(Number)
  const [hour, minute] = String(reminder.time).split(':').map(Number)
  if (!year || !month || !day) return null
  const date = new Date(year, month - 1, day, hour || 0, minute || 0, 0, 0)
  return Number.isNaN(date.getTime()) ? null : date
}

export function pad(value) {
  return String(value).padStart(2, '0')
}

export function formatClock(date) {
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** "Today 18:30", "Tomorrow 09:00", "Sat 20 Sep 11:15" */
export function formatWhen(reminder, now = new Date()) {
  const due = toDueDate(reminder)
  if (!due) return 'No date set'

  const startOfDay = (d) => new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime()
  const diffDays = Math.round((startOfDay(due) - startOfDay(now)) / 86400000)
  const clock = formatClock(due)

  if (diffDays === 0) return `Today ${clock}`
  if (diffDays === 1) return `Tomorrow ${clock}`
  if (diffDays === -1) return `Yesterday ${clock}`
  return `${DAYS[due.getDay()]} ${due.getDate()} ${MONTHS[due.getMonth()]} ${clock}`
}

/** "in 5 min", "in 2 h 10 min", "12 min overdue" */
export function formatRelative(reminder, now = new Date()) {
  const due = toDueDate(reminder)
  if (!due) return ''

  const minutes = Math.round((due.getTime() - now.getTime()) / 60000)
  if (minutes === 0) return 'due now'

  const abs = Math.abs(minutes)
  const parts =
    abs >= 60 ? (abs % 60 === 0 ? `${Math.floor(abs / 60)} h` : `${Math.floor(abs / 60)} h ${abs % 60} min`) : `${abs} min`

  return minutes > 0 ? `in ${parts}` : `${parts} overdue`
}

export function isOverdue(reminder, now = new Date()) {
  const due = toDueDate(reminder)
  return Boolean(due && due.getTime() <= now.getTime())
}

/** For <input type="date"> / <input type="time">, which need "YYYY-MM-DD" / "HH:MM". */
export function dueDateInputValue(reminder) {
  return reminder?.date ? String(reminder.date) : ''
}

export function dueTimeInputValue(reminder) {
  if (!reminder?.time) return ''
  const [h, m] = String(reminder.time).split(':')
  return `${pad(Number(h))}:${pad(Number(m) || 0)}`
}

export function todayInputValue(now = new Date()) {
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}
