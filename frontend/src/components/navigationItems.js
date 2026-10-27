import { Home, LayoutGrid, MessageCircle, ShieldCheck } from 'lucide-react'

export const navItems = [
  { label: 'Home', icon: Home },
  { label: 'Marketplace', icon: LayoutGrid },
  { label: 'Messages', icon: MessageCircle, requiresAuth: true },
  { label: 'Management', icon: ShieldCheck, requiresAuth: true, roles: ['MANAGER', 'ADMIN'] },
]

export function visibleNavItems(user) {
  return navItems.filter((item) => !item.roles || item.roles.includes(user?.role))
}
