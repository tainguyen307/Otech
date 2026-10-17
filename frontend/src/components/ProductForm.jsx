import { ImagePlus, LoaderCircle, X } from 'lucide-react'
import { useState } from 'react'
import { apiErrorMessage, apiFetch } from '../lib/api'

const conditions = [
  ['NEW', 'New'],
  ['LIKE_NEW', 'Like new'],
  ['GOOD', 'Good'],
  ['FAIR', 'Fair'],
  ['POOR', 'Well loved'],
]

function initialValues(product) {
  return {
    title: product?.title || '',
    description: product?.description || '',
    price: product?.price || '',
    categoryId: product?.categoryId || '',
    condition: product?.condition || 'GOOD',
    location: product?.location || '',
    imageUrls: product?.imageUrls || [],
  }
}

export default function ProductForm({ product, categories, isSaving, onSave, onCancel }) {
  const [values, setValues] = useState(() => initialValues(product))
  const [uploading, setUploading] = useState(false)
  const [uploadMessage, setUploadMessage] = useState('')
  const [error, setError] = useState('')

  function update(field, value) {
    setValues((current) => ({ ...current, [field]: value }))
  }

  async function uploadFiles(event) {
    const files = Array.from(event.target.files || [])
    event.target.value = ''
    if (values.imageUrls.length + files.length > 10) {
      setError('You can add up to 10 photos.')
      return
    }
    setUploading(true)
    setError('')
    let uploaded = 0
    try {
      for (const file of files) {
        const formData = new FormData()
        formData.append('file', file)
        const response = await apiFetch('/products/images', { method: 'POST', body: formData })
        if (!response.ok) throw new Error(await apiErrorMessage(response, 'Could not upload this photo.'))
        const body = await response.json()
        if (!body.imageUrl) throw new Error('The image service did not return a photo URL.')
        setValues((current) => ({ ...current, imageUrls: [...current.imageUrls, body.imageUrl] }))
        uploaded += 1
        setUploadMessage(`Uploaded ${uploaded} of ${files.length} photos`)
      }
    } catch (uploadError) {
      setError(uploadError.message || 'Could not upload photo.')
    } finally {
      setUploading(false)
    }
  }

  async function submit(event) {
    event.preventDefault()
    setError('')
    const imageUrls = [...new Set(values.imageUrls.map((url) => url.trim()).filter(Boolean))]
    if (imageUrls.length > 10) {
      setError('You can add up to 10 photos.')
      return
    }
    try {
      await onSave({
        ...values,
        title: values.title.trim(),
        description: values.description.trim(),
        price: Number(values.price),
        categoryId: values.categoryId || null,
        location: values.location.trim(),
        imageUrls,
      })
    } catch (saveError) {
      setError(saveError.message || 'Could not save this listing.')
    }
  }

  return <form onSubmit={submit} className="space-y-5 rounded-2xl border border-[#dce8f5] bg-white p-5 shadow-sm dark:border-slate-700 dark:bg-slate-900 sm:p-7">
    <div className="flex items-start justify-between gap-4">
      <div><p className="text-xs font-bold uppercase tracking-[0.16em] text-[#087cf5]">{product ? 'Edit listing' : 'New listing'}</p><h2 className="mt-1 text-xl font-extrabold text-[#172238] dark:text-white">{product ? 'Update your item' : 'Pass a good find forward'}</h2></div>
      <button type="button" onClick={onCancel} aria-label="Close listing form" className="rounded-xl p-2 text-[#718097] hover:bg-[#f5f7fa] dark:text-slate-400 dark:hover:bg-slate-800"><X size={18} /></button>
    </div>
    <div className="grid gap-4 sm:grid-cols-2">
      <label className="space-y-1.5 text-sm font-semibold text-[#526078] dark:text-slate-300 sm:col-span-2">Item name
        <input required maxLength="255" value={values.title} onChange={(event) => update('title', event.target.value)} className="min-h-11 w-full rounded-xl border border-[#dfe6ef] bg-white px-3 text-sm text-[#172238] outline-none focus:border-[#087cf5] focus:ring-2 focus:ring-blue-100 dark:border-slate-700 dark:bg-slate-950 dark:text-white dark:focus:ring-blue-950" />
      </label>
      <label className="space-y-1.5 text-sm font-semibold text-[#526078] dark:text-slate-300">Price (VND)
        <input required type="number" min="0.01" step="0.01" value={values.price} onChange={(event) => update('price', event.target.value)} className="min-h-11 w-full rounded-xl border border-[#dfe6ef] bg-white px-3 text-sm text-[#172238] outline-none focus:border-[#087cf5] focus:ring-2 focus:ring-blue-100 dark:border-slate-700 dark:bg-slate-950 dark:text-white dark:focus:ring-blue-950" />
      </label>
      <label className="space-y-1.5 text-sm font-semibold text-[#526078] dark:text-slate-300">Condition
        <select value={values.condition} onChange={(event) => update('condition', event.target.value)} className="min-h-11 w-full rounded-xl border border-[#dfe6ef] bg-white px-3 text-sm text-[#172238] outline-none focus:border-[#087cf5] focus:ring-2 focus:ring-blue-100 dark:border-slate-700 dark:bg-slate-950 dark:text-white dark:focus:ring-blue-950">{conditions.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select>
      </label>
      <label className="space-y-1.5 text-sm font-semibold text-[#526078] dark:text-slate-300">Category
        <select value={values.categoryId} onChange={(event) => update('categoryId', event.target.value)} className="min-h-11 w-full rounded-xl border border-[#dfe6ef] bg-white px-3 text-sm text-[#172238] outline-none focus:border-[#087cf5] focus:ring-2 focus:ring-blue-100 dark:border-slate-700 dark:bg-slate-950 dark:text-white dark:focus:ring-blue-950"><option value="">Choose category</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.name}</option>)}</select>
      </label>
      <label className="space-y-1.5 text-sm font-semibold text-[#526078] dark:text-slate-300">Meet-up location
        <input maxLength="255" value={values.location} onChange={(event) => update('location', event.target.value)} placeholder="e.g. District 1, Ho Chi Minh City" className="min-h-11 w-full rounded-xl border border-[#dfe6ef] bg-white px-3 text-sm text-[#172238] outline-none placeholder:text-[#9aa6b8] focus:border-[#087cf5] focus:ring-2 focus:ring-blue-100 dark:border-slate-700 dark:bg-slate-950 dark:text-white dark:focus:ring-blue-950" />
      </label>
      <label className="space-y-1.5 text-sm font-semibold text-[#526078] dark:text-slate-300 sm:col-span-2">Description
        <textarea rows="4" maxLength="10000" value={values.description} onChange={(event) => update('description', event.target.value)} placeholder="Describe the item's condition, age, and anything a buyer should know." className="w-full resize-y rounded-xl border border-[#dfe6ef] bg-white px-3 py-2.5 text-sm text-[#172238] outline-none placeholder:text-[#9aa6b8] focus:border-[#087cf5] focus:ring-2 focus:ring-blue-100 dark:border-slate-700 dark:bg-slate-950 dark:text-white dark:focus:ring-blue-950" />
      </label>
    </div>
    <div className="space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div><p className="text-sm font-semibold text-[#526078] dark:text-slate-300">Photos <span className="font-normal text-[#8a96a8]">({values.imageUrls.length}/10)</span></p><p className="mt-1 text-xs text-[#8a96a8]">JPEG, PNG or WebP; up to 8 MB each. Or add secure image URLs.</p></div>
        <label className={`inline-flex min-h-10 cursor-pointer items-center gap-2 rounded-xl border border-[#dfe6ef] px-3 text-xs font-bold text-[#526078] hover:bg-[#f5f7fa] dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800 ${uploading || values.imageUrls.length >= 10 ? 'pointer-events-none opacity-50' : ''}`}>
          {uploading ? <LoaderCircle size={15} className="animate-spin" /> : <ImagePlus size={15} />}
          {uploading ? 'Uploading…' : 'Upload photos'}
          <input type="file" accept="image/jpeg,image/png,image/webp" multiple disabled={uploading || values.imageUrls.length >= 10} onChange={uploadFiles} className="sr-only" />
        </label>
      </div>
      {uploadMessage && <p role="status" className="text-xs text-[#087cf5]">{uploadMessage}</p>}
      {values.imageUrls.length > 0 && <ul className="grid grid-cols-3 gap-3 sm:grid-cols-5">{values.imageUrls.map((url, index) => <li key={`${url}-${index}`} className="relative aspect-square overflow-hidden rounded-xl bg-[#f5f7fa] dark:bg-slate-800"><img src={url} alt={`Listing photo ${index + 1}`} className="h-full w-full object-cover" /><button type="button" onClick={() => update('imageUrls', values.imageUrls.filter((_, itemIndex) => itemIndex !== index))} aria-label={`Remove photo ${index + 1}`} className="absolute right-1.5 top-1.5 rounded-full bg-slate-950/75 p-1.5 text-white hover:bg-rose-600"><X size={13} /></button></li>)}</ul>}
      <label className="block space-y-1.5 text-xs font-semibold text-[#718097] dark:text-slate-400">Or paste HTTPS image URLs, one per line
        <textarea rows="2" value={values.imageUrls.join('\n')} onChange={(event) => update('imageUrls', event.target.value.split('\n').slice(0, 10))} placeholder="https://example.com/item-photo.jpg" className="w-full rounded-xl border border-[#dfe6ef] bg-white px-3 py-2 text-sm font-normal text-[#172238] outline-none placeholder:text-[#a6b0bf] focus:border-[#087cf5] focus:ring-2 focus:ring-blue-100 dark:border-slate-700 dark:bg-slate-950 dark:text-white dark:focus:ring-blue-950" />
      </label>
    </div>
    {error && <p role="alert" className="rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700 dark:bg-rose-950/30 dark:text-rose-300">{error}</p>}
    <div className="flex flex-col-reverse gap-3 border-t border-[#eef1f5] pt-4 dark:border-slate-800 sm:flex-row sm:justify-end">
      <button type="button" onClick={onCancel} className="min-h-11 rounded-xl px-5 text-sm font-bold text-[#718097] hover:bg-[#f5f7fa] dark:text-slate-300 dark:hover:bg-slate-800">Cancel</button>
      <button type="submit" disabled={isSaving || uploading} className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-[#087cf5] px-5 text-sm font-bold text-white hover:bg-[#006cdf] disabled:cursor-not-allowed disabled:opacity-60">{isSaving && <LoaderCircle size={16} className="animate-spin" />}{isSaving ? 'Saving…' : product ? 'Save changes' : 'Publish listing'}</button>
    </div>
  </form>
}
