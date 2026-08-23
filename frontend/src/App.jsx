import { useEffect, useState } from 'react'
import Header from './components/Header'
import { DesktopSidebar, NavButton, visibleNavItems } from './components/Navigation'
import HomeScreen from './screens/HomeScreen'
import AuthScreen from './screens/AuthScreen'
import ProfileScreen from './screens/ProfileScreen'
import { useAuth } from './auth/AuthContext.jsx'

function App() {
  const [activeNav, setActiveNav] = useState('Home')
  const [liked, setLiked] = useState([])
  const [saved, setSaved] = useState([])
  const [authMode, setAuthMode] = useState(null)
  const [darkMode, setDarkMode] = useState(() => localStorage.getItem('theme') === 'dark')
  const { user, signIn, signOut } = useAuth()
  const navigationItems = visibleNavItems(user)

  useEffect(() => {
    document.documentElement.classList.toggle('dark', darkMode)
    localStorage.setItem('theme', darkMode ? 'dark' : 'light')
  }, [darkMode])

  const requireAuth = () => setAuthMode('signIn')
  const handleNavigate = (item) => item.requiresAuth && !user ? requireAuth() : setActiveNav(item.label)
  const toggle = (list, setList, id) => setList(list.includes(id) ? list.filter((item) => item !== id) : [...list, id])

  return <div className="min-h-screen bg-[#f5f7fa] text-[#172238] dark:bg-slate-950 dark:text-slate-100">
    <DesktopSidebar activeNav={activeNav} onNavigate={handleNavigate} onAuth={requireAuth} items={navigationItems} user={user} />
    <Header onAuth={requireAuth} darkMode={darkMode} onToggleTheme={() => setDarkMode((value) => !value)} user={user} onSignOut={() => { signOut(); setActiveNav('Home') }} onHome={() => setActiveNav('Home')} />
    {activeNav === 'Home' && <HomeScreen liked={liked} saved={saved} onLike={(id) => toggle(liked, setLiked, id)} onSave={(id) => toggle(saved, setSaved, id)} onAuth={requireAuth} />}
    {activeNav === 'Marketplace' && <HomeScreen liked={liked} saved={saved} onLike={(id) => toggle(liked, setLiked, id)} onSave={(id) => toggle(saved, setSaved, id)} onAuth={requireAuth} />}
      {activeNav === 'Profile' && user && <ProfileScreen user={user} onSignOut={() => { signOut(); setActiveNav('Home') }} />}
    <nav className="fixed inset-x-0 bottom-0 z-20 flex justify-around border-t border-[#e3e8f0] bg-white px-3 py-3 lg:hidden">{navigationItems.slice(0, 4).map((item) => <NavButton key={item.label} item={item} active={activeNav === item.label} onClick={() => handleNavigate(item)} mobile />)}</nav>
    {authMode && <AuthScreen initialMode={authMode} onClose={() => setAuthMode(null)} onSignedIn={(response) => { signIn(response); setAuthMode(null) }} />}
  </div>
}

export default App
