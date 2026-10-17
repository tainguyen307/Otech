import { useCallback, useEffect, useState } from 'react'
import Header from './components/Header'
import { DesktopSidebar, NavButton, visibleNavItems } from './components/Navigation'
import HomeScreen from './screens/HomeScreen'
import MarketplaceScreen from './screens/MarketplaceScreen'
import MessagesScreen from './screens/MessagesScreen'
import AuthScreen from './screens/AuthScreen'
import ProfileScreen from './screens/ProfileScreen'
import PublicProfileScreen from './screens/PublicProfileScreen'
import { useAuth } from './auth/AuthContext.jsx'
import { apiErrorMessage, apiFetch } from './lib/api'

function routeFromLocation() {
  const path = window.location.pathname.replace(/\/+$/, '') || '/'
  if (path === '/marketplace') return { activeNav: 'Marketplace', profileUserId: null, conversationId: '' }
  if (path === '/messages') return { activeNav: 'Messages', profileUserId: null, conversationId: '' }
  const conversationMatch = path.match(/^\/messages\/([^/]+)$/)
  if (conversationMatch) {
    return { activeNav: 'Messages', profileUserId: null, conversationId: decodeURIComponent(conversationMatch[1]) }
  }
  if (path === '/profile') return { activeNav: 'Profile', profileUserId: null, conversationId: '' }
  const profileMatch = path.match(/^\/profile\/([^/]+)$/)
  if (profileMatch) {
    return { activeNav: 'Profile', profileUserId: decodeURIComponent(profileMatch[1]), conversationId: '' }
  }
  if (path === '/admin') return { activeNav: 'Admin', profileUserId: null, conversationId: '' }
  return { activeNav: 'Home', profileUserId: null, conversationId: '' }
}

function App() {
  const [route, setRoute] = useState(routeFromLocation)
  const [liked, setLiked] = useState([])
  const [saved, setSaved] = useState([])
  const [authMode, setAuthMode] = useState(null)
  const [actionError, setActionError] = useState('')
  const [darkMode, setDarkMode] = useState(() => localStorage.getItem('theme') === 'dark')
  const { user, signIn, signOut, updateUser } = useAuth()
  const navigationItems = visibleNavItems(user)
  const { activeNav, profileUserId, conversationId } = route

  useEffect(() => {
    document.documentElement.classList.toggle('dark', darkMode)
    localStorage.setItem('theme', darkMode ? 'dark' : 'light')
  }, [darkMode])

  useEffect(() => {
    const handlePopState = () => {
      setRoute(routeFromLocation())
      setActionError('')
    }
    window.addEventListener('popstate', handlePopState)
    return () => window.removeEventListener('popstate', handlePopState)
  }, [])

  const navigateTo = (path) => {
    if (window.location.pathname !== path) window.history.pushState({}, '', path)
    setRoute(routeFromLocation())
    setActionError('')
  }
  const requireAuth = useCallback(() => setAuthMode('signIn'), [])
  const handleNavigate = (item) => {
    if (item.requiresAuth && !user) {
      requireAuth()
      return
    }
    const paths = { Home: '/', Marketplace: '/marketplace', Messages: '/messages', Profile: '/profile', Admin: '/admin' }
    navigateTo(paths[item.label] || '/')
  }
  const handleOpenProfile = (profileId) => {
    navigateTo(user?.id === profileId ? '/profile' : `/profile/${encodeURIComponent(profileId)}`)
  }
  const handleStartConversation = (conversationId) => {
    navigateTo(`/messages/${encodeURIComponent(conversationId)}`)
  }
  const handleStartDirectConversation = async (recipientId) => {
    if (!user) {
      requireAuth()
      return
    }
    setActionError('')
    try {
      const response = await apiFetch('/conversations', {
        method: 'POST',
        body: JSON.stringify({ recipientId }),
      })
      if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not start this conversation.'))
      const conversation = await response.json()
      handleStartConversation(conversation.id)
    } catch (requestError) {
      setActionError(requestError.message || 'Could not start this conversation.')
    }
  }
  const toggle = (list, setList, id) => setList(list.includes(id) ? list.filter((item) => item !== id) : [...list, id])

  return <div className="min-h-screen bg-[#f5f7fa] text-[#172238] dark:bg-slate-950 dark:text-slate-100">
    <DesktopSidebar activeNav={activeNav} onNavigate={handleNavigate} onAuth={requireAuth} items={navigationItems} user={user} />
    <Header onAuth={requireAuth} darkMode={darkMode} onToggleTheme={() => setDarkMode((value) => !value)} user={user} onSignOut={() => { signOut(); navigateTo('/') }} onHome={() => navigateTo('/')} onProfile={() => navigateTo('/profile')} />
    {actionError && <div role="alert" className="mx-5 mt-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700 lg:ml-[268px] lg:mr-10 dark:border-rose-900 dark:bg-rose-950/30 dark:text-rose-300">{actionError}</div>}
    {activeNav === 'Home' && <HomeScreen liked={liked} saved={saved} onLike={(id) => toggle(liked, setLiked, id)} onSave={(id) => toggle(saved, setSaved, id)} onAuth={requireAuth} onOpenProfile={handleOpenProfile} onMessage={handleStartDirectConversation} user={user} />}
    {activeNav === 'Marketplace' && <MarketplaceScreen user={user} onAuth={requireAuth} onStartConversation={handleStartConversation} />}
    {activeNav === 'Messages' && user && <MessagesScreen key={conversationId || 'messages'} user={user} initialConversationId={conversationId} onAuth={requireAuth} />}
    {activeNav === 'Messages' && !user && <main className="px-5 py-16 text-center lg:ml-[248px]"><h1 className="text-2xl font-extrabold">Sign in to view messages</h1><button onClick={requireAuth} className="mt-4 rounded-xl bg-[#087cf5] px-4 py-3 text-sm font-bold text-white">Sign in</button></main>}
    {activeNav === 'Profile' && profileUserId && <PublicProfileScreen key={profileUserId} userId={profileUserId} currentUser={user} onBack={() => navigateTo('/')} onAuth={requireAuth} onMessage={handleStartDirectConversation} />}
    {activeNav === 'Profile' && !profileUserId && user && <ProfileScreen user={user} onUpdateUser={updateUser} onSignOut={() => { signOut(); navigateTo('/') }} />}
    {activeNav === 'Profile' && !profileUserId && !user && <main className="px-5 py-16 text-center lg:ml-[248px]"><h1 className="text-2xl font-extrabold">Sign in to view your profile</h1><button onClick={requireAuth} className="mt-4 rounded-xl bg-[#087cf5] px-4 py-3 text-sm font-bold text-white">Sign in</button></main>}
    <nav className="fixed inset-x-0 bottom-0 z-20 flex justify-around border-t border-[#e3e8f0] bg-white px-3 py-3 lg:hidden">{navigationItems.slice(0, 4).map((item) => <NavButton key={item.label} item={item} active={activeNav === item.label} onClick={() => handleNavigate(item)} mobile />)}</nav>
    {authMode && <AuthScreen initialMode={authMode} onClose={() => setAuthMode(null)} onSignedIn={(response) => { signIn(response); setAuthMode(null) }} />}
  </div>
}

export default App
