import api from './axios'

export const accountApi = {
  async downloadExport() {
    const response = await api.get<Blob>('/account/export', { responseType: 'blob' })
    const date = new Date().toISOString().slice(0, 10)
    const url = URL.createObjectURL(new Blob([response.data], { type: 'application/json' }))
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = `seopulse-data-${date}.json`
    document.body.appendChild(anchor)
    anchor.click()
    anchor.remove()
    URL.revokeObjectURL(url)
  },
  delete: (password: string) => api.post('/account/delete', { password }),
}
