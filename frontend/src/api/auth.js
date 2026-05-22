import client, { unwrap } from './client'

export const authApi = {
  login: (username, password) =>
    client.post('/auth/login', { username, password }).then(unwrap),
  me: () => client.get('/auth/me').then(unwrap),
}
