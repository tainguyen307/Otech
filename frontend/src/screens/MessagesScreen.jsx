import { useEffect, useRef, useState } from 'react'
import { Client } from '@stomp/stompjs'
import { Check, CheckCheck, ChevronLeft, LoaderCircle, MessageCircle, RefreshCw, Send, Tag, Wifi, WifiOff } from 'lucide-react'
import { API_URL, apiErrorMessage, apiFetch } from '../lib/api'

function websocketUrl() {
  const api = new URL(API_URL)
  return `${api.protocol === 'https:' ? 'wss:' : 'ws:'}//${api.host}/ws`
}

function formatTime(value) {
  if (!value) return ''
  return new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit' }).format(new Date(value))
}

function formatConversationTime(value) {
  if (!value) return ''
  const date = new Date(value)
  const now = new Date()
  return date.toDateString() === now.toDateString()
    ? formatTime(value)
    : new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit' }).format(date)
}

function upsertMessage(messages, incoming) {
  const existingIndex = messages.findIndex((message) =>
    message.id === incoming.id || message.clientMessageId === incoming.clientMessageId)
  if (existingIndex < 0) return [...messages, incoming]
  return messages.map((message, index) => index === existingIndex ? { ...message, ...incoming, pending: false } : message)
}

export default function MessagesScreen({ user, initialConversationId, onAuth }) {
  const [conversations, setConversations] = useState([])
  const [selectedId, setSelectedId] = useState('')
  const [messages, setMessages] = useState([])
  const [draft, setDraft] = useState('')
  const [nextCursor, setNextCursor] = useState(null)
  const [hasMore, setHasMore] = useState(false)
  const [isConnected, setIsConnected] = useState(false)
  const [isLoadingConversations, setIsLoadingConversations] = useState(true)
  const [isLoadingMessages, setIsLoadingMessages] = useState(false)
  const [isLoadingOlder, setIsLoadingOlder] = useState(false)
  const [isSending, setIsSending] = useState(false)
  const [error, setError] = useState('')
  const [chatError, setChatError] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const clientRef = useRef(null)
  const selectedIdRef = useRef('')
  const bottomRef = useRef(null)
  const shouldScrollRef = useRef(true)
  const inputRef = useRef(null)
  const pendingMessagesRef = useRef(new Map())
  const appliedInitialIdRef = useRef('')
  const conversation = conversations.find((item) => item.id === selectedId)

  useEffect(() => { selectedIdRef.current = selectedId }, [selectedId])

  useEffect(() => {
    const token = localStorage.getItem('token')
    const pendingMessages = pendingMessagesRef.current
    if (!token) {
      onAuth()
      return undefined
    }

    let active = true
    async function loadConversations() {
      setIsLoadingConversations(true)
      try {
        const response = await apiFetch('/conversations')
        if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not load conversations.'))
        const body = await response.json()
        if (!active) return
        const items = Array.isArray(body) ? body : []
        setConversations(items)
        setError('')
        if (initialConversationId && items.some((item) => item.id === initialConversationId)) {
          setSelectedId(initialConversationId)
        } else if (!selectedIdRef.current && items.length > 0) {
          setSelectedId(items[0].id)
        }
      } catch (requestError) {
        if (active) setError(requestError.message || 'Could not load conversations.')
      } finally {
        if (active) setIsLoadingConversations(false)
      }
    }

    loadConversations()
    const client = new Client({
      brokerURL: websocketUrl(),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 2000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        if (!active) return
        setIsConnected(true)
        client.subscribe('/user/queue/messages', (frame) => {
          try {
            const incoming = JSON.parse(frame.body)
            const pending = pendingMessagesRef.current.get(incoming.clientMessageId)
            if (pending) {
              window.clearTimeout(pending.timer)
              pendingMessagesRef.current.delete(incoming.clientMessageId)
              setDraft((current) => current === pending.content ? '' : current)
              shouldScrollRef.current = true
            }
            setConversations((current) => {
              const existing = current.find((item) => item.id === incoming.conversationId)
              if (!existing) return current
              const selected = selectedIdRef.current === incoming.conversationId
              const preview = {
                content: incoming.content,
                senderId: incoming.senderId,
                status: incoming.status,
                createdAt: incoming.createdAt,
              }
              return [
                { ...existing, lastMessage: preview, unreadCount: selected || incoming.senderId === user.id ? 0 : existing.unreadCount + 1 },
                ...current.filter((item) => item.id !== incoming.conversationId),
              ]
            })
            if (selectedIdRef.current === incoming.conversationId) {
              setMessages((current) => upsertMessage(current, incoming))
              if (incoming.senderId !== user.id) {
                client.publish({
                  destination: `/app/conversations/${incoming.conversationId}/read`,
                  body: '{}',
                })
              }
            }
          } catch {
            setChatError('Received an unreadable chat update. Refresh the conversation to continue.')
          }
        })
        client.subscribe('/user/queue/message-status', (frame) => {
          try {
            const update = JSON.parse(frame.body)
            setMessages((current) => current.map((message) =>
              message.senderId === user.id && update.readerId !== user.id
                ? { ...message, status: update.status }
                : message))
            setConversations((current) => current.map((item) => item.id === update.conversationId && item.lastMessage
              ? { ...item, lastMessage: { ...item.lastMessage, status: update.status } }
              : item))
          } catch {
            setChatError('Could not update message delivery status.')
          }
        })
        client.subscribe('/user/queue/errors', (frame) => {
          try {
            const body = JSON.parse(frame.body)
            const pending = pendingMessagesRef.current.entries().next().value
            if (pending) {
              window.clearTimeout(pending[1].timer)
              pendingMessagesRef.current.delete(pending[0])
              setDraft((current) => current || pending[1].content)
            }
            setChatError(body.message || 'The chat request could not be completed.')
          } catch {
            setChatError('The chat request could not be completed.')
          }
        })
        loadConversations()
      },
      onWebSocketClose: () => {
        if (active) setIsConnected(false)
      },
      onWebSocketError: () => {
        if (active) setIsConnected(false)
      },
      onStompError: (frame) => {
        if (active) setChatError(frame.headers.message || 'Could not connect to live chat.')
      },
    })
    clientRef.current = client
    client.activate()

    return () => {
      active = false
      setIsConnected(false)
      clientRef.current = null
      pendingMessages.forEach((pending) => window.clearTimeout(pending.timer))
      pendingMessages.clear()
      void client.deactivate()
    }
  }, [initialConversationId, onAuth, refreshKey, user.id])

  useEffect(() => {
    if (!selectedId) {
      return undefined
    }
    const controller = new AbortController()
    async function loadMessages() {
      setIsLoadingMessages(true)
      setChatError('')
      setMessages([])
      shouldScrollRef.current = true
      try {
        const response = await apiFetch(`/conversations/${selectedId}/messages?limit=50`, { signal: controller.signal })
        if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not load messages.'))
        const body = await response.json()
        if (controller.signal.aborted) return
        setMessages(Array.isArray(body.items) ? body.items : [])
        setNextCursor(body.nextCursor || null)
        setHasMore(Boolean(body.hasMore))
        setConversations((current) => current.map((item) => item.id === selectedId ? { ...item, unreadCount: 0 } : item))
      } catch (requestError) {
        if (requestError.name !== 'AbortError') setChatError(requestError.message || 'Could not load messages.')
      } finally {
        if (!controller.signal.aborted) setIsLoadingMessages(false)
      }
    }
    loadMessages()
    return () => controller.abort()
  }, [selectedId])

  useEffect(() => {
    if (initialConversationId && appliedInitialIdRef.current !== initialConversationId
      && conversations.some((item) => item.id === initialConversationId)) {
      appliedInitialIdRef.current = initialConversationId
      setSelectedId(initialConversationId)
    }
  }, [conversations, initialConversationId])

  useEffect(() => {
    if (shouldScrollRef.current) {
      bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
      shouldScrollRef.current = false
    }
  }, [messages])

  async function loadOlder() {
    if (!selectedId || !nextCursor || isLoadingOlder) return
    setIsLoadingOlder(true)
    setChatError('')
    shouldScrollRef.current = false
    try {
      const params = new URLSearchParams({ limit: '50', cursor: nextCursor })
      const response = await apiFetch(`/conversations/${selectedId}/messages?${params}`)
      if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not load earlier messages.'))
      const body = await response.json()
      setMessages((current) => [...(body.items || []), ...current])
      setNextCursor(body.nextCursor || null)
      setHasMore(Boolean(body.hasMore))
    } catch (requestError) {
      setChatError(requestError.message || 'Could not load earlier messages.')
    } finally {
      setIsLoadingOlder(false)
    }
  }

  function updateConversationFromMessage(sentMessage) {
    setConversations((current) => {
      const existing = current.find((item) => item.id === sentMessage.conversationId)
      if (!existing) return current
      const preview = {
        content: sentMessage.content,
        senderId: sentMessage.senderId,
        status: sentMessage.status,
        createdAt: sentMessage.createdAt,
      }
      return [
        { ...existing, lastMessage: preview, unreadCount: 0 },
        ...current.filter((item) => item.id !== sentMessage.conversationId),
      ]
    })
  }

  async function sendMessage(event) {
    event.preventDefault()
    const content = draft.trim()
    if (!content || !selectedId || isSending) return
    if (content.length > 5000) {
      setChatError('Messages can be up to 5,000 characters.')
      return
    }
    const request = { clientMessageId: crypto.randomUUID(), content }
    setIsSending(true)
    setChatError('')
    try {
      if (isConnected && clientRef.current?.connected) {
        if ([...pendingMessagesRef.current.values()].some((pending) => pending.content === content)) {
          setChatError('That message is still being delivered. Please wait a moment before trying again.')
          return
        }
        const optimistic = {
          id: request.clientMessageId,
          clientMessageId: request.clientMessageId,
          conversationId: selectedId,
          senderId: user.id,
          senderName: user.fullName,
          content,
          status: 'SENT',
          createdAt: new Date().toISOString(),
          pending: true,
        }
        shouldScrollRef.current = true
        setMessages((current) => upsertMessage(current, optimistic))
        updateConversationFromMessage(optimistic)
        const timer = window.setTimeout(async () => {
          if (!pendingMessagesRef.current.has(request.clientMessageId)) return
          try {
            const response = await apiFetch(`/conversations/${selectedId}/messages`, {
              method: 'POST',
              body: JSON.stringify(request),
            })
            if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not send your message.'))
            const sentMessage = await response.json()
            setMessages((current) => upsertMessage(current, sentMessage))
            updateConversationFromMessage(sentMessage)
            const pending = pendingMessagesRef.current.get(request.clientMessageId)
            if (pending) {
              window.clearTimeout(pending.timer)
              pendingMessagesRef.current.delete(request.clientMessageId)
              setDraft((current) => current === pending.content ? '' : current)
            }
          } catch (sendError) {
            pendingMessagesRef.current.delete(request.clientMessageId)
            setDraft((current) => current || content)
            setChatError(sendError.message || 'Could not send your message.')
          }
        }, 8000)
        pendingMessagesRef.current.set(request.clientMessageId, { content, timer })
        clientRef.current.publish({
          destination: `/app/conversations/${selectedId}/messages`,
          body: JSON.stringify(request),
        })
      } else {
        const response = await apiFetch(`/conversations/${selectedId}/messages`, {
          method: 'POST',
          body: JSON.stringify(request),
        })
        if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not send your message.'))
        const sentMessage = await response.json()
        setMessages((current) => upsertMessage(current, sentMessage))
        updateConversationFromMessage(sentMessage)
        setDraft('')
        shouldScrollRef.current = true
      }
      inputRef.current?.focus()
    } catch (sendError) {
      setChatError(sendError.message || 'Could not send your message.')
    } finally {
      setIsSending(false)
    }
  }

  function selectConversation(id) {
    setSelectedId(id)
    setChatError('')
  }

  const formatStatus = (message) => {
    if (message.status === 'READ') return <span aria-label="Read" title="Read"><CheckCheck size={15} className="text-[#087cf5]" /></span>
    if (message.status === 'DELIVERED') return <span aria-label="Delivered" title="Delivered"><CheckCheck size={15} /></span>
    return <span aria-label="Sent" title="Sent"><Check size={15} /></span>
  }

  return <main className="px-3 pb-24 pt-5 sm:px-5 lg:ml-[248px] lg:px-10 lg:pb-8 lg:pt-7">
    <div className="mx-auto max-w-[1180px]">
      <div className="mb-5 flex items-end justify-between gap-4"><div><p className="mb-1 text-xs font-bold uppercase tracking-[0.18em] text-[#087cf5]">Otech inbox</p><h1 className="text-2xl font-extrabold tracking-tight text-[#172238] dark:text-white sm:text-3xl">Messages</h1></div><div aria-live="polite" className={`inline-flex items-center gap-2 rounded-full px-3 py-2 text-xs font-bold ${isConnected ? 'bg-emerald-50 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300' : 'bg-amber-50 text-amber-700 dark:bg-amber-950/40 dark:text-amber-300'}`}>{isConnected ? <Wifi size={14} /> : <WifiOff size={14} />}{isConnected ? 'Live' : 'Reconnecting'}</div></div>
      <div className="grid min-h-[min(720px,calc(100vh-190px))] overflow-hidden rounded-2xl border border-[#e3e8f0] bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900 lg:grid-cols-[320px_minmax(0,1fr)]">
        <aside className={`${selectedId ? 'hidden' : 'flex'} min-h-[min(720px,calc(100vh-190px))] flex-col border-r border-[#e3e8f0] dark:border-slate-800 lg:flex`}>
          <div className="border-b border-[#eef1f5] p-4 dark:border-slate-800"><h2 className="font-extrabold text-[#172238] dark:text-white">Your conversations</h2><p className="mt-1 text-xs text-[#718097] dark:text-slate-400">Keep the details of a good find moving.</p></div>
          {error && <div role="alert" className="m-3 rounded-xl bg-rose-50 p-3 text-xs text-rose-700 dark:bg-rose-950/30 dark:text-rose-300">{error}<button onClick={() => setRefreshKey((value) => value + 1)} className="ml-2 font-bold underline">Retry</button></div>}
          {isLoadingConversations && conversations.length === 0 && <div className="flex flex-1 items-center justify-center gap-2 text-sm text-[#718097] dark:text-slate-400"><LoaderCircle size={17} className="animate-spin" />Loading messages…</div>}
          {!isLoadingConversations && !error && conversations.length === 0 && <div className="flex flex-1 flex-col items-center justify-center px-6 text-center"><span className="flex h-12 w-12 items-center justify-center rounded-2xl bg-[#eaf3ff] text-[#087cf5] dark:bg-blue-950/50"><MessageCircle size={22} /></span><h3 className="mt-4 font-extrabold text-[#172238] dark:text-white">No conversations yet</h3><p className="mt-2 text-sm leading-6 text-[#718097] dark:text-slate-400">Message a seller from a listing to start a conversation.</p></div>}
          <ul className="flex-1 overflow-y-auto">{conversations.map((item) => <li key={item.id}><button onClick={() => selectConversation(item.id)} aria-current={item.id === selectedId ? 'true' : undefined} className={`flex min-h-[82px] w-full items-center gap-3 border-b border-[#f0f2f5] px-4 py-3 text-left transition hover:bg-[#f7f9fc] dark:border-slate-800 dark:hover:bg-slate-800/70 ${item.id === selectedId ? 'bg-[#f2f7ff] dark:bg-blue-950/25' : ''}`}>
            {item.participant?.avatarUrl ? <img src={item.participant.avatarUrl} alt="" className="h-11 w-11 shrink-0 rounded-full object-cover" /> : <span aria-hidden="true" className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-[#eaf3ff] font-bold text-[#087cf5] dark:bg-blue-950/50">{item.participant?.fullName?.[0] || 'O'}</span>}
            <span className="min-w-0 flex-1"><span className="flex items-center justify-between gap-2"><span className="truncate text-sm font-extrabold text-[#172238] dark:text-white">{item.participant?.fullName || 'Otech member'}</span><span className="shrink-0 text-[10px] text-[#8a96a8]">{formatConversationTime(item.lastMessage?.createdAt || item.createdAt)}</span></span><span className="mt-1 block truncate text-xs text-[#718097] dark:text-slate-400">{item.product?.title ? `${item.product.title} · ` : ''}{item.lastMessage?.content || 'Conversation started'}</span></span>
            {item.unreadCount > 0 && <span aria-label={`${item.unreadCount} unread messages`} className="flex h-5 min-w-5 items-center justify-center rounded-full bg-[#087cf5] px-1.5 text-[10px] font-bold text-white">{item.unreadCount > 99 ? '99+' : item.unreadCount}</span>}
          </button></li>)}</ul>
        </aside>

        <section className={`${selectedId ? 'flex' : 'hidden'} min-h-[min(720px,calc(100vh-190px))] min-w-0 flex-col lg:flex`}>
          {!conversation && <div className="hidden flex-1 flex-col items-center justify-center text-center lg:flex"><span className="flex h-14 w-14 items-center justify-center rounded-2xl bg-[#eaf3ff] text-[#087cf5] dark:bg-blue-950/50"><MessageCircle size={24} /></span><h2 className="mt-4 font-extrabold text-[#172238] dark:text-white">Choose a conversation</h2><p className="mt-2 text-sm text-[#718097] dark:text-slate-400">Your messages will appear here.</p></div>}
          {conversation && <>
            <header className="flex min-h-[74px] items-center gap-3 border-b border-[#eef1f5] px-3 py-3 dark:border-slate-800 sm:px-5">
              <button onClick={() => setSelectedId('')} aria-label="Back to conversations" className="rounded-xl p-2 text-[#718097] hover:bg-[#f5f7fa] dark:text-slate-300 dark:hover:bg-slate-800 lg:hidden"><ChevronLeft size={20} /></button>
              {conversation.participant?.avatarUrl ? <img src={conversation.participant.avatarUrl} alt="" className="h-10 w-10 rounded-full object-cover" /> : <span className="flex h-10 w-10 items-center justify-center rounded-full bg-[#eaf3ff] font-bold text-[#087cf5] dark:bg-blue-950/50">{conversation.participant?.fullName?.[0] || 'O'}</span>}
              <div className="min-w-0 flex-1"><h2 className="truncate text-sm font-extrabold text-[#172238] dark:text-white">{conversation.participant?.fullName || 'Otech member'}</h2><p className="truncate text-xs text-[#718097] dark:text-slate-400">{conversation.product?.title ? `About ${conversation.product.title}` : 'Direct conversation'}</p></div>
              {conversation.product?.imageUrl && <img src={conversation.product.imageUrl} alt="" className="h-10 w-10 rounded-lg object-cover" />}
            </header>
            {conversation.product && <div className="flex items-center gap-3 border-b border-[#eef1f5] bg-[#fafbfd] px-4 py-2.5 dark:border-slate-800 dark:bg-slate-950/50"><Tag size={14} className="shrink-0 text-[#087cf5]" /><p className="truncate text-xs font-semibold text-[#526078] dark:text-slate-300">Listing: {conversation.product.title}</p></div>}
            {chatError && <div role="alert" className="mx-4 mt-3 flex items-start justify-between gap-3 rounded-xl bg-rose-50 px-3 py-2.5 text-xs text-rose-700 dark:bg-rose-950/30 dark:text-rose-300"><span>{chatError}</span><button onClick={() => setChatError('')} aria-label="Dismiss chat error" className="shrink-0 rounded p-0.5"><XIcon /></button></div>}
            <div aria-live="polite" aria-relevant="additions text" className="flex-1 space-y-3 overflow-y-auto bg-[#fbfcfe] px-3 py-4 dark:bg-slate-950/30 sm:px-5">
              {hasMore && <div className="text-center"><button onClick={loadOlder} disabled={isLoadingOlder} className="inline-flex min-h-9 items-center gap-2 rounded-full border border-[#dfe6ef] bg-white px-3 text-xs font-bold text-[#526078] disabled:opacity-60 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300">{isLoadingOlder && <LoaderCircle size={13} className="animate-spin" />}Load earlier messages</button></div>}
              {isLoadingMessages && <div className="flex justify-center py-8 text-sm text-[#718097]"><LoaderCircle size={17} className="animate-spin" /></div>}
              {!isLoadingMessages && messages.length === 0 && <p className="py-8 text-center text-sm text-[#718097] dark:text-slate-400">Say hello and make the first move.</p>}
              {messages.map((message) => {
                const own = message.senderId === user.id
                return <div key={message.id || message.clientMessageId} className={`flex ${own ? 'justify-end' : 'justify-start'}`}>
                  <div className={`max-w-[85%] rounded-2xl px-3.5 py-2.5 sm:max-w-[72%] ${own ? 'rounded-br-md bg-[#087cf5] text-white' : 'rounded-bl-md border border-[#e8edf3] bg-white text-[#172238] dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100'}`}>
                    <p className="whitespace-pre-wrap break-words text-sm leading-6">{message.content}</p>
                    <div className={`mt-1 flex items-center justify-end gap-1.5 text-[10px] ${own ? 'text-blue-100' : 'text-[#8a96a8]'}`}><time dateTime={message.createdAt}>{formatTime(message.createdAt)}</time>{own && formatStatus(message)}</div>
                  </div>
                </div>
              })}
              <div ref={bottomRef} />
            </div>
            <form onSubmit={sendMessage} className="border-t border-[#eef1f5] bg-white p-3 dark:border-slate-800 dark:bg-slate-900 sm:p-4">
              <div className="flex items-end gap-2">
                <label className="min-w-0 flex-1"><span className="sr-only">Write a message</span><textarea ref={inputRef} rows="1" maxLength="5000" value={draft} onChange={(event) => setDraft(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter' && !event.shiftKey) { event.preventDefault(); event.currentTarget.form?.requestSubmit() } }} placeholder="Write a message…" className="max-h-32 min-h-11 w-full resize-y rounded-xl border border-[#dfe6ef] bg-[#f7f9fc] px-3.5 py-3 text-sm text-[#172238] outline-none placeholder:text-[#9aa6b8] focus:border-[#087cf5] focus:ring-2 focus:ring-blue-100 dark:border-slate-700 dark:bg-slate-950 dark:text-white dark:focus:ring-blue-950" /></label>
                <button type="submit" disabled={!draft.trim() || isSending || isLoadingMessages} aria-label="Send message" className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-[#087cf5] text-white hover:bg-[#006cdf] disabled:cursor-not-allowed disabled:opacity-50"><Send size={17} /></button>
              </div>
              <p className="mt-2 flex items-center justify-between px-1 text-[10px] text-[#8a96a8]"><span>Enter to send · Shift+Enter for a new line</span><span>{draft.length}/5000</span></p>
            </form>
          </>}
        </section>
      </div>
      {!isConnected && <p className="mt-3 flex items-center gap-2 text-xs text-[#8a96a8]"><RefreshCw size={13} className="animate-spin" />Live connection will retry automatically. You can still send messages using secure HTTP fallback.</p>}
    </div>
  </main>
}

function XIcon() {
  return <span aria-hidden="true">×</span>
}
