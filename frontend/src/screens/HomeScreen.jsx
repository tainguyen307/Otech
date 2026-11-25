import { useCallback, useEffect, useState } from 'react'
import { ArrowRight, LoaderCircle, RefreshCw, ShieldCheck, Tag, UsersRound } from 'lucide-react'
import PostCard from '../components/PostCard'
import PostComposer from '../components/PostComposer'
import { apiErrorMessage, apiFetch } from '../lib/api'

export default function HomeScreen({ saved, onSave, onAuth, onOpenProfile, onMessage, onBrowseCategory, onBrowseGroupCategory, onBrowseMarket, onBrowseGroups, user }) {
  const [posts, setPosts] = useState([])
  const [categories, setCategories] = useState([])
  const [editingPost, setEditingPost] = useState(null)
  const [isSaving, setIsSaving] = useState(false)
  const [isLoading, setIsLoading] = useState(true)
  const [isLoadingCategories, setIsLoadingCategories] = useState(true)
  const [error, setError] = useState('')
  const [categoryError, setCategoryError] = useState('')
  const [actionError, setActionError] = useState('')
  const [notice, setNotice] = useState('')
  const [categoryRefreshKey, setCategoryRefreshKey] = useState(0)

  const fetchPosts = useCallback(async (signal) => {
    const response = await apiFetch('/posts', { signal })
    if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not load posts.'))
    const body = await response.json()
    return Array.isArray(body) ? body : []
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    let active = true
    fetchPosts(controller.signal)
      .then((items) => { if (active) setPosts(items) })
      .catch((requestError) => { if (active && requestError.name !== 'AbortError') setError(requestError.message || 'Could not load posts.') })
      .finally(() => { if (active) setIsLoading(false) })
    return () => controller.abort()
  }, [fetchPosts, user?.id])

  useEffect(() => {
    const controller = new AbortController()
    apiFetch('/categories', { signal: controller.signal })
      .then(async (response) => {
        if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not load categories.'))
        return response.json()
      })
      .then((items) => {
        if (!controller.signal.aborted) setCategories(Array.isArray(items) ? items : [])
      })
      .catch((requestError) => {
        if (!controller.signal.aborted && requestError.name !== 'AbortError') {
          setCategoryError(requestError.message || 'Could not load categories.')
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsLoadingCategories(false)
      })
    return () => controller.abort()
  }, [categoryRefreshKey])

  useEffect(() => {
    function scrollToPost() {
      const postId = window.location.hash.match(/^#post-(.+)$/)?.[1]
      if (!postId || isLoading) return
      document.getElementById(`post-${postId}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }

    scrollToPost()
    window.addEventListener('hashchange', scrollToPost)
    return () => window.removeEventListener('hashchange', scrollToPost)
  }, [isLoading])

  async function loadPosts() {
    setIsLoading(true)
    setError('')
    try {
      setPosts(await fetchPosts())
    } catch (requestError) {
      setError(requestError.message || 'Could not load posts.')
    } finally {
      setIsLoading(false)
    }
  }

  async function savePost(values) {
    setIsSaving(true)
    setActionError('')
    const isEditing = Boolean(editingPost)
    try {
      const response = await apiFetch(isEditing ? `/posts/${editingPost.id}` : '/posts', {
        method: isEditing ? 'PUT' : 'POST',
        body: JSON.stringify(values),
      })
      if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not save your post.'))
      const post = await response.json()
      setPosts((current) => isEditing
        ? current.map((item) => item.id === post.id ? post : item)
        : [post, ...current])
      setEditingPost(null)
    } finally {
      setIsSaving(false)
    }
  }

  async function toggleLike(post) {
    if (!user) {
      onAuth()
      return
    }
    setActionError('')
    try {
      const response = await apiFetch(`/posts/${post.id}/likes`, { method: post.likedByCurrentUser ? 'DELETE' : 'PUT' })
      if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not update your like.'))
      const engagement = await response.json()
      setPosts((current) => current.map((item) => item.id === post.id
        ? { ...item, likes: engagement.likes, comments: engagement.comments, shares: engagement.shares, likedByCurrentUser: engagement.likedByCurrentUser }
        : item))
    } catch (requestError) {
      setActionError(requestError.message || 'Could not update your like.')
    }
  }

  async function sharePost(post) {
    if (!user) {
      onAuth()
      return
    }
    setActionError('')
    setNotice('')
    const shareUrl = new URL(window.location.href)
    shareUrl.hash = `post-${post.id}`
    try {
      if (navigator.share) {
        await navigator.share({ title: post.title, text: post.copy, url: shareUrl.href })
      } else if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(shareUrl.href)
        setNotice('Post link copied to clipboard.')
      } else {
        throw new Error('Sharing is not available in this browser.')
      }
      const response = await apiFetch(`/posts/${post.id}/shares`, { method: 'PUT' })
      if (!response.ok) throw new Error(await apiErrorMessage(response, 'The post was shared, but its share count could not be updated.'))
      const engagement = await response.json()
      setPosts((current) => current.map((item) => item.id === post.id
        ? { ...item, shares: engagement.shares }
        : item))
    } catch (requestError) {
      if (requestError.name !== 'AbortError') setActionError(requestError.message || 'Could not share this post.')
    }
  }

  async function deletePost(post) {
    if (!window.confirm('Delete this post?')) return
    setActionError('')
    try {
      const response = await apiFetch(`/posts/${post.id}`, { method: 'DELETE' })
      if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not delete this post.'))
      setPosts((current) => current.filter((item) => item.id !== post.id))
      if (editingPost?.id === post.id) setEditingPost(null)
    } catch (requestError) {
      setActionError(requestError.message || 'Could not delete this post.')
    }
  }

  function updateCommentCount(postId) {
    setPosts((current) => current.map((item) => item.id === postId
      ? { ...item, comments: (item.comments || 0) + 1 }
      : item))
  }

  return <main className="px-5 pb-28 pt-7 lg:ml-[248px] lg:px-10 lg:pb-10">
    <div className="mx-auto grid max-w-[1180px] gap-8 xl:grid-cols-[minmax(0,1fr)_300px]">
      <section>
        <div className="mb-7">
          <p className="mb-2 text-xs font-bold uppercase tracking-[0.18em] text-[#087cf5]">Your community</p>
          <h1 className="text-3xl font-extrabold tracking-[-0.04em] text-[#172238] dark:text-white sm:text-4xl">Find your corner of Otech.</h1>
          <p className="mt-2 text-sm text-[#718097] dark:text-slate-400">Start with a category to find relevant listings and communities.</p>
        </div>
        <section aria-labelledby="category-heading" className="mb-8 rounded-2xl border border-[#e3e8f0] bg-white p-5 dark:border-slate-800 dark:bg-slate-900 sm:p-6">
          <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
            <div>
              <h2 id="category-heading" className="text-xl font-extrabold text-[#172238] dark:text-white">Browse by category</h2>
              <p className="mt-1 text-sm text-[#718097] dark:text-slate-400">A more focused way to discover secondhand finds.</p>
            </div>
            <div className="flex flex-wrap gap-2">
              <button type="button" onClick={onBrowseGroups} className="inline-flex min-h-10 items-center gap-2 rounded-xl border border-[#dfe6ef] px-3 text-xs font-bold text-[#526078] hover:bg-[#f5f7fa] dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800"><UsersRound size={15} />Explore groups</button>
              <button type="button" onClick={onBrowseMarket} className="inline-flex min-h-10 items-center gap-2 rounded-xl bg-[#eaf3ff] px-3 text-xs font-bold text-[#087cf5] hover:bg-blue-100 dark:bg-blue-950/50 dark:text-blue-300 dark:hover:bg-blue-950">All listings<ArrowRight size={14} /></button>
            </div>
          </div>
          {isLoadingCategories && <div className="flex min-h-20 items-center justify-center gap-2 text-sm text-[#718097] dark:text-slate-400"><LoaderCircle size={17} className="animate-spin text-[#087cf5]" />Loading categories…</div>}
          {categoryError && !isLoadingCategories && <div role="alert" className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700 dark:border-rose-900 dark:bg-rose-950/30 dark:text-rose-300"><span>{categoryError}</span><button type="button" onClick={() => { setIsLoadingCategories(true); setCategoryError(''); setCategoryRefreshKey((value) => value + 1) }} className="inline-flex min-h-9 items-center gap-2 rounded-lg bg-white px-3 text-xs font-bold text-rose-700 hover:bg-rose-100 dark:bg-slate-900 dark:text-rose-300 dark:hover:bg-rose-950"><RefreshCw size={13} />Try again</button></div>}
          {!isLoadingCategories && !categoryError && categories.length === 0 && <p className="rounded-xl bg-[#f5f7fa] px-4 py-5 text-sm text-[#718097] dark:bg-slate-800 dark:text-slate-400">Categories are not available yet. You can still browse all listings.</p>}
          {!isLoadingCategories && !categoryError && categories.length > 0 && <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-4">{categories.map((category) => <article key={category.id} className="rounded-xl border border-[#e3e8f0] bg-white p-3 dark:border-slate-700 dark:bg-slate-950"><div className="flex min-w-0 items-center gap-2"><span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-[#eaf3ff] text-[#087cf5] dark:bg-blue-950/50 dark:text-blue-300"><Tag size={16} /></span><h3 className="truncate text-sm font-bold text-[#34445d] dark:text-slate-200">{category.name}</h3></div><div className="mt-3 grid grid-cols-2 gap-2"><button type="button" onClick={() => onBrowseCategory(category.id)} className="inline-flex min-h-9 items-center justify-center gap-1 rounded-lg bg-[#eaf3ff] px-2 text-xs font-bold text-[#087cf5] hover:bg-blue-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#087cf5] dark:bg-blue-950/50 dark:text-blue-300 dark:hover:bg-blue-950">Listings<ArrowRight size={13} /></button><button type="button" onClick={() => onBrowseGroupCategory(category.id)} className="inline-flex min-h-9 items-center justify-center gap-1 rounded-lg border border-[#dfe6ef] px-2 text-xs font-bold text-[#526078] hover:bg-[#f5f7fa] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#087cf5] dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800">Groups<ArrowRight size={13} /></button></div></article>)}</div>}
        </section>
        {user
          ? <PostComposer key={editingPost?.id || 'new-post'} post={editingPost} isSaving={isSaving} onSave={savePost} onCancel={() => setEditingPost(null)} />
          : <button type="button" onClick={onAuth} className="mb-6 w-full rounded-2xl border border-[#dce8f5] bg-white p-4 text-left text-sm font-semibold text-[#718097] shadow-sm hover:border-[#087cf5] dark:border-slate-800 dark:bg-slate-900 dark:text-slate-300">Sign in to share a post with your community</button>}
        <div className="mb-4"><h2 className="text-xl font-extrabold text-[#172238] dark:text-white">Community posts</h2><p className="mt-1 text-sm text-[#718097] dark:text-slate-400">Conversations from across Otech.</p></div>
        {actionError && <p role="alert" className="mb-4 rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700 dark:border-rose-900 dark:bg-rose-950/30 dark:text-rose-300">{actionError}</p>}
        {notice && <p role="status" className="mb-4 rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-700 dark:border-emerald-900 dark:bg-emerald-950/30 dark:text-emerald-300">{notice}</p>}
        {isLoading && <div className="flex items-center justify-center gap-2 rounded-2xl border border-dashed border-[#e3e8f0] p-10 text-sm text-[#718097] dark:border-slate-700 dark:text-slate-400"><LoaderCircle size={18} className="animate-spin text-[#087cf5]" /> Loading community posts...</div>}
        {error && !isLoading && <div className="rounded-2xl border border-rose-200 bg-rose-50 p-5 text-sm text-rose-700 dark:border-rose-900 dark:bg-rose-950/30 dark:text-rose-300"><p>{error}</p><button type="button" onClick={() => loadPosts()} className="mt-3 inline-flex items-center gap-2 rounded-lg bg-white px-3 py-2 text-xs font-bold text-rose-700 shadow-sm hover:bg-rose-100 dark:bg-slate-900 dark:text-rose-300 dark:hover:bg-rose-950"><RefreshCw size={14} /> Try again</button></div>}
        {!isLoading && !error && posts.length === 0 && <div className="rounded-2xl border border-dashed border-[#e3e8f0] p-10 text-center text-sm text-[#718097] dark:border-slate-700 dark:text-slate-400">No community posts yet.</div>}
        {!isLoading && !error && <div className="space-y-6">{posts.map((post) => <PostCard
          key={post.id}
          post={post}
          isSaved={saved.includes(post.id)}
          onLike={() => toggleLike(post)}
          onSave={() => onSave(post.id)}
          onShare={() => sharePost(post)}
          onEdit={() => { setEditingPost(post); window.scrollTo({ top: 0, behavior: 'smooth' }) }}
          onDelete={() => deletePost(post)}
          onAuth={onAuth}
          onOpenProfile={onOpenProfile}
          onMessage={onMessage}
          onCommentAdded={() => updateCommentCount(post.id)}
          user={user}
        />)}</div>}
      </section>
      <aside className="hidden space-y-5 xl:block">
        <div className="rounded-2xl border border-[#e3e8f0] bg-white p-5 dark:border-slate-800 dark:bg-slate-900">
          <div className="mb-5 flex items-center justify-between"><h2 className="font-extrabold">Trade with confidence</h2><ShieldCheck size={20} className="text-[#087cf5]" /></div>
          <p className="text-sm leading-6 text-[#718097] dark:text-slate-400">Verified profiles and thoughtful communities make every exchange feel easy.</p>
        </div>
      </aside>
    </div>
  </main>
}
