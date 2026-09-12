import api from './client.js'

export function parseReminderWithAI(text) {
  const now = new Date()
  const pad = (value) => String(value).padStart(2, '0')
  const currentDate = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
  const currentTime = `${pad(now.getHours())}:${pad(now.getMinutes())}`
  return api.post('/ai/parse-reminder', {
    text,
    currentDate,
    currentTime,
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
  }).then((response) => response.data)
}
