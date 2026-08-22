import { useState } from 'react'
import AuthPrompt from './components/AuthPrompt'
import Header from './components/Header'
import { DesktopSidebar, NavButton, visibleNavItems } from './components/Navigation'
import HomeScreen from './screens/HomeScreen'
import { useAuth } from './auth/AuthContext.jsx'

function App() {
  const [activeNav, setActiveNav] = useState('Home')
  const [liked, setLiked] = useState([])
  const [saved, setSaved] = useState([])
  const [authPrompt, setAuthPrompt] = useState(false)
  const { user, signIn } = useAuth()
  const navigationItems = visibleNavItems(user)

  const requireAuth = () => setAuthPrompt(true)
  const handleNavigate = (item) => item.requiresAuth ? requireAuth() : setActiveNav(item.label)
  const toggle = (list, setList, id) => setList(list.includes(id) ? list.filter((item) => item !== id) : [...list, id])

  return <div className="min-h-screen bg-[#f5f7fa] text-[#172238]">
    <DesktopSidebar activeNav={activeNav} onNavigate={handleNavigate} onAuth={requireAuth} items={navigationItems} />
    <Header onAuth={requireAuth} />
    {activeNav === 'Home' && <HomeScreen liked={liked} saved={saved} onLike={(id) => toggle(liked, setLiked, id)} onSave={(id) => toggle(saved, setSaved, id)} onAuth={requireAuth} />}
    {activeNav === 'Marketplace' && <HomeScreen liked={liked} saved={saved} onLike={(id) => toggle(liked, setLiked, id)} onSave={(id) => toggle(saved, setSaved, id)} onAuth={requireAuth} />}
    <nav className="fixed inset-x-0 bottom-0 z-20 flex justify-around border-t border-[#e3e8f0] bg-white px-3 py-3 lg:hidden">{navigationItems.slice(0, 4).map((item) => <NavButton key={item.label} item={item} active={activeNav === item.label} onClick={() => handleNavigate(item)} mobile />)}</nav>
    {authPrompt && <AuthPrompt onClose={() => setAuthPrompt(false)} onSignedIn={(response) => { signIn(response); setAuthPrompt(false) }} />}
  </div>
}

export default App
