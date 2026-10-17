import { useEffect, useState } from 'react'
import { ArrowLeft, CalendarDays, LoaderCircle, MessageCircle, RefreshCw, UserPlus, UserRound, UserRoundCheck } from 'lucide-react'
import { apiErrorMessage, apiFetch } from '../lib/api'

function formatJoinedAt(value) {
  if (!value) return 'Otech member'
  return `Joined ${new Intl.DateTimeFormat(undefined, { month: 'long', year: 'numeric' }).format(new Date(value))}`
}

export default function PublicProfileScreen({ userId, currentUser, onBack, onAuth, onMessage }) {
  const [profile, setProfile] = useState(null)
  const [error, setError] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [isFollowing, setIsFollowing] = useState(false)
  const [isUpdatingFollow, setIsUpdatingFollow] = useState(false)
  const isOwnProfile = currentUser?.id === userId
  const viewerId = currentUser?.id

  useEffect(() => {
    let isCurrent = true
    apiFetch(`/profiles/${userId}`)
      .then(async (response) => {
        if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not load this profile.'))
        return response.json()
      })
      .then((body) => {
        if (!isCurrent) return
        setProfile(body)
        setIsFollowing(Boolean(body.followedByCurrentUser))
      })
      .catch((requestError) => {
        if (isCurrent) setError(requestError.message || 'Could not load this profile.')
      })
      .finally(() => {
        if (isCurrent) setIsLoading(false)
      })
    return () => { isCurrent = false }
  }, [userId, viewerId])

  async function loadProfile() {
    setIsLoading(true)
    setError('')
    try {
      const response = await apiFetch(`/profiles/${userId}`)
      if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not load this profile.'))
      const body = await response.json()
      setProfile(body)
      setIsFollowing(Boolean(body.followedByCurrentUser))
    } catch (requestError) {
      setError(requestError.message || 'Could not load this profile.')
    } finally {
      setIsLoading(false)
    }
  }

  async function toggleFollow() {
    if (!currentUser) {
      onAuth()
      return
    }
    if (isUpdatingFollow) return
    setIsUpdatingFollow(true)
    setError('')
    try {
      const response = await apiFetch(`/profiles/${userId}/follow`, { method: isFollowing ? 'DELETE' : 'POST' })
      if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not update follow status.'))
      setIsFollowing((value) => !value)
      setProfile((value) => ({
        ...value,
        followerCount: Math.max(0, value.followerCount + (isFollowing ? -1 : 1)),
      }))
    } catch (requestError) {
      setError(requestError.message || 'Could not update follow status.')
    } finally {
      setIsUpdatingFollow(false)
    }
  }

  return <main className="px-5 pb-28 pt-7 lg:ml-[248px] lg:px-10 lg:pb-10">
    <div className="mx-auto max-w-[980px]">
      <button type="button" onClick={onBack} className="mb-5 inline-flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-bold text-[#718097] transition hover:bg-white hover:text-[#087cf5] dark:text-slate-400 dark:hover:bg-slate-900">
        <ArrowLeft size={17} /> Back to community
      </button>
      {isLoading && <div className="flex items-center justify-center gap-2 rounded-2xl border border-dashed border-[#e3e8f0] p-12 text-sm text-[#718097] dark:border-slate-700 dark:text-slate-400"><LoaderCircle size={18} className="animate-spin text-[#087cf5]" /> Loading profile...</div>}
      {error && !isLoading && !profile && <section className="rounded-2xl border border-rose-200 bg-rose-50 p-6 text-sm text-rose-700 dark:border-rose-900 dark:bg-rose-950/30 dark:text-rose-300"><p>{error}</p><button type="button" onClick={loadProfile} className="mt-3 inline-flex items-center gap-2 rounded-lg bg-white px-3 py-2 text-xs font-bold dark:bg-slate-900"><RefreshCw size={14} /> Try again</button></section>}
      {profile && !isLoading && <>
        <section className="overflow-hidden rounded-2xl border border-[#e3e8f0] bg-white shadow-[0_8px_28px_rgba(34,53,84,0.05)] dark:border-slate-800 dark:bg-slate-900">
          <div className="h-40 bg-[linear-gradient(115deg,rgba(8,124,245,0.95),rgba(14,165,233,0.65))] sm:h-52" />
          <div className="px-6 sm:px-8">
            <div className="-mt-12 flex flex-wrap items-end justify-between gap-4">
              <div className="flex h-24 w-24 items-center justify-center overflow-hidden rounded-2xl border-4 border-white bg-[#eaf3ff] text-[#087cf5] shadow-md dark:border-slate-900 dark:bg-slate-800">
                {profile.avatarUrl ? <img src={profile.avatarUrl} alt={`${profile.fullName}'s profile`} className="h-full w-full object-cover" /> : <UserRound size={34} />}
              </div>
              {!isOwnProfile && <div className="mb-1 flex flex-wrap gap-2">
                <button type="button" onClick={toggleFollow} disabled={isUpdatingFollow} className={`inline-flex min-h-11 items-center justify-center gap-2 rounded-xl px-4 py-2 text-sm font-bold transition disabled:cursor-wait disabled:opacity-60 ${isFollowing ? 'border border-[#e3e8f0] bg-white text-[#526177] hover:bg-[#f5f7fa] dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300' : 'bg-[#087cf5] text-white hover:bg-[#006cdf]'}`}>
                  {isUpdatingFollow ? <LoaderCircle size={16} className="animate-spin" /> : isFollowing ? <UserRoundCheck size={16} /> : <UserPlus size={16} />}
                  {isFollowing ? 'Following' : 'Follow'}
                </button>
                <button type="button" onClick={() => currentUser ? onMessage(profile.userId) : onAuth()} className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl border border-[#e3e8f0] bg-white px-4 py-2 text-sm font-bold text-[#526177] transition hover:border-[#087cf5] hover:text-[#087cf5] dark:border-slate-700 dark:bg-slate-900 dark:text-slate-300">
                  <MessageCircle size={16} /> Message
                </button>
              </div>}
            </div>
            <div className="pb-6 pt-4">
              <h1 className="text-2xl font-extrabold tracking-[-0.04em] text-[#172238] dark:text-white">{profile.fullName}</h1>
              <p className="mt-2 max-w-2xl whitespace-pre-wrap text-sm leading-6 text-[#718097] dark:text-slate-400">{profile.bio || 'This member has not added a bio yet.'}</p>
              <div className="mt-5 flex flex-wrap gap-x-6 gap-y-3 text-sm text-[#718097] dark:text-slate-400">
                <span><strong className="text-[#172238] dark:text-white">{profile.postCount}</strong> posts</span>
                <span><strong className="text-[#172238] dark:text-white">{profile.followerCount}</strong> followers</span>
                <span><strong className="text-[#172238] dark:text-white">{profile.followingCount}</strong> following</span>
                <span className="inline-flex items-center gap-1.5"><CalendarDays size={15} /> {formatJoinedAt(profile.joinedAt)}</span>
              </div>
            </div>
          </div>
        </section>
        {error && <p role="alert" className="mt-4 rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700 dark:bg-rose-950/30 dark:text-rose-300">{error}</p>}
        <section className="mt-6">
          <h2 className="mb-4 text-lg font-extrabold text-[#172238] dark:text-white">Community posts</h2>
          {profile.posts.length === 0 ? <div className="rounded-2xl border border-dashed border-[#e3e8f0] p-8 text-center text-sm text-[#718097] dark:border-slate-700 dark:text-slate-400">No public posts yet.</div> : <div className="space-y-3">{profile.posts.map((post) => <article key={post.id} className="rounded-2xl border border-[#e3e8f0] bg-white p-5 dark:border-slate-800 dark:bg-slate-900"><p className="whitespace-pre-wrap text-sm leading-6 text-[#526177] dark:text-slate-300">{post.copy}</p><p className="mt-3 text-xs text-[#8c98aa]">{post.time}</p></article>)}</div>}
        </section>
      </>}
    </div>
  </main>
}
