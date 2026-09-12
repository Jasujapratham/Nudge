import api from './client.js'

/** Thin wrapper around the reminder endpoints - one function per API call. */

export function listReminders() {
  return api.get('/reminders').then((res) => res.data)
}

export function getReminder(id) {
  return api.get(`/reminders/${id}`).then((res) => res.data)
}

export function createReminder(payload) {
  return api.post('/reminders', payload).then((res) => res.data)
}

export function updateReminder(id, payload) {
  return api.put(`/reminders/${id}`, payload).then((res) => res.data)
}

/** Mark done (true) or back to pending (false). No body = server toggles. */
export function setReminderCompleted(id, completed) {
  return api
    .patch(`/reminders/${id}/complete`, { completed })
    .then((res) => res.data)
}

export function deleteReminder(id) {
  return api.delete(`/reminders/${id}`).then((res) => res.data)
}
