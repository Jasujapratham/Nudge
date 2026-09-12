import { useCallback, useEffect, useRef, useState } from 'react'
import { formatWhen, toDueDate } from '../utils/reminderTime.js'

const CHECK_INTERVAL_MS = 15000
const FIRED_KEY_PREFIX = 'nudge.fired.'

/**
 * The browser alarm.
 *
 * Every 15 seconds (and once on mount) it looks at the pending reminders and
 * rings for any whose date/time has arrived. A reminder rings once and then
 * goes into localStorage, so refreshing the page does not spam you again.
 *
 * Rings means: an in-app banner (always), a desktop Notification (if the user
 * granted permission), and a short beep (if the browser allows audio).
 */
export function useReminderAlarm(reminders, userId) {
  const [dueList, setDueList] = useState([])
  const [permission, setPermission] = useState(
    typeof Notification !== 'undefined' ? Notification.permission : 'unsupported',
  )
  const [soundOn, setSoundOn] = useState(true)

  // Ids already announced. Persisted per user so a reload stays quiet.
  const storageKey = `${FIRED_KEY_PREFIX}${userId ?? 'anon'}`
  const firedRef = useRef(null)

  if (firedRef.current === null) {
    try {
      firedRef.current = new Set(JSON.parse(localStorage.getItem(storageKey) || '[]'))
    } catch {
      firedRef.current = new Set()
    }
  }

  const audioContextRef = useRef(null)

  const playChime = useCallback(() => {
    try {
      const Ctx = window.AudioContext || window.webkitAudioContext
      if (!Ctx) return
      audioContextRef.current = audioContextRef.current || new Ctx()
      const ctx = audioContextRef.current
      if (ctx.state === 'suspended') {
        ctx.resume().catch(() => {})
      }
      // Two short notes - no audio file to ship, works offline.
      ;[0, 0.22].forEach((offset, index) => {
        const osc = ctx.createOscillator()
        const gain = ctx.createGain()
        osc.type = 'sine'
        osc.frequency.value = index === 0 ? 880 : 1175
        gain.gain.setValueAtTime(0.0001, ctx.currentTime + offset)
        gain.gain.exponentialRampToValueAtTime(0.18, ctx.currentTime + offset + 0.02)
        gain.gain.exponentialRampToValueAtTime(0.0001, ctx.currentTime + offset + 0.2)
        osc.connect(gain).connect(ctx.destination)
        osc.start(ctx.currentTime + offset)
        osc.stop(ctx.currentTime + offset + 0.22)
      })
    } catch {
      // Audio is a nice-to-have; never let it break the alarm.
    }
  }, [])

  const check = useCallback(() => {
    if (!Array.isArray(reminders) || reminders.length === 0) return

    const now = new Date()
    const dueNow = reminders.filter((reminder) => {
      if (reminder.completed) return false
      if (firedRef.current.has(reminder.id)) return false
      const due = toDueDate(reminder)
      // "Due" means the alarm moment has arrived (within this poll window).
      return due !== null && due.getTime() <= now.getTime()
    })

    if (dueNow.length === 0) return

    dueNow.forEach((reminder) => firedRef.current.add(reminder.id))
    try {
      localStorage.setItem(storageKey, JSON.stringify([...firedRef.current]))
    } catch {
      // Storage can be full or blocked; the in-app banner still works.
    }

    setDueList((current) => [...current, ...dueNow])

    dueNow.forEach((reminder) => {
      const body = `${formatWhen(reminder)}${reminder.category ? ` - ${reminder.category}` : ''}`
      if (typeof Notification !== 'undefined' && Notification.permission === 'granted') {
        try {
          new Notification(`Nudge: ${reminder.title}`, { body, tag: `nudge-${reminder.id}` })
        } catch {
          // Some browsers refuse notifications from non-secure origins - ignore.
        }
      }
    })

    if (soundOn) playChime()
  }, [reminders, soundOn, playChime, storageKey])

  useEffect(() => {
    check()
    const timer = window.setInterval(check, CHECK_INTERVAL_MS)
    return () => window.clearInterval(timer)
  }, [check])

  const requestPermission = useCallback(async () => {
    if (typeof Notification === 'undefined') return
    try {
      const result = await Notification.requestPermission()
      setPermission(result)
    } catch {
      setPermission('denied')
    }
  }, [])

  /** Clears the banner. The reminder stays pending, so it is still on the list. */
  const dismissDue = useCallback((id) => {
    setDueList((current) => current.filter((reminder) => reminder.id !== id))
  }, [])

  const clearAllDue = useCallback(() => setDueList([]), [])

  const notificationsSupported = typeof Notification !== 'undefined'

  return {
    dueList,
    check,
    dismissDue,
    clearAllDue,
    requestPermission,
    permission,
    notificationsSupported,
    soundOn,
    setSoundOn,
  }
}
