import { useEffect, useRef, useState } from 'react'
import { ChevronDown, Home, LogOut, Moon, Sun, UserRound } from 'lucide-react'

function initials(name) {
  return name.split(' ').map((part) => part[0]).join('').slice(0, 2).toUpperCase()
}

export default function ProfileMenu({ user, darkMode, onToggleTheme, onSignOut, onProfile, onHome }) {
  const [isOpen, setIsOpen] = useState(false)
  const menuRef = useRef(null)
  const displayName = user.fullName || 'Otech member'

  useEffect(() => {
    function closeMenu(event) {
      if (!menuRef.current?.contains(event.target)) setIsOpen(false)
    }
    document.addEventListener('mousedown', closeMenu)
    return () => document.removeEventListener('mousedown', closeMenu)
  }, [])

  return <div className="relative" ref={menuRef}>
    <button aria-expanded={isOpen} aria-haspopup="menu" onClick={() => setIsOpen((value) => !value)} className="flex h-11 items-center gap-2 rounded-xl px-2 transition hover:bg-[#f5f7fa] dark:hover:bg-slate-800">
      <span className="flex h-9 w-9 items-center justify-center overflow-hidden rounded-full bg-[#dcecff] text-xs font-extrabold text-[#087cf5]">{user.avatarUrl ? <img src={user.avatarUrl} alt="" className="h-full w-full object-cover" /> : initials(displayName)}</span>
      <span className="hidden max-w-[130px] truncate text-left text-sm font-bold text-[#172238] dark:text-white sm:block">{displayName}</span>
      <ChevronDown size={16} className={`text-[#8290a4] transition ${isOpen ? 'rotate-180' : ''}`} />
    </button>
    {isOpen && <div role="menu" className="absolute right-0 top-14 w-64 rounded-2xl border border-[#e3e8f0] bg-white p-2 shadow-xl dark:border-slate-700 dark:bg-slate-900">
      <div className="border-b border-[#edf0f4] px-3 py-3 dark:border-slate-800"><p className="truncate text-sm font-extrabold text-[#172238] dark:text-white">{displayName}</p><p className="mt-0.5 text-xs text-[#8290a4]">Account menu</p></div>
      <button role="menuitem" onClick={() => { setIsOpen(false); onProfile() }} className="mt-2 flex w-full items-center gap-3 rounded-xl bg-[#eaf3ff] px-3 py-2.5 text-sm font-bold text-[#087cf5] transition hover:bg-[#dcecff] dark:bg-blue-950/50 dark:text-blue-300 dark:hover:bg-blue-900/60"><UserRound size={17} /> Profile</button>
      <button role="menuitem" onClick={() => { setIsOpen(false); onHome() }} className="mt-1 flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold text-[#526177] transition hover:bg-[#f5f7fa] dark:text-slate-300 dark:hover:bg-slate-800"><Home size={17} /> Home</button>
      <button role="menuitem" onClick={() => { setIsOpen(false); onToggleTheme() }} className="mt-1 flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold text-[#526177] transition hover:bg-[#f5f7fa] dark:text-slate-300 dark:hover:bg-slate-800">{darkMode ? <Sun size={17} /> : <Moon size={17} />} {darkMode ? 'Light mode' : 'Dark mode'}</button>
      <button role="menuitem" onClick={() => { setIsOpen(false); onSignOut() }} className="mt-1 flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-semibold text-[#d9485f] transition hover:bg-[#fff1f2] dark:hover:bg-rose-950/40"><LogOut size={17} /> Sign out</button>
    </div>}
  </div>
}
