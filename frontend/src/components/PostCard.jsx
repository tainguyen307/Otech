import { Bookmark, Heart, ImageOff, MessageCircle, Package, Pencil, Send, Share2, Trash2, UserRound } from 'lucide-react'
import { useState } from 'react'
import PostComments from './PostComments'

function PostImage({ src, alt }) {
  const [status, setStatus] = useState('loading')

  if (!src) return null

  return <div className="relative aspect-[16/9] w-full overflow-hidden bg-[#f5f7fa] dark:bg-slate-800">
    {status !== 'loaded' && <div className="absolute inset-0 flex items-center justify-center text-[#9aa5b5] dark:text-slate-500">
      {status === 'error' && <ImageOff size={30} strokeWidth={1.5} aria-label="Image could not be loaded" />}
    </div>}
    <img
      className={`h-full w-full object-cover transition-opacity ${status === 'loaded' ? 'opacity-100' : 'opacity-0'}`}
      src={src}
      alt={alt}
      onLoad={() => setStatus('loaded')}
      onError={() => setStatus('error')}
    />
  </div>
}

export default function PostCard({ post, isSaved, onLike, onSave, onShare, onEdit, onDelete, onAuth, onOpenProfile, onMessage, onCommentAdded, user }) {
  const [showComments, setShowComments] = useState(false)
  const isLiked = Boolean(post.likedByCurrentUser)
  const isOwner = Boolean(user?.id && user.id === post.authorId)
  return <article id={`post-${post.id}`} className="overflow-hidden rounded-2xl border border-[#e3e8f0] bg-white shadow-[0_8px_28px_rgba(34,53,84,0.04)] dark:border-slate-800 dark:bg-slate-900">
    <div className="flex items-center gap-3 px-5 py-4"><button type="button" disabled={!post.authorId} onClick={() => onOpenProfile(post.authorId)} aria-label={`View ${post.name}'s profile`} className="flex h-10 w-10 shrink-0 items-center justify-center overflow-hidden rounded-full bg-[#eaf3ff] text-[#087cf5] disabled:cursor-default dark:bg-slate-800">{post.avatar ? <img className="h-full w-full object-cover" src={post.avatar} alt="" /> : <UserRound size={20} />}</button><div className="min-w-0 flex-1"><button type="button" disabled={!post.authorId} onClick={() => onOpenProfile(post.authorId)} className="block max-w-full truncate text-left text-sm font-extrabold hover:text-[#087cf5] disabled:cursor-default disabled:hover:text-inherit">{post.name} <span className="font-normal text-[#9aa5b5]">{post.handle}</span></button><p className="text-xs text-[#8c98aa]">{post.time}</p></div>{isOwner && <div className="flex gap-1"><button type="button" onClick={onEdit} aria-label="Edit post" className="rounded-lg p-2 text-[#718097] hover:bg-[#f5f7fa] hover:text-[#087cf5] dark:hover:bg-slate-800"><Pencil size={16} /></button><button type="button" onClick={onDelete} aria-label="Delete post" className="rounded-lg p-2 text-[#718097] hover:bg-rose-50 hover:text-rose-600 dark:hover:bg-rose-950/30"><Trash2 size={16} /></button></div>}</div>
    <PostImage key={post.image || 'no-image'} src={post.image} alt={post.title} />
    <div className="p-5"><div className="mb-3 flex items-start justify-between gap-3"><p className="whitespace-pre-wrap text-base leading-7 text-[#34445d] dark:text-slate-200">{post.copy}</p><strong className="whitespace-nowrap text-lg text-[#087cf5]">{post.price}</strong></div>{post.tag && <span className="inline-flex items-center gap-1.5 rounded-full bg-[#e8f7ee] px-2.5 py-1 text-[11px] font-bold text-[#22804a]"><Package size={13} /> {post.tag}</span>}<div className="mt-5 flex flex-wrap items-center justify-between gap-2 border-t border-[#edf0f4] pt-4"><div className="flex flex-wrap gap-1"><button type="button" onClick={onLike} aria-pressed={isLiked} className={`flex items-center gap-1.5 rounded-lg px-2 py-1.5 text-xs font-bold transition ${isLiked ? 'text-[#f05b67]' : 'text-[#7c899c] hover:bg-[#f5f7fa]'}`}><Heart size={16} fill={isLiked ? 'currentColor' : 'none'} /> {post.likes || 0}</button><button type="button" onClick={() => setShowComments((current) => !current)} aria-expanded={showComments} className="flex items-center gap-1.5 rounded-lg px-2 py-1.5 text-xs font-bold text-[#7c899c] hover:bg-[#f5f7fa]"><MessageCircle size={16} /> {post.comments || 0}</button><button type="button" onClick={onShare} className="flex items-center gap-1.5 rounded-lg px-2 py-1.5 text-xs font-bold text-[#7c899c] hover:bg-[#f5f7fa]"><Share2 size={16} /> {post.shares || 0}</button></div><div className="flex gap-1"><button type="button" onClick={onSave} aria-label="Save post" className={`rounded-lg p-2 transition ${isSaved ? 'text-[#087cf5]' : 'text-[#7c899c] hover:bg-[#f5f7fa]'}`}><Bookmark size={17} fill={isSaved ? 'currentColor' : 'none'} /></button>{post.authorId && user?.id !== post.authorId && <button type="button" onClick={() => onMessage(post.authorId)} className="flex items-center gap-1.5 rounded-lg bg-[#087cf5] px-3 py-2 text-xs font-bold text-white transition hover:bg-[#006cdf]"><Send size={14} /> Message</button>}</div></div></div>
    {showComments && <PostComments postId={post.id} user={user} onAuth={onAuth} onCommentAdded={onCommentAdded} />}
  </article>
}
