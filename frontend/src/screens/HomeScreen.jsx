import { useEffect, useState } from 'react'
import { LoaderCircle, RefreshCw, ShieldCheck } from 'lucide-react'
import PostCard from '../components/PostCard'
import { apiFetch } from '../lib/api'

const filters = ['For you', 'Nearby', 'Furniture', 'Tech', 'Free']

export default function HomeScreen({ liked, saved, onLike, onSave, onAuth }) {
  const [posts, setPosts] = useState([])
  const [activeFilter, setActiveFilter] = useState('For you')
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')

  async function loadPosts() {
    setIsLoading(true)
    setError('')
    try {
      const response = await apiFetch('/posts')
      const body = await response.json().catch(() => [])
      if (!response.ok) throw new Error(body.message || 'Could not load posts')
      setPosts(Array.isArray(body) ? body : [])
    } catch (requestError) {
      setError(requestError.message || 'Could not load posts')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => { loadPosts() }, [])

  return <main className="px-5 pb-28 pt-7 lg:ml-[248px] lg:px-10 lg:pb-10"><div className="mx-auto grid max-w-[1180px] gap-8 xl:grid-cols-[minmax(0,1fr)_300px]"><section><div className="mb-7"><p className="mb-2 text-xs font-bold uppercase tracking-[0.18em] text-[#087cf5]">Your community</p><h1 className="text-3xl font-extrabold tracking-[-0.04em] text-[#172238] dark:text-white sm:text-4xl">Good finds, shared well.</h1><p className="mt-2 text-sm text-[#718097] dark:text-slate-400">See what your neighbors are passing on today.</p></div><div className="mb-6 flex gap-2 overflow-x-auto pb-1">{filters.map((filter) => <button key={filter} onClick={() => setActiveFilter(filter)} className={`min-h-9 whitespace-nowrap rounded-full px-4 py-2.5 text-xs font-bold transition focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#087cf5] ${activeFilter === filter ? 'bg-[#172238] text-white dark:bg-slate-700' : 'bg-white text-[#718097] ring-1 ring-[#e3e8f0] hover:bg-[#eaf3ff] hover:text-[#087cf5] dark:bg-slate-800 dark:text-slate-300 dark:ring-slate-700 dark:hover:bg-slate-950 dark:hover:text-blue-300'}`}>{filter}</button>)}</div>{isLoading && <div className="flex items-center justify-center gap-2 rounded-2xl border border-dashed border-[#e3e8f0] p-10 text-sm text-[#718097] dark:border-slate-700 dark:text-slate-400"><LoaderCircle size={18} className="animate-spin text-[#087cf5]" /> Loading community posts...</div>}{error && !isLoading && <div className="rounded-2xl border border-rose-200 bg-rose-50 p-5 text-sm text-rose-700 dark:border-rose-900 dark:bg-rose-950/30 dark:text-rose-300"><p>{error}</p><button onClick={loadPosts} className="mt-3 inline-flex items-center gap-2 rounded-lg bg-white px-3 py-2 text-xs font-bold text-rose-700 shadow-sm hover:bg-rose-100 dark:bg-slate-900 dark:text-rose-300 dark:hover:bg-rose-950"><RefreshCw size={14} /> Try again</button></div>}{!isLoading && !error && posts.length === 0 && <div className="rounded-2xl border border-dashed border-[#e3e8f0] p-10 text-center text-sm text-[#718097] dark:border-slate-700 dark:text-slate-400">No community posts yet.</div>}{!isLoading && !error && <div className="space-y-6">{posts.map((post) => <PostCard key={post.id} post={post} isLiked={liked.includes(post.id)} isSaved={saved.includes(post.id)} onLike={() => onLike(post.id)} onSave={() => onSave(post.id)} onAuth={onAuth} />)}</div>}</section><aside className="hidden space-y-5 xl:block"><div className="rounded-2xl border border-[#e3e8f0] bg-white p-5 dark:border-slate-800 dark:bg-slate-900"><div className="mb-5 flex items-center justify-between"><h2 className="font-extrabold">Trade with confidence</h2><ShieldCheck size={20} className="text-[#087cf5]" /></div><p className="text-sm leading-6 text-[#718097] dark:text-slate-400">Verified profiles and thoughtful communities make every exchange feel easy.</p></div></aside></div></main>
}
