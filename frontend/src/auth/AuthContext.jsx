import { createContext, useContext, useEffect, useState } from 'react'

const AuthContext = createContext(null)

function readUser() {
  try { return JSON.parse(localStorage.getItem('otech_user') || 'null') } catch { return null }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(readUser)

  useEffect(() => {
    const handleAuthRequired = () => {
      localStorage.removeItem('otech_token')
      localStorage.removeItem('otech_user')
      setUser(null)
    }
    window.addEventListener('otech:auth-required', handleAuthRequired)
    return () => window.removeEventListener('otech:auth-required', handleAuthRequired)
  }, [])

  const signIn = (authResponse) => {
    const nextUser = { id: authResponse.userId, email: authResponse.email, role: authResponse.role }
    localStorage.setItem('otech_token', authResponse.token)
    localStorage.setItem('otech_user', JSON.stringify(nextUser))
    setUser(nextUser)
  }

  const signOut = () => {
    localStorage.removeItem('otech_token')
    localStorage.removeItem('otech_user')
    setUser(null)
  }

  return <AuthContext.Provider value={{ user, isAuthenticated: Boolean(user), signIn, signOut }}>{children}</AuthContext.Provider>
}

export function useAuth() { return useContext(AuthContext) }