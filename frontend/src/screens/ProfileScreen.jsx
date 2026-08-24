import { useRef, useState } from 'react'
import { Camera, Mail, Save, ShieldCheck, UserRound } from 'lucide-react'
import { apiFetch } from '../lib/api'

const tabs = ['Overview', 'Listings', 'Saved']

function Field({ label, name, value, onChange, type = 'text', ...props }) {
  return <label className="block text-sm font-bold text-[#526177] dark:text-slate-300">{label}<input type={type} name={name} value={value} onChange={onChange} className="mt-1.5 w-full rounded-xl border border-[#e3e8f0] bg-white px-3 py-2.5 font-normal text-[#172238] outline-none focus:border-[#087cf5] dark:border-slate-700 dark:bg-slate-800 dark:text-white" {...props} /></label>
}

export default function ProfileScreen({ user, onUpdateUser, onSignOut }) {
  const fileInput = useRef(null)
  const [activeTab, setActiveTab] = useState('Overview')
  const [form, setForm] = useState({ fullName: user.fullName || '', bio: user.bio || '', contactInfo: user.contactInfo || '', oldPassword: '', newPassword: '', confirmPassword: '' })
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [isSaving, setIsSaving] = useState(false)
  const [isUploading, setIsUploading] = useState(false)

  const displayName = user.fullName || 'Otech member'
  const setField = (event) => setForm({ ...form, [event.target.name]: event.target.value })

  async function saveProfile(event) {
    event.preventDefault()
    setMessage('')
    setError('')
    if (!form.fullName.trim()) { setError('Full name is required.'); return }
    setIsSaving(true)
    try {
      const response = await apiFetch('/user/profile', { method: 'PATCH', body: JSON.stringify({ ...form, fullName: form.fullName.trim() }) })
      const body = await response.json().catch(() => ({}))
      if (!response.ok) throw new Error(body.message || 'Could not update profile')
      onUpdateUser(body)
      setForm({ ...form, fullName: body.fullName, oldPassword: '', newPassword: '', confirmPassword: '' })
      setMessage('Profile updated successfully.')
    } catch (requestError) { setError(requestError.message || 'Could not update profile') } finally { setIsSaving(false) }
  }

  async function uploadAvatar(event) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    if (!file.type.startsWith('image/')) { setError('Please choose an image file.'); return }
    if (file.size > 5 * 1024 * 1024) { setError('Image must be 5 MB or smaller.'); return }
    setMessage('')
    setError('')
    setIsUploading(true)
    const data = new FormData()
    data.append('file', file)
    try {
      const response = await apiFetch('/user/profile/avatar', { method: 'POST', body: data })
      const body = await response.json().catch(() => ({}))
      if (!response.ok) throw new Error(body.message || 'Could not upload image')
      onUpdateUser(body)
      setMessage('Profile image updated.')
    } catch (requestError) { setError(requestError.message || 'Could not upload image') } finally { setIsUploading(false) }
  }

  const emptyCopy = activeTab === 'Listings' ? 'Your items for sale will appear here.' : 'Items you save for later will appear here.'
  return <main className="px-5 pb-28 pt-7 lg:ml-[248px] lg:px-10 lg:pb-10"><div className="mx-auto max-w-[980px]">
    <section className="overflow-hidden rounded-2xl border border-[#e3e8f0] bg-white shadow-[0_8px_28px_rgba(34,53,84,0.05)] dark:border-slate-800 dark:bg-slate-900">
      <div className="relative h-40 bg-[#087cf5] sm:h-52"><div className="absolute inset-0 bg-[linear-gradient(115deg,rgba(8,124,245,0.95),rgba(14,165,233,0.65))]" /><div className="absolute bottom-5 left-6 text-white sm:left-8"><p className="text-xs font-bold uppercase tracking-[0.2em] text-blue-100">Otech member</p><p className="mt-1 text-sm text-blue-50/90">Good finds, shared well.</p></div></div>
      <div className="px-6 sm:px-8"><div className="-mt-12 flex flex-wrap items-end justify-between gap-4"><div className="relative"><div className="flex h-24 w-24 items-center justify-center overflow-hidden rounded-2xl border-4 border-white bg-[#087cf5] text-3xl font-black text-white shadow-md dark:border-slate-900">{user.avatarUrl ? <img src={user.avatarUrl} alt="Profile" className="h-full w-full object-cover" /> : displayName.slice(0, 2).toUpperCase()}</div><button type="button" disabled={isUploading} aria-label="Upload profile image" onClick={() => fileInput.current?.click()} className="absolute -bottom-2 -right-2 rounded-full bg-[#172238] p-2 text-white shadow-md transition hover:bg-[#087cf5] disabled:cursor-wait disabled:opacity-60"><Camera size={15} /></button><input ref={fileInput} type="file" accept="image/*" onChange={uploadAvatar} className="hidden" /></div><button onClick={onSignOut} className="mb-1 rounded-xl border border-[#e3e8f0] px-4 py-2 text-sm font-bold text-[#d9485f] transition hover:bg-[#fff1f2] dark:border-slate-700 dark:hover:bg-rose-950/40">Sign out</button></div><div className="pb-5 pt-4"><h1 className="text-2xl font-extrabold tracking-[-0.04em] text-[#172238] dark:text-white">{displayName}</h1><p className="mt-1 max-w-2xl text-sm text-[#718097] dark:text-slate-400">{user.bio || 'Building a more circular community, one useful find at a time.'}</p><div className="mt-5 flex flex-wrap gap-x-6 gap-y-2 text-sm text-[#718097] dark:text-slate-400"><span><strong className="text-[#172238] dark:text-white">0</strong> listings</span><span><strong className="text-[#172238] dark:text-white">0</strong> saved</span><span>{isUploading ? 'Uploading image...' : 'Member on Otech'}</span></div></div><div className="-mx-6 flex gap-6 overflow-x-auto border-t border-[#edf0f4] px-6 dark:border-slate-800 sm:-mx-8 sm:px-8">{tabs.map((tab) => <button key={tab} type="button" onClick={() => setActiveTab(tab)} className={`border-b-2 px-1 py-4 text-sm font-bold transition ${activeTab === tab ? 'border-[#087cf5] text-[#087cf5]' : 'border-transparent text-[#8290a4] hover:text-[#172238] dark:hover:text-white'}`}>{tab}</button>)}</div></div>
    </section>
    <div className="mt-6 grid gap-6 lg:grid-cols-[minmax(0,1fr)_280px]">
      <section className="rounded-2xl border border-[#e3e8f0] bg-white p-6 dark:border-slate-800 dark:bg-slate-900"><p className="text-xs font-bold uppercase tracking-[0.16em] text-[#087cf5]">{activeTab}</p>{activeTab === 'Overview' ? <><h2 className="mt-1 text-xl font-extrabold text-[#172238] dark:text-white">Edit your profile</h2><form onSubmit={saveProfile} className="mt-5 space-y-4"><Field label="Full name" name="fullName" value={form.fullName} onChange={setField} required maxLength="255" /><div className="grid gap-4 sm:grid-cols-2"><label className="block text-sm font-bold text-[#526177] dark:text-slate-300">Bio<textarea maxLength="2000" name="bio" value={form.bio} onChange={setField} rows="4" className="mt-1.5 w-full resize-y rounded-xl border border-[#e3e8f0] bg-white px-3 py-2.5 font-normal text-[#172238] outline-none focus:border-[#087cf5] dark:border-slate-700 dark:bg-slate-800 dark:text-white" /></label><label className="block text-sm font-bold text-[#526177] dark:text-slate-300">Contact information<textarea maxLength="2000" name="contactInfo" value={form.contactInfo} onChange={setField} rows="4" className="mt-1.5 w-full resize-y rounded-xl border border-[#e3e8f0] bg-white px-3 py-2.5 font-normal text-[#172238] outline-none focus:border-[#087cf5] dark:border-slate-700 dark:bg-slate-800 dark:text-white" /></label></div><div className="border-t border-[#edf0f4] pt-4 dark:border-slate-800"><p className="text-sm font-bold text-[#526177] dark:text-slate-300">Change password</p><p className="mt-1 text-xs text-[#8290a4]">Enter your old password, then the new password twice.</p><div className="mt-3 grid gap-3 sm:grid-cols-3">{[['oldPassword', 'Old password'], ['newPassword', 'New password'], ['confirmPassword', 'Recheck new password']].map(([name, label]) => <Field key={name} label={label} name={name} type="password" value={form[name]} onChange={setField} placeholder={label} />)}</div></div>{error && <p className="text-sm font-semibold text-rose-600 dark:text-rose-400">{error}</p>}{message && <p className="text-sm font-semibold text-emerald-600 dark:text-emerald-400">{message}</p>}<button disabled={isSaving} className="inline-flex items-center gap-2 rounded-xl bg-[#087cf5] px-4 py-2.5 text-sm font-bold text-white hover:bg-[#006cdf] disabled:opacity-60"><Save size={16} />{isSaving ? 'Saving...' : 'Save changes'}</button></form></> : <div className="mt-5 rounded-xl border border-dashed border-[#cbd5e1] bg-[#f5f7fa] p-8 text-center text-sm text-[#718097] dark:border-slate-700 dark:bg-slate-800 dark:text-slate-400"><p className="font-bold text-[#526177] dark:text-slate-200">Nothing here yet</p><p className="mt-1">{emptyCopy}</p></div>}</section>
      <aside className="space-y-4"><div className="rounded-2xl border border-[#e3e8f0] bg-white p-5 dark:border-slate-800 dark:bg-slate-900"><h2 className="font-extrabold text-[#172238] dark:text-white">About you</h2><div className="mt-4 space-y-4 text-sm"><div className="flex gap-3"><Mail size={18} className="mt-0.5 shrink-0 text-[#087cf5]" /><div><p className="text-[11px] font-bold uppercase tracking-wide text-[#8290a4]">Email</p><p className="mt-1 break-all font-semibold text-[#172238] dark:text-slate-200">{user.email}</p></div></div><div className="flex gap-3"><ShieldCheck size={18} className="mt-0.5 shrink-0 text-[#087cf5]" /><div><p className="text-[11px] font-bold uppercase tracking-wide text-[#8290a4]">Account type</p><p className="mt-1 font-semibold text-[#172238] dark:text-slate-200">{user.role || 'USER'}</p></div></div>{user.contactInfo && <div><p className="text-[11px] font-bold uppercase tracking-wide text-[#8290a4]">Contact</p><p className="mt-1 whitespace-pre-line text-[#526177] dark:text-slate-300">{user.contactInfo}</p></div>}</div></div><div className="flex items-center gap-3 rounded-2xl border border-dashed border-[#cbd5e1] p-5 text-sm text-[#718097] dark:border-slate-700 dark:text-slate-400"><UserRound size={18} className="shrink-0 text-[#087cf5]" />Your profile helps the community trade with confidence.</div></aside>
    </div>
  </div></main>
}
