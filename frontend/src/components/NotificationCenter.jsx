import { useEffect, useRef, useState } from 'react'
import { Bell, Check, LoaderCircle } from 'lucide-react'
import { apiErrorMessage, apiFetch } from '../lib/api'

const PAGE_SIZE = 20

export default function NotificationCenter({ user, onAuth, onSelect }) {
  const [isOpen, setIsOpen] = useState(false)
  const [notifications, setNotifications] = useState([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [nextCursor, setNextCursor] = useState(null)
  const [hasMore, setHasMore] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [isLoadingMore, setIsLoadingMore] = useState(false)
  const [error, setError] = useState('')
  const centerRef = useRef(null)

  useEffect(() => {
    if (!user) return

    async function refreshUnreadCount() {
      try {
        const response = await apiFetch('/notifications/unread-count')
        if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not load notifications.'))
        const body = await response.json()
        setUnreadCount(body.unreadCount || 0)
        setError('')
      } catch (requestError) {
        setError(requestError.message || 'Could not load notifications.')
      }
    }

    refreshUnreadCount()
    window.addEventListener('focus', refreshUnreadCount)
    return () => window.removeEventListener('focus', refreshUnreadCount)
  }, [user])

  useEffect(() => {
    function closeOnOutsideClick(event) {
      if (!centerRef.current?.contains(event.target)) setIsOpen(false)
    }
    document.addEventListener('mousedown', closeOnOutsideClick)
    return () => document.removeEventListener('mousedown', closeOnOutsideClick)
  }, [])

  async function loadNotifications(cursor = null, append = false) {
    setError('')
    if (append) setIsLoadingMore(true)
    else setIsLoading(true)
    try {
      const params = new URLSearchParams({ limit: String(PAGE_SIZE) })
      if (cursor) params.set('cursor', cursor)
      const response = await apiFetch(`/notifications?${params.toString()}`)
      if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not load notifications.'))
      const body = await response.json()
      setNotifications((current) => append ? [...current, ...body.items] : body.items)
      setUnreadCount(body.unreadCount || 0)
      setNextCursor(body.nextCursor || null)
      setHasMore(Boolean(body.hasMore))
    } catch (requestError) {
      setError(requestError.message || 'Could not load notifications.')
    } finally {
      setIsLoading(false)
      setIsLoadingMore(false)
    }
  }

  async function openCenter() {
    if (!user) {
      onAuth()
      return
    }
    const shouldOpen = !isOpen
    setIsOpen(shouldOpen)
    if (shouldOpen) await loadNotifications()
  }

  function selectNotification(notification) {
    setIsOpen(false)
    onSelect(notification)
    if (!notification.read) void markNotificationRead(notification.id)
  }

  async function markNotificationRead(notificationId) {
    try {
      const response = await apiFetch(`/notifications/${notificationId}/read`, { method: 'PATCH' })
      if (!response.ok) return
      setNotifications((current) => current.map((item) => item.id === notificationId ? { ...item, read: true } : item))
      setUnreadCount((current) => Math.max(0, current - 1))
    } catch {
      // Keep local state unchanged; opening the center again reloads it from the server.
    }
  }

  async function markAllRead() {
    setError('')
    try {
      const response = await apiFetch('/notifications/read-all', { method: 'PATCH' })
      if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not mark notifications as read.'))
      setNotifications((current) => current.map((item) => ({ ...item, read: true })))
      setUnreadCount(0)
    } catch (requestError) {
      setError(requestError.message || 'Could not mark notifications as read.')
    }
  }

  return <div ref={centerRef} className="relative">
    <button
      type="button"
      aria-label={unreadCount > 0 ? `Notifications, ${unreadCount} unread` : 'Notifications'}
      aria-expanded={isOpen}
      onClick={openCenter}
      className="relative rounded-xl p-2.5 text-[#64728a] transition hover:bg-[#f5f7fa] dark:text-slate-300 dark:hover:bg-slate-800"
    >
      <Bell size={19} />
      {unreadCount > 0 && <span className="absolute -right-1 -top-1 flex min-h-4 min-w-4 items-center justify-center rounded-full bg-[#ff6b57] px-1 text-[10px] font-bold text-white">{unreadCount > 99 ? '99+' : unreadCount}</span>}
    </button>
    {isOpen && <section aria-label="Notification history" className="absolute right-0 top-full z-50 mt-3 w-[min(360px,calc(100vw-24px))] overflow-hidden rounded-2xl border border-[#e3e8f0] bg-white shadow-xl dark:border-slate-700 dark:bg-slate-900">
      <div className="flex items-center justify-between border-b border-[#eef1f5] px-4 py-3 dark:border-slate-800">
        <div><h2 className="text-sm font-extrabold text-[#172238] dark:text-white">Notifications</h2><p aria-live="polite" className="text-xs text-[#718097] dark:text-slate-400">{unreadCount} unread</p></div>
        <button type="button" onClick={markAllRead} disabled={unreadCount === 0} className="inline-flex min-h-9 items-center gap-1.5 rounded-lg px-2 text-xs font-bold text-[#087cf5] hover:bg-[#eaf3ff] disabled:cursor-not-allowed disabled:opacity-50 dark:hover:bg-blue-950/50"><Check size={14} />Mark all read</button>
      </div>
      {error && <p role="alert" className="mx-3 mt-3 rounded-lg bg-rose-50 px-3 py-2 text-xs text-rose-700 dark:bg-rose-950/30 dark:text-rose-300">{error}</p>}
      <div className="max-h-[min(60vh,480px)] overflow-y-auto">
        {isLoading
          ? <div className="flex items-center justify-center gap-2 p-8 text-sm text-[#718097] dark:text-slate-400"><LoaderCircle size={16} className="animate-spin" />Loading notifications…</div>
          : notifications.length === 0
            ? <p className="p-8 text-center text-sm text-[#718097] dark:text-slate-400">You're all caught up.</p>
            : notifications.map((notification) => <button
              key={notification.id}
              type="button"
              onClick={() => selectNotification(notification)}
              className={`block w-full border-b border-[#eef1f5] px-4 py-3 text-left transition hover:bg-[#f7f9fc] dark:border-slate-800 dark:hover:bg-slate-800/70 ${notification.read ? '' : 'bg-blue-50/60 dark:bg-blue-950/20'}`}
            >
              <span className="flex items-start gap-3">
                {!notification.read && <span aria-hidden="true" className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-[#087cf5]" />}
                <span className="min-w-0 flex-1">
                  <span className="block text-sm font-bold text-[#172238] dark:text-white">{notification.title}</span>
                  <span className="mt-1 block break-words text-xs leading-5 text-[#718097] dark:text-slate-400">{notification.content}</span>
                  <time className="mt-1.5 block text-[11px] text-[#92a0b4]">{notification.createdAt ? new Date(notification.createdAt).toLocaleString() : ''}</time>
                </span>
              </span>
            </button>)}
      </div>
      {hasMore && <div className="p-3 text-center"><button type="button" onClick={() => loadNotifications(nextCursor, true)} disabled={isLoadingMore} className="inline-flex min-h-9 items-center gap-2 rounded-lg px-3 text-xs font-bold text-[#087cf5] hover:bg-[#eaf3ff] disabled:opacity-60 dark:hover:bg-blue-950/50">{isLoadingMore && <LoaderCircle size={14} className="animate-spin" />}Load more</button></div>}
    </section>}
  </div>
}
