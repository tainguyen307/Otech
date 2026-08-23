import { useState } from 'react'
import { LogIn, X } from 'lucide-react'
import { apiFetch } from '../lib/api'

export default function AuthPrompt({ onClose, onSignedIn }) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function submit(event) {
    event.preventDefault()
    setError('')
    setIsSubmitting(true)
    try {
      const response = await apiFetch('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      })
      const body = await response.json().catch(() => ({}))
      if (!response.ok) throw new Error(body.message || 'Unable to sign in')
      onSignedIn(body)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return <div className="fixed inset-0 z-50 flex items-end justify-center bg-[#172238]/40 p-4 backdrop-blur-sm sm:items-center"><div className="relative w-full max-w-[420px] rounded-2xl bg-white p-7 shadow-2xl"><button aria-label="Close" onClick={onClose} className="absolute right-4 top-4 rounded-lg p-2 text-[#8290a4] hover:bg-[#f5f7fa]"><X size={18} /></button><div className="mb-5 flex h-12 w-12 items-center justify-center rounded-2xl bg-[#eaf3ff] text-[#087cf5]"><LogIn size={22} /></div><h2 className="text-2xl font-extrabold tracking-[-0.04em]">Sign in to Otech</h2><p className="mt-2 text-sm leading-6 text-[#718097]">Sign in to message sellers and join conversations.</p><form onSubmit={submit} className="mt-6 space-y-3"><label className="block text-xs font-bold text-[#526177]">Email<input required type="email" value={email} onChange={(event) => setEmail(event.target.value)} className="mt-1 h-11 w-full rounded-xl border border-[#dfe5ed] px-3 text-sm outline-none focus:border-[#087cf5]" /></label><label className="block text-xs font-bold text-[#526177]">Password<input required type="password" value={password} onChange={(event) => setPassword(event.target.value)} className="mt-1 h-11 w-full rounded-xl border border-[#dfe5ed] px-3 text-sm outline-none focus:border-[#087cf5]" /></label>{error && <p role="alert" className="text-xs font-semibold text-[#d9485f]">{error}</p>}<div className="flex gap-3 pt-3"><button type="button" className="flex-1 rounded-xl border border-[#dfe5ed] py-3 text-sm font-bold text-[#526177] hover:bg-[#f5f7fa]" onClick={onClose}>Maybe later</button><button disabled={isSubmitting} type="submit" className="flex-1 rounded-xl bg-[#087cf5] py-3 text-sm font-bold text-white hover:bg-[#006cdf] disabled:cursor-wait disabled:opacity-60">{isSubmitting ? 'Signing in...' : 'Sign in'}</button></div></form></div></div>
}
