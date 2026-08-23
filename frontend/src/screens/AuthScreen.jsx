import { useState } from 'react'
import { ArrowLeft, CheckCircle2, KeyRound, LockKeyhole, Mail, UserRound, X } from 'lucide-react'
import { apiFetch } from '../lib/api'

const modes = {
  signIn: { title: 'Welcome back', eyebrow: 'Sign in', description: 'Pick up where you left off on Otech.' },
  signUp: { title: 'Join Otech', eyebrow: 'Create account', description: 'Make your first find part of a more circular neighborhood.' },
  forgot: { title: 'Reset your password', eyebrow: 'Account recovery', description: 'We will help you get back into your account.' },
}

function Field({ label, icon: Icon, ...props }) {
  return <label className="block text-xs font-bold text-[#526177] dark:text-slate-300">
    {label}
    <span className="relative mt-1 block">
      <Icon size={17} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[#92a0b4]" />
      <input {...props} className="h-12 w-full rounded-xl border border-[#dfe5ed] bg-white pl-10 pr-3 text-sm text-[#172238] outline-none transition placeholder:text-[#9aa7b8] focus:border-[#087cf5] focus:ring-2 focus:ring-[#087cf5]/15 dark:border-slate-700 dark:bg-slate-900 dark:text-white" />
    </span>
  </label>
}

export default function AuthScreen({ initialMode = 'signIn', onClose, onSignedIn }) {
  const [mode, setMode] = useState(initialMode)
  const [email, setEmail] = useState('')
  const [fullName, setFullName] = useState('')
  const [password, setPassword] = useState('')
  const [otp, setOtp] = useState('')
  const [resetToken, setResetToken] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  const showError = (requestError) => setError(requestError.message || 'Something went wrong. Please try again.')
  const switchMode = (nextMode) => { setMode(nextMode); setError(''); setMessage('') }

  async function submit(event) {
    event.preventDefault()
    setError('')
    setMessage('')
    setIsSubmitting(true)
    try {
      let path = '/auth/login'
      let payload = { email, password }
      if (mode === 'signUp') { path = '/auth/signup'; payload = { fullName, email, password } }
      if (mode === 'verify') { path = '/auth/verify-signup'; payload = { email, otp } }
      if (mode === 'forgot') { path = '/auth/forgot-password'; payload = { email } }
      if (mode === 'reset') { path = '/auth/reset-password'; payload = { token: resetToken, newPassword: password } }

      const response = await apiFetch(path, { method: 'POST', body: JSON.stringify(payload) })
      const body = await response.json().catch(() => ({}))
      if (!response.ok) {
        const responseMessage = body.message || body.error || ''
        if (responseMessage.includes('signup_otp_tokens') || responseMessage.includes('relation')) {
          throw new Error('Signup is temporarily unavailable')
        }
        throw new Error(responseMessage || 'Request failed')
      }
      if (mode === 'signIn' || mode === 'verify') { onSignedIn(body); return }
      if (mode === 'signUp') { setMode('verify'); setMessage('A 6-digit verification code was sent to your email.'); return }
      if (mode === 'forgot') { setResetToken(''); setMode('reset'); setMessage('Check your email for your 6-digit reset code.'); return }
      setMode('signIn')
      setMessage('Password updated. You can sign in now.')
    } catch (requestError) { showError(requestError) } finally { setIsSubmitting(false) }
  }

  const isVerification = mode === 'verify'
  const isReset = mode === 'reset'
  const content = modes[mode] || (isVerification ? { title: 'Verify your email', eyebrow: 'One more step', description: 'Enter the code we sent to finish creating your account.' } : { title: 'Choose a new password', eyebrow: 'Almost there', description: 'Enter the reset token and your new password.' })

  return <div className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-[#172238]/55 p-4 backdrop-blur-sm">
    <div className="relative my-4 w-full max-w-[460px] rounded-[26px] bg-white p-7 shadow-2xl dark:bg-slate-950 sm:p-9">
      <button aria-label="Close" onClick={onClose} className="absolute right-5 top-5 rounded-xl p-2 text-[#8290a4] transition hover:bg-[#f5f7fa] dark:hover:bg-slate-800"><X size={19} /></button>
      <div className="mb-7 flex items-center gap-3"><span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-[#087cf5] text-lg font-black text-white">O</span><span className="text-lg font-black tracking-[-0.04em] text-[#172238] dark:text-white">otech<span className="text-[#087cf5]">.</span></span></div>
      {!isVerification && !isReset && mode !== 'forgot' && <div className="mb-7 flex rounded-xl bg-[#f5f7fa] p-1 dark:bg-slate-900"><button onClick={() => switchMode('signIn')} className={`flex-1 rounded-lg py-2 text-xs font-bold ${mode === 'signIn' ? 'bg-white text-[#172238] shadow-sm dark:bg-slate-700 dark:text-white' : 'text-[#8290a4]'}`}>Sign in</button><button onClick={() => switchMode('signUp')} className={`flex-1 rounded-lg py-2 text-xs font-bold ${mode === 'signUp' ? 'bg-white text-[#172238] shadow-sm dark:bg-slate-700 dark:text-white' : 'text-[#8290a4]'}`}>Sign up</button></div>}
      <p className="mb-2 text-xs font-bold uppercase tracking-[0.18em] text-[#087cf5]">{content.eyebrow}</p>
      <h2 className="text-3xl font-extrabold tracking-[-0.05em] text-[#172238] dark:text-white">{content.title}</h2>
      <p className="mt-2 text-sm leading-6 text-[#718097] dark:text-slate-400">{content.description}</p>
      <form onSubmit={submit} className="mt-7 space-y-4">
        {mode === 'signUp' && <Field label="Full name" icon={UserRound} value={fullName} onChange={(event) => setFullName(event.target.value)} required placeholder="Your name" />}
        {(mode !== 'reset') && <Field label="Email" icon={Mail} type="email" value={email} onChange={(event) => setEmail(event.target.value)} required placeholder="you@example.com" />}
        {mode === 'verify' && <><Field label="Verification code" icon={KeyRound} inputMode="numeric" pattern="[0-9]{6}" maxLength="6" value={otp} onChange={(event) => setOtp(event.target.value)} required placeholder="6 digits" /><button type="button" onClick={async () => { setError(''); setMessage(''); try { const response = await apiFetch('/auth/resend-signup-otp', { method: 'POST', body: JSON.stringify({ email }) }); const body = await response.json().catch(() => ({})); if (!response.ok) throw new Error(body.message || 'Could not resend code'); setMessage('A new verification code was sent.') } catch (requestError) { showError(requestError) } }} className="text-xs font-bold text-[#087cf5] hover:underline">Resend verification code</button></>}
        {isReset && <Field label="Reset code" icon={KeyRound} inputMode="numeric" pattern="[0-9]{6}" maxLength="6" value={resetToken} onChange={(event) => setResetToken(event.target.value)} required placeholder="Enter the 6-digit code" />}
        {['signIn', 'signUp', 'reset'].includes(mode) && <Field label={isReset ? 'New password' : 'Password'} icon={LockKeyhole} type="password" minLength="8" value={password} onChange={(event) => setPassword(event.target.value)} required placeholder="At least 8 characters" />}
        {error && <p role="alert" className="text-sm font-semibold text-[#d9485f]">{error}</p>}
        {message && <p className="flex gap-2 text-sm font-semibold text-[#16835b]"><CheckCircle2 size={17} className="mt-0.5 shrink-0" />{message}</p>}
        {mode === 'signIn' && <button type="button" onClick={() => switchMode('forgot')} className="text-xs font-bold text-[#087cf5] hover:underline">Forgot password?</button>}
        <button disabled={isSubmitting} type="submit" className="flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-[#087cf5] text-sm font-bold text-white shadow-[0_8px_20px_rgba(8,124,245,0.2)] transition hover:bg-[#006cdf] disabled:cursor-wait disabled:opacity-60">{isSubmitting ? 'Please wait...' : mode === 'signIn' ? 'Sign in' : mode === 'signUp' ? 'Create account' : mode === 'verify' ? 'Verify and enter' : mode === 'forgot' ? 'Send reset link' : 'Update password'}</button>
      </form>
      {(isVerification || isReset || mode === 'forgot') && <button onClick={() => switchMode('signIn')} className="mt-5 flex w-full items-center justify-center gap-2 text-xs font-bold text-[#718097] hover:text-[#087cf5]"><ArrowLeft size={15} /> Back to sign in</button>}
    </div>
  </div>
}
