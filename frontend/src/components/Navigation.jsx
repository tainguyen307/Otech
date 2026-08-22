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
  return <button onClick={onClick} className={`flex items-center justify-center gap-3 rounded-xl text-sm font-bold transition ${mobile ? 'w-1/4 flex-col gap-1 px-1 py-1 text-[10px]' : 'justify-start px-3 py-3'} ${active ? 'bg-[#eaf3ff] text-[#087cf5]' : 'text-[#7b879b] hover:bg-[#f5f7fa] hover:text-[#172238]'}`}><Icon size={mobile ? 20 : 19} strokeWidth={active ? 2.5 : 2} /><span>{item.label}</span></button>
}

export function DesktopSidebar({ activeNav, onNavigate, onAuth, items }) {
  return <aside className="fixed inset-y-0 left-0 z-20 hidden w-[248px] border-r border-[#e3e8f0] bg-white px-5 py-7 lg:flex lg:flex-col"><Logo /><div className="mt-12 flex flex-col gap-2">{items.map((item) => <NavButton key={item.label} item={item} active={activeNav === item.label} onClick={() => onNavigate(item)} />)}</div><div className="mt-auto rounded-2xl bg-[#eaf3ff] p-4"><div className="mb-3 flex h-9 w-9 items-center justify-center rounded-xl bg-[#087cf5] text-white"><Sparkles size={18} /></div><p className="text-sm font-bold text-[#123563]">Pass it forward</p><p className="mt-1 text-xs leading-5 text-[#527099]">Every secondhand find keeps something useful in motion.</p></div><button onClick={onAuth} className="mt-5 flex items-center gap-3 rounded-xl px-3 py-3 text-sm font-semibold text-[#63708a] transition hover:bg-[#f5f7fa] hover:text-[#087cf5]"><LogIn size={18} /> Sign in</button></aside>
}
