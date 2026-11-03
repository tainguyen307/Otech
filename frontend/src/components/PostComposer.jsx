import { ImagePlus, LoaderCircle, Send, X } from 'lucide-react'
import { useState } from 'react'
import { apiErrorMessage, apiFetch } from '../lib/api'

export default function PostComposer({ post, isSaving, onSave, onCancel }) {
  const [content, setContent] = useState(post?.copy || '')
  const [imageUrl, setImageUrl] = useState(post?.image || '')
  const [isUploading, setIsUploading] = useState(false)
  const [error, setError] = useState('')

  async function uploadImage(event) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return

    setError('')
    setIsUploading(true)
    try {
      const formData = new FormData()
      formData.append('file', file)
      const response = await apiFetch('/products/images', { method: 'POST', body: formData })
      if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not upload this photo.'))
      const body = await response.json()
      if (!body.imageUrl) throw new Error('The image service did not return a photo URL.')
      setImageUrl(body.imageUrl)
    } catch (uploadError) {
      setError(uploadError.message || 'Could not upload this photo.')
    } finally {
      setIsUploading(false)
    }
  }

  async function submit(event) {
    event.preventDefault()
    setError('')
    if (!content.trim()) {
      setError('Write something before publishing your post.')
      return
    }
    try {
      await onSave({ content: content.trim(), imageUrl: imageUrl || null })
    } catch (saveError) {
      setError(saveError.message || 'Could not save your post.')
    }
  }

  return <form onSubmit={submit} className="mb-6 rounded-2xl border border-[#e3e8f0] bg-white p-5 shadow-sm dark:border-slate-800 dark:bg-slate-900">
    <div className="mb-3 flex items-center justify-between gap-3">
      <div>
        <p className="text-xs font-bold uppercase tracking-[0.16em] text-[#087cf5]">{post ? 'Edit post' : 'Share with your community'}</p>
        <p className="mt-1 text-sm font-semibold text-[#526078] dark:text-slate-300">{post ? 'Update your post' : 'What would you like to share?'}</p>
      </div>
      {post && <button type="button" onClick={onCancel} aria-label="Cancel editing" className="rounded-lg p-2 text-[#718097] hover:bg-[#f5f7fa] dark:hover:bg-slate-800"><X size={17} /></button>}
    </div>
    <textarea
      required
      maxLength="5000"
      rows="3"
      value={content}
      onChange={(event) => setContent(event.target.value)}
      placeholder="Share a thought, a useful find, or a story..."
      className="w-full resize-y rounded-xl border border-[#dfe6ef] bg-white px-3 py-2.5 text-sm text-[#172238] outline-none placeholder:text-[#9aa6b8] focus:border-[#087cf5] focus:ring-2 focus:ring-blue-100 dark:border-slate-700 dark:bg-slate-950 dark:text-white dark:focus:ring-blue-950"
    />
    {imageUrl && <div className="relative mt-3 overflow-hidden rounded-xl border border-[#e3e8f0] dark:border-slate-700">
      <img src={imageUrl} alt="Post attachment preview" className="max-h-64 w-full object-cover" />
      <button type="button" onClick={() => setImageUrl('')} aria-label="Remove attached image" className="absolute right-2 top-2 rounded-full bg-slate-950/70 p-2 text-white hover:bg-slate-950"><X size={15} /></button>
    </div>}
    {error && <p role="alert" className="mt-3 text-sm text-rose-600 dark:text-rose-300">{error}</p>}
    <div className="mt-4 flex flex-wrap items-center justify-between gap-3">
      <label className={`inline-flex min-h-10 cursor-pointer items-center gap-2 rounded-xl border border-[#dfe6ef] px-3 text-xs font-bold text-[#526078] hover:bg-[#f5f7fa] dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800 ${isUploading ? 'pointer-events-none opacity-50' : ''}`}>
        {isUploading ? <LoaderCircle size={15} className="animate-spin" /> : <ImagePlus size={15} />}
        {isUploading ? 'Uploading…' : 'Add a photo'}
        <input type="file" accept="image/jpeg,image/png,image/webp" disabled={isUploading} onChange={uploadImage} className="sr-only" />
      </label>
      <button type="submit" disabled={isSaving || isUploading || !content.trim()} className="inline-flex min-h-10 items-center gap-2 rounded-xl bg-[#087cf5] px-4 text-xs font-bold text-white transition hover:bg-[#006cdf] disabled:cursor-not-allowed disabled:opacity-60">
        {isSaving ? <LoaderCircle size={15} className="animate-spin" /> : <Send size={14} />}
        {isSaving ? 'Publishing…' : post ? 'Save changes' : 'Publish post'}
      </button>
    </div>
  </form>
}
