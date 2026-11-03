import { CornerDownRight, LoaderCircle, MessageCircle, Send, UserRound, X } from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { apiErrorMessage, apiFetch } from '../lib/api'

function buildThreads(comments) {
  const nodes = new Map(comments.map((comment) => [comment.id, { ...comment, replies: [] }]))
  const roots = []
  comments.forEach((comment) => {
    const node = nodes.get(comment.id)
    const parent = comment.parentCommentId ? nodes.get(comment.parentCommentId) : null
    if (parent) parent.replies.push(node)
    else roots.push(node)
  })
  return roots
}

function CommentThread({ comment, depth, onReply }) {
  return <li className="space-y-2">
    <div className="flex items-start gap-2.5">
      <div className="flex h-8 w-8 shrink-0 items-center justify-center overflow-hidden rounded-full bg-[#eaf3ff] text-[#087cf5] dark:bg-slate-800">
        {comment.authorAvatar ? <img src={comment.authorAvatar} alt="" className="h-full w-full object-cover" /> : <UserRound size={16} />}
      </div>
      <div className="min-w-0 flex-1 rounded-xl bg-[#f5f7fa] px-3 py-2 dark:bg-slate-800">
        <p className="text-xs font-extrabold text-[#172238] dark:text-slate-100">{comment.authorName}</p>
        <p className="mt-1 whitespace-pre-wrap break-words text-sm text-[#526078] dark:text-slate-300">{comment.content}</p>
        <button type="button" onClick={() => onReply(comment)} className="mt-2 text-xs font-bold text-[#087cf5] hover:underline">Reply</button>
      </div>
    </div>
    {comment.replies.length > 0 && <ul className={`space-y-3 border-l border-[#dfe6ef] pl-3 dark:border-slate-700 ${depth < 3 ? 'ml-5' : 'ml-3'}`}>
      {comment.replies.map((reply) => <CommentThread key={reply.id} comment={reply} depth={depth + 1} onReply={onReply} />)}
    </ul>}
  </li>
}

export default function PostComments({ postId, user, onAuth, onCommentAdded }) {
  const [comments, setComments] = useState([])
  const [content, setContent] = useState('')
  const [replyTo, setReplyTo] = useState(null)
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')
  const threads = useMemo(() => buildThreads(comments), [comments])

  useEffect(() => {
    let active = true
    apiFetch(`/posts/${postId}/comments`)
      .then(async (response) => {
        if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not load comments.'))
        return response.json()
      })
      .then((body) => { if (active) setComments(Array.isArray(body) ? body : []) })
      .catch((requestError) => { if (active) setError(requestError.message || 'Could not load comments.') })
      .finally(() => { if (active) setIsLoading(false) })
    return () => { active = false }
  }, [postId])

  async function submit(event) {
    event.preventDefault()
    if (!user) {
      onAuth()
      return
    }
    if (!content.trim()) return

    setError('')
    setIsSubmitting(true)
    try {
      const response = await apiFetch(`/posts/${postId}/comments`, {
        method: 'POST',
        body: JSON.stringify({ content: content.trim(), parentCommentId: replyTo?.id || null }),
      })
      if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not publish your comment.'))
      const comment = await response.json()
      setComments((current) => [...current, comment])
      setContent('')
      setReplyTo(null)
      onCommentAdded()
    } catch (requestError) {
      setError(requestError.message || 'Could not publish your comment.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return <section aria-label="Post comments" className="border-t border-[#edf0f4] px-5 py-4 dark:border-slate-800">
    <div className="mb-4 flex items-center gap-2 text-sm font-bold text-[#526078] dark:text-slate-300"><MessageCircle size={16} /> Comments</div>
    {isLoading && <div className="flex items-center gap-2 py-3 text-sm text-[#718097]"><LoaderCircle size={16} className="animate-spin" /> Loading comments...</div>}
    {!isLoading && threads.length === 0 && <p className="py-2 text-sm text-[#718097] dark:text-slate-400">No comments yet. Start the conversation.</p>}
    {!isLoading && threads.length > 0 && <ul className="mb-4 space-y-4">{threads.map((comment) => <CommentThread key={comment.id} comment={comment} depth={0} onReply={setReplyTo} />)}</ul>}
    {error && <p role="alert" className="mb-3 text-sm text-rose-600 dark:text-rose-300">{error}</p>}
    {replyTo && <div className="mb-2 flex items-center gap-2 text-xs text-[#718097]"><CornerDownRight size={14} /> Replying to {replyTo.authorName}<button type="button" onClick={() => setReplyTo(null)} aria-label="Cancel reply" className="rounded p-1 hover:bg-[#f5f7fa] dark:hover:bg-slate-800"><X size={13} /></button></div>}
    <form onSubmit={submit} className="flex items-end gap-2">
      <textarea
        rows="1"
        maxLength="2000"
        value={content}
        onChange={(event) => setContent(event.target.value)}
        onFocus={() => { if (!user) onAuth() }}
        placeholder={replyTo ? `Reply to ${replyTo.authorName}...` : user ? 'Write a comment...' : 'Sign in to join the conversation'}
        className="min-h-10 flex-1 resize-y rounded-xl border border-[#dfe6ef] bg-white px-3 py-2 text-sm outline-none focus:border-[#087cf5] dark:border-slate-700 dark:bg-slate-950 dark:text-white"
      />
      <button type="submit" disabled={isSubmitting || !content.trim()} aria-label={replyTo ? 'Send reply' : 'Send comment'} className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-[#087cf5] text-white hover:bg-[#006cdf] disabled:cursor-not-allowed disabled:opacity-50">
        {isSubmitting ? <LoaderCircle size={16} className="animate-spin" /> : <Send size={15} />}
      </button>
    </form>
  </section>
}
