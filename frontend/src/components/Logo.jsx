export default function Logo({ onClick }) {
  const content = <><span className="flex h-9 w-9 items-center justify-center rounded-xl bg-[#087cf5] text-sm font-black text-white">O</span><span className="text-xl font-black tracking-[-0.05em] text-[#172238] dark:text-white">otech<span className="text-[#087cf5]">.</span></span></>
  return onClick ? <button type="button" aria-label="Go to Otech home" onClick={onClick} className="flex items-center gap-2.5 rounded-xl text-left transition hover:opacity-80 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[#087cf5]">{content}</button> : <div className="flex items-center gap-2.5">{content}</div>
}
