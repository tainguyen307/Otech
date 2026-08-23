import { createContext, useContext, useEffect, useState } from 'react'

const AuthContext = createContext(null)

function readUser() {
  try {
    const storedUser = JSON.parse(localStorage.getItem('user') || 'null')
    if (!storedUser) return null
    return { ...storedUser, fullName: storedUser.fullName || storedUser.name || 'Otech member' }
  } catch { return null }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readUser)

  useEffect(() => {
    const handleAuthRequired = () => {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      setUser(null)
    }
    window.addEventListener('otech:auth-required', handleAuthRequired)
    return () => window.removeEventListener('otech:auth-required', handleAuthRequired)
  }, [])

  const signIn = (authResponse) => {
    const nextUser = { id: authResponse.userId, email: authResponse.email, fullName: authResponse.fullName || 'Otech member', avatarUrl: authResponse.avatarUrl || null, role: authResponse.role }
    localStorage.setItem('token', authResponse.token)
    localStorage.setItem('user', JSON.stringify(nextUser))
    setUser(nextUser)
  }

  const signOut = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setUser(null)
  }

  return <AuthContext.Provider value={{ user, isAuthenticated: Boolean(user), signIn, signOut }}>{children}</AuthContext.Provider>
}

export function useAuth() { return useContext(AuthContext) }