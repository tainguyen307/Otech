import { useEffect, useRef, useState } from 'react'
import { CheckCircle2, CreditCard, ImagePlus, LoaderCircle, RefreshCw, X } from 'lucide-react'
import { apiErrorMessage, apiFetch } from '../lib/api'

const money = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 })

function requestMessage(request) {
  if (request.status === 'APPROVED') return 'Đã duyệt'
  if (request.status === 'REJECTED') return 'Bị từ chối'
  return 'Chờ Manager/Admin xác nhận'
}

export default function BillingCenter({ user, onAuth, open, onClose, productToBoost, refreshKey: externalRefreshKey = 0 }) {
  const dialogRef = useRef(null)
  const [catalog, setCatalog] = useState(null)
  const [requests, setRequests] = useState([])
  const [activeRequest, setActiveRequest] = useState(null)
  const [transferReference, setTransferReference] = useState('')
  const [proofImageUrl, setProofImageUrl] = useState('')
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [isUploading, setIsUploading] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [refreshKey, setRefreshKey] = useState(0)
  const userId = user?.id

  useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog) return
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
  }, [open])

  useEffect(() => {
    if (!userId) return undefined
    const controller = new AbortController()
    Promise.all([
      apiFetch('/billing/catalog', { signal: controller.signal }),
      apiFetch('/billing/payment-requests', { signal: controller.signal }),
    ])
      .then(async ([catalogResponse, requestResponse]) => {
        if (!catalogResponse.ok) throw new Error(await apiErrorMessage(catalogResponse, 'Không thể tải gói dịch vụ.'))
        if (!requestResponse.ok) throw new Error(await apiErrorMessage(requestResponse, 'Không thể tải yêu cầu thanh toán.'))
        return Promise.all([catalogResponse.json(), requestResponse.json()])
      })
      .then(([catalogBody, requestBody]) => {
        if (controller.signal.aborted) return
        setCatalog(catalogBody)
        const paymentRequests = Array.isArray(requestBody) ? requestBody : []
        setRequests(paymentRequests)
        setActiveRequest((current) => current
          ? paymentRequests.find((request) => request.id === current.id) || current
          : current)
      })
      .catch((requestError) => {
        if (!controller.signal.aborted) setError(requestError.message || 'Không thể tải thông tin thanh toán.')
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsLoading(false)
      })
    return () => controller.abort()
  }, [externalRefreshKey, refreshKey, userId])

  async function createRequest(billingPackage) {
    if (!user) {
      onAuth()
      return
    }
    setIsSaving(true)
    setError('')
    setNotice('')
    try {
      const response = await apiFetch('/billing/payment-requests', {
        method: 'POST',
        body: JSON.stringify({
          packageId: billingPackage.id,
          productId: productToBoost?.id || null,
        }),
      })
      if (!response.ok) throw new Error(await apiErrorMessage(response, 'Không thể tạo yêu cầu thanh toán.'))
      const request = await response.json()
      setActiveRequest(request)
      setTransferReference('')
      setProofImageUrl('')
      setRequests((current) => [request, ...current])
      setNotice('Yêu cầu đã tạo. Chuyển khoản đúng số tiền và nội dung bên dưới; gói sẽ có hiệu lực sau khi được duyệt.')
    } catch (requestError) {
      setError(requestError.message || 'Không thể tạo yêu cầu thanh toán.')
    } finally {
      setIsSaving(false)
    }
  }

  async function uploadProof(event) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    setIsUploading(true)
    setError('')
    try {
      const body = new FormData()
      body.append('file', file)
      const response = await apiFetch('/products/images', { method: 'POST', body })
      if (!response.ok) throw new Error(await apiErrorMessage(response, 'Không thể tải biên lai.'))
      const result = await response.json()
      if (!result.imageUrl) throw new Error('Dịch vụ ảnh không trả về liên kết biên lai.')
      setProofImageUrl(result.imageUrl)
    } catch (requestError) {
      setError(requestError.message || 'Không thể tải biên lai.')
    } finally {
      setIsUploading(false)
    }
  }

  async function submitProof(event) {
    event.preventDefault()
    if (!activeRequest || (!transferReference.trim() && !proofImageUrl)) {
      setError('Nhập mã giao dịch ngân hàng hoặc tải ảnh biên lai.')
      return
    }
    setIsSaving(true)
    setError('')
    try {
      const response = await apiFetch(`/billing/payment-requests/${activeRequest.id}/proof`, {
        method: 'PATCH',
        body: JSON.stringify({
          transferReference: transferReference.trim() || null,
          proofImageUrl: proofImageUrl || null,
        }),
      })
      if (!response.ok) throw new Error(await apiErrorMessage(response, 'Không thể lưu biên lai.'))
      const updated = await response.json()
      setActiveRequest(updated)
      setRequests((current) => current.map((item) => item.id === updated.id ? updated : item))
      setNotice('Đã gửi thông tin chuyển khoản. Manager/Admin sẽ đối soát thủ công.')
    } catch (requestError) {
      setError(requestError.message || 'Không thể lưu biên lai.')
    } finally {
      setIsSaving(false)
    }
  }

  const packages = (catalog?.packages || []).filter((item) =>
    productToBoost ? item.packageType === 'BOOST' : item.packageType === 'LISTING_CREDITS')

  return <>
    {user && <div className="mb-6 flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-[#dce8f5] bg-white px-4 py-3 dark:border-slate-800 dark:bg-slate-900">
      <div className="flex items-center gap-3">
        <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#eaf3ff] text-[#087cf5] dark:bg-blue-950/50 dark:text-blue-300"><CreditCard size={19} /></span>
        <div><p className="text-sm font-extrabold text-[#172238] dark:text-white">Đăng tin & gói đẩy tin</p><p className="mt-0.5 text-xs text-[#718097] dark:text-slate-400">{catalog ? `Miễn phí ${catalog.freeListingsPerDay} tin/ngày · đã dùng ${catalog.freeListingsUsedToday} · ${catalog.availableListingCredits} lượt trả phí còn hạn` : 'Đang tải hạn mức đăng tin…'}</p></div>
      </div>
      <button type="button" onClick={() => { setError(''); setNotice(''); if (!user) onAuth(); else if (dialogRef.current && !dialogRef.current.open) dialogRef.current.showModal() }} className="inline-flex min-h-10 items-center gap-2 rounded-xl border border-[#dfe6ef] px-3 text-xs font-bold text-[#526078] hover:bg-[#f5f7fa] dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800"><CreditCard size={14} />Xem gói</button>
    </div>}

    <dialog ref={dialogRef} onClose={onClose} aria-labelledby="billing-title" className="fixed inset-0 m-auto max-h-[92vh] w-[min(760px,calc(100%-24px))] overflow-y-auto rounded-2xl border border-[#e3e8f0] bg-white p-0 text-[#172238] shadow-2xl backdrop:bg-slate-950/60 dark:border-slate-700 dark:bg-slate-900 dark:text-white">
      <div className="flex items-start justify-between gap-4 border-b border-[#eef1f5] p-5 dark:border-slate-800 sm:p-6">
        <div><p className="text-xs font-bold uppercase tracking-[0.16em] text-[#087cf5]">Otech · Dịch vụ đăng tin</p><h2 id="billing-title" className="mt-1 text-xl font-extrabold">{productToBoost ? `Đẩy tin: ${productToBoost.title}` : 'Gói lượt đăng'}</h2><p className="mt-1 text-sm text-[#718097] dark:text-slate-400">Thanh toán QR thủ công; quyền lợi chỉ được cấp sau khi Manager/Admin duyệt.</p></div>
        <button type="button" onClick={onClose} aria-label="Đóng gói dịch vụ" className="rounded-lg p-2 text-[#718097] hover:bg-[#f5f7fa] dark:hover:bg-slate-800"><X size={18} /></button>
      </div>
      <div className="space-y-5 p-5 sm:p-6">
        {error && <p role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-sm text-rose-700 dark:border-rose-900 dark:bg-rose-950/30 dark:text-rose-300">{error}</p>}
        {notice && <p role="status" className="rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-700 dark:border-emerald-900 dark:bg-emerald-950/30 dark:text-emerald-300">{notice}</p>}
        {isLoading && <div className="flex justify-center py-8 text-sm text-[#718097]"><LoaderCircle size={18} className="mr-2 animate-spin" />Đang tải gói…</div>}
        {!isLoading && !catalog?.qrImageUrl && <p className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800 dark:border-amber-900 dark:bg-amber-950/30 dark:text-amber-300">Quản trị viên chưa cấu hình mã QR thanh toán. Vui lòng quay lại sau.</p>}
        {!isLoading && catalog?.qrImageUrl && <div className="grid gap-3 sm:grid-cols-2">
          {packages.map((billingPackage) => <article key={billingPackage.id} className="rounded-xl border border-[#e3e8f0] p-4 dark:border-slate-700">
            <h3 className="font-extrabold">{billingPackage.name}</h3>
            <p className="mt-1 min-h-10 text-sm leading-5 text-[#718097] dark:text-slate-400">{billingPackage.description || (billingPackage.packageType === 'BOOST' ? `Đẩy tin trong ${billingPackage.durationHours} giờ.` : `${billingPackage.listingCredits} lượt đăng, hiệu lực ${billingPackage.durationHours} giờ.`)}</p>
            <p className="mt-3 text-lg font-extrabold text-[#087cf5]">{money.format(Number(billingPackage.price))}</p>
            <button type="button" disabled={isSaving || Boolean(productToBoost) !== (billingPackage.packageType === 'BOOST')} onClick={() => createRequest(billingPackage)} className="mt-3 inline-flex min-h-10 w-full items-center justify-center gap-2 rounded-lg bg-[#087cf5] px-3 text-xs font-bold text-white hover:bg-[#006cdf] disabled:opacity-50">{isSaving && <LoaderCircle size={14} className="animate-spin" />}Tạo yêu cầu thanh toán</button>
          </article>)}
          {packages.length === 0 && <p className="rounded-xl bg-[#f5f7fa] p-4 text-sm text-[#718097] dark:bg-slate-800 dark:text-slate-400">Chưa có gói phù hợp được quản trị viên cấu hình.</p>}
        </div>}

        {activeRequest?.status === 'PENDING' && <section className="rounded-xl border border-blue-200 bg-blue-50 p-4 dark:border-blue-900 dark:bg-blue-950/25">
          <div className="grid gap-4 sm:grid-cols-[140px_1fr]">
            <img src={catalog?.qrImageUrl} alt="Mã QR thanh toán Otech" className="mx-auto aspect-square w-36 rounded-lg bg-white object-contain p-2" />
            <div className="min-w-0">
              <h3 className="font-extrabold">Thông tin chuyển khoản</h3>
              <dl className="mt-2 grid grid-cols-[auto_1fr] gap-x-3 gap-y-1 text-sm"><dt className="text-[#718097]">Số tiền</dt><dd className="font-bold">{money.format(Number(activeRequest.amount))}</dd><dt className="text-[#718097]">Nội dung</dt><dd className="break-all font-mono font-bold text-[#087cf5]">{activeRequest.paymentCode}</dd><dt className="text-[#718097]">Gói</dt><dd>{activeRequest.packageName}</dd></dl>
              <p className="mt-2 text-xs leading-5 text-[#526078] dark:text-slate-300">Ghi đúng nội dung để đối soát. Không đăng thêm tin trả phí hoặc đẩy tin trước khi yêu cầu được duyệt.</p>
            </div>
          </div>
          <form onSubmit={submitProof} className="mt-4 grid gap-3 border-t border-blue-200 pt-4 dark:border-blue-900 sm:grid-cols-[1fr_auto]">
            <label className="text-xs font-bold text-[#526078] dark:text-slate-300">Mã giao dịch ngân hàng
              <input value={transferReference} onChange={(event) => setTransferReference(event.target.value)} maxLength="120" className="mt-1.5 min-h-10 w-full rounded-lg border border-[#dfe6ef] bg-white px-3 text-sm font-normal dark:border-slate-700 dark:bg-slate-900" placeholder="Có thể bổ sung sau khi chuyển khoản" />
            </label>
            <div className="flex items-end gap-2">
              <label className={`inline-flex min-h-10 cursor-pointer items-center gap-2 rounded-lg border border-[#dfe6ef] px-3 text-xs font-bold ${isUploading ? 'pointer-events-none opacity-50' : ''}`}><ImagePlus size={14} />{isUploading ? 'Đang tải…' : proofImageUrl ? 'Đã tải biên lai' : 'Tải biên lai'}<input type="file" accept="image/jpeg,image/png,image/webp" disabled={isUploading} onChange={uploadProof} className="sr-only" /></label>
              <button type="submit" disabled={isSaving || isUploading || (!transferReference.trim() && !proofImageUrl)} className="inline-flex min-h-10 items-center gap-2 rounded-lg bg-[#172238] px-3 text-xs font-bold text-white disabled:opacity-50 dark:bg-slate-700">{isSaving && <LoaderCircle size={14} className="animate-spin" />}Gửi đối soát</button>
            </div>
          </form>
        </section>}

        <section>
          <div className="mb-2 flex items-center justify-between gap-3"><h3 className="font-extrabold">Lịch sử yêu cầu</h3><button type="button" onClick={() => setRefreshKey((value) => value + 1)} aria-label="Tải lại yêu cầu thanh toán" className="rounded-lg p-2 text-[#718097] hover:bg-[#f5f7fa] dark:hover:bg-slate-800"><RefreshCw size={15} /></button></div>
          {requests.length === 0 ? <p className="text-sm text-[#718097] dark:text-slate-400">Chưa có yêu cầu thanh toán.</p> : <ul className="max-h-60 space-y-2 overflow-y-auto">{requests.map((request) => <li key={request.id} className="flex flex-wrap items-center justify-between gap-2 rounded-lg bg-[#f5f7fa] px-3 py-2 text-sm dark:bg-slate-800">
            {request.status === 'PENDING' ? <button type="button" onClick={() => { setActiveRequest(request); setTransferReference(request.transferReference || ''); setProofImageUrl(request.proofImageUrl || ''); setError(''); setNotice('') }} className="min-w-0 text-left"><span className="font-semibold">{request.packageName}</span><span className="ml-2 font-mono text-xs text-[#718097]">{request.paymentCode}</span><span className="ml-2 text-xs font-bold text-[#087cf5]">Tiếp tục thanh toán</span></button> : <span className="min-w-0"><span className="font-semibold">{request.packageName}</span><span className="ml-2 font-mono text-xs text-[#718097]">{request.paymentCode}</span></span>}
            <span className="inline-flex items-center gap-1.5 text-xs font-bold text-[#526078] dark:text-slate-300">{request.status === 'APPROVED' && <CheckCircle2 size={14} className="text-emerald-600" />}{requestMessage(request)}</span>
          </li>)}</ul>}
        </section>
      </div>
    </dialog>
  </>
}
