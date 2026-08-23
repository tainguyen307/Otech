import { Home, LayoutGrid, LogIn, MessageCircle, ShieldCheck, Sparkles, UserRound } from 'lucide-react'
import Logo from './Logo'

export const navItems = [
  { label: 'Home', icon: Home },
  { label: 'Marketplace', icon: LayoutGrid },
  { label: 'Messages', icon: MessageCircle, requiresAuth: true },
  { label: 'Profile', icon: UserRound, requiresAuth: true },
  { label: 'Admin', icon: ShieldCheck, requiresAuth: true, roles: ['ADMIN'] },
]

export function visibleNavItems(user) {
  return navItems.filter((item) => !item.roles || item.roles.includes(user?.role))
}

export function NavButton({ item, active, onClick, mobile = false }) {
  const Icon = item.icon
  return <button onClick={onClick} className={`flex items-center justify-center gap-3 rounded-xl text-sm font-bold transition ${mobile ? 'w-1/4 flex-col gap-1 px-1 py-1 text-[10px]' : 'justify-start px-3 py-3'} ${active ? 'bg-[#eaf3ff] text-[#087cf5] dark:bg-blue-950/60 dark:text-blue-300' : 'text-[#7b879b] hover:bg-[#f5f7fa] hover:text-[#172238] dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-100'}`}><Icon size={mobile ? 20 : 19} strokeWidth={active ? 2.5 : 2} /><span>{item.label}</span></button>
}

export function DesktopSidebar({ activeNav, onNavigate, onAuth, items, user }) {
  return <aside className="fixed inset-y-0 left-0 z-20 hidden w-[248px] border-r border-[#e3e8f0] bg-white px-5 py-7 dark:border-slate-800 dark:bg-slate-950 lg:flex lg:flex-col"><Logo onClick={() => onNavigate({ label: 'Home' })} /><div className="mt-12 flex flex-col gap-2">{items.map((item) => <NavButton key={item.label} item={item} active={activeNav === item.label} onClick={() => onNavigate(item)} />)}</div><div className="mt-auto rounded-2xl bg-[#eaf3ff] p-4 dark:bg-blue-950/50"><div className="mb-3 flex h-9 w-9 items-center justify-center rounded-xl bg-[#087cf5] text-white"><Sparkles size={18} /></div><p className="text-sm font-bold text-[#123563] dark:text-blue-100">Pass it forward</p><p className="mt-1 text-xs leading-5 text-[#527099] dark:text-blue-200/70">Every secondhand find keeps something useful in motion.</p></div>{!user && <button onClick={onAuth} className="mt-5 flex items-center gap-3 rounded-xl px-3 py-3 text-sm font-semibold text-[#63708a] transition hover:bg-[#f5f7fa] hover:text-[#087cf5] dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-blue-300"><LogIn size={18} /> Sign in</button>}</aside>
}
