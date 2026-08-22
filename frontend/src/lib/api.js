const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

export async function apiFetch(path, options = {}) {
  const token = localStorage.getItem('otech_token')
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  })

  if (response.status === 401 || response.status === 403) {
    window.dispatchEvent(new CustomEvent('otech:auth-required'))
  }
  return response
}

export { API_URL }