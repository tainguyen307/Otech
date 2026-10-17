import { MapPin, MessageCircle, Pencil, Tag } from 'lucide-react'

const priceFormatter = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
})

const conditionLabels = {
  NEW: 'New',
  LIKE_NEW: 'Like new',
  GOOD: 'Good',
  FAIR: 'Fair',
  POOR: 'Well loved',
}

export default function ProductCard({ product, isOwner, onOpen, onEdit, onStartChat, onStatusChange }) {
  const imageUrl = product.imageUrls?.[0]
  return <article className="overflow-hidden rounded-2xl border border-[#e3e8f0] bg-white shadow-sm transition hover:-translate-y-0.5 hover:shadow-md dark:border-slate-800 dark:bg-slate-900">
    <button type="button" onClick={onOpen} className="block w-full text-left focus-visible:outline-2 focus-visible:outline-offset-[-3px] focus-visible:outline-[#087cf5]">
      <div className="relative aspect-[4/3] overflow-hidden bg-[#eef3f8] dark:bg-slate-800">
        {imageUrl ? <img src={imageUrl} alt={product.title} loading="lazy" className="h-full w-full object-cover transition duration-300 hover:scale-[1.03]" /> : <div className="flex h-full items-center justify-center text-sm font-semibold text-[#718097] dark:text-slate-400">No photo yet</div>}
        <span className="absolute left-3 top-3 rounded-full bg-white/95 px-3 py-1 text-xs font-bold text-[#34445d] shadow-sm dark:bg-slate-950/90 dark:text-slate-200">{conditionLabels[product.condition] || 'Pre-loved'}</span>
      </div>
      <div className="p-4">
        <p className="line-clamp-1 text-base font-extrabold text-[#172238] dark:text-white">{product.title}</p>
        <p className="mt-2 text-lg font-extrabold text-[#087cf5]">{priceFormatter.format(Number(product.price))}</p>
        <div className="mt-3 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-[#718097] dark:text-slate-400">
          {product.categoryName && <span className="inline-flex items-center gap-1"><Tag size={13} />{product.categoryName}</span>}
          {product.location && <span className="inline-flex items-center gap-1"><MapPin size={13} />{product.location}</span>}
        </div>
        <div className="mt-4 flex items-center gap-2 border-t border-[#eef1f5] pt-3 dark:border-slate-800">
          {product.seller?.avatarUrl ? <img src={product.seller.avatarUrl} alt="" className="h-7 w-7 rounded-full object-cover" /> : <span aria-hidden="true" className="flex h-7 w-7 items-center justify-center rounded-full bg-[#eaf3ff] text-xs font-bold text-[#087cf5]">{product.seller?.fullName?.[0] || 'O'}</span>}
          <span className="truncate text-xs font-semibold text-[#526078] dark:text-slate-300">{product.seller?.fullName || 'Otech member'}</span>
          {product.status !== 'SELLING' && <span className="ml-auto rounded-full bg-slate-100 px-2.5 py-1 text-[10px] font-bold uppercase tracking-wide text-slate-500 dark:bg-slate-800 dark:text-slate-300">{product.status === 'SOLD' ? 'Sold' : 'Hidden'}</span>}
        </div>
      </div>
    </button>
    <div className="flex gap-2 px-4 pb-4">
      {isOwner ? <>
        <button type="button" onClick={onEdit} className="inline-flex min-h-10 flex-1 items-center justify-center gap-2 rounded-xl border border-[#dfe6ef] px-3 text-xs font-bold text-[#526078] hover:bg-[#f5f7fa] dark:border-slate-700 dark:text-slate-300 dark:hover:bg-slate-800"><Pencil size={14} />Edit</button>
        {product.status === 'SELLING'
          ? <button type="button" onClick={() => onStatusChange('SOLD')} className="min-h-10 flex-1 rounded-xl bg-[#eaf3ff] px-3 text-xs font-bold text-[#087cf5] hover:bg-blue-100 dark:bg-blue-950/50 dark:text-blue-300 dark:hover:bg-blue-950">Mark sold</button>
          : <button type="button" onClick={() => onStatusChange('SELLING')} className="min-h-10 flex-1 rounded-xl bg-[#eaf3ff] px-3 text-xs font-bold text-[#087cf5] hover:bg-blue-100 dark:bg-blue-950/50 dark:text-blue-300 dark:hover:bg-blue-950">Relist</button>}
      </> : product.status === 'SELLING' && <button type="button" onClick={onStartChat} className="inline-flex min-h-10 w-full items-center justify-center gap-2 rounded-xl bg-[#087cf5] px-3 text-xs font-bold text-white hover:bg-[#006cdf]"><MessageCircle size={15} />Message seller</button>}
    </div>
  </article>
}
