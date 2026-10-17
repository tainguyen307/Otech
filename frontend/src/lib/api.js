const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

export async function apiFetch(path, options = {}) {
  const token = localStorage.getItem('token')
  const isFormData = options.body instanceof FormData
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  })

  if (response.status === 401) {
    window.dispatchEvent(new CustomEvent('otech:auth-required'))
  }
  return response
}

export async function apiErrorMessage(response, fallback) {
  try {
    const body = await response.json()
    return body.error?.message || body.message || fallback
  } catch {
    return fallback
  }
}

export { API_URL }