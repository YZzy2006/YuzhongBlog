// CSS-based hover visibility for copy buttons
if (typeof document !== 'undefined' && !document.getElementById('cb-copy-hover-style')) {
  const style = document.createElement('style')
  style.id = 'cb-copy-hover-style'
  style.textContent = 'pre:hover .cb-copy-btn{opacity:1!important}'
  document.head.appendChild(style)
}

/**
 * Inject copy buttons into all <pre> code blocks inside a container.
 * Hover visibility is handled by CSS (pre:hover .cb-copy-btn).
 * Uses event delegation for click handling only.
 *
 * @param {HTMLElement} container - The wrapper element containing rendered HTML
 * @param {object} [opts]
 * @param {number} [opts.borderRadius=6] - Border radius of the copy button in px
 */
export function injectCopyButtons(container, opts = {}) {
  if (!container) return
  const borderRadius = opts.borderRadius ?? 6
  const pres = container.querySelectorAll('pre')
  pres.forEach(pre => {
    if (pre.querySelector('.cb-copy-btn')) return
    if (getComputedStyle(pre).position === 'static') {
      pre.style.position = 'relative'
    }
    const btn = document.createElement('button')
    btn.className = 'cb-copy-btn'
    btn.innerHTML = `<svg viewBox="0 0 6.35 6.35" height="16" width="16" xmlns="http://www.w3.org/2000/svg"><g><path fill="currentColor" d="M2.43.265c-.3 0-.548.236-.573.53h-.328a.74.74 0 0 0-.735.734v3.822a.74.74 0 0 0 .735.734H4.82a.74.74 0 0 0 .735-.734V1.529a.74.74 0 0 0-.735-.735h-.328a.58.58 0 0 0-.573-.53zm0 .529h1.49c.032 0 .049.017.049.049v.431c0 .032-.017.049-.049.049H2.43c-.032 0-.05-.017-.05-.049V.843c0-.032.018-.05.05-.05zm-.901.53h.328c.026.292.274.528.573.528h1.49a.58.58 0 0 0 .573-.529h.328a.2.2 0 0 1 .206.206v3.822a.2.2 0 0 1-.206.205H1.53a.2.2 0 0 1-.206-.205V1.529a.2.2 0 0 1 .206-.206z"/></g></svg>`
    btn.style.cssText = `position:absolute;top:6px;right:6px;width:28px;height:28px;border-radius:${borderRadius}px;background:#353434;color:#ccc;border:none;cursor:pointer;display:flex;align-items:center;justify-content:center;opacity:0;transition:opacity 0.2s;z-index:5;outline:none;`
    pre.appendChild(btn)
  })

  // Use event delegation on container — one click listener for all <pre> blocks
  if (!container._copyDelegated) {
    container._copyDelegated = true
    container.addEventListener('click', async (e) => {
      const btn = e.target.closest('.cb-copy-btn')
      if (!btn || !container.contains(btn)) return
      const pre = btn.closest('pre')
      if (!pre) return
      const code = pre.querySelector('code')?.innerText || pre.innerText
      try {
        await navigator.clipboard.writeText(code)
      } catch {
        const ta = document.createElement('textarea')
        ta.value = code
        ta.style.cssText = 'position:fixed;left:-9999px'
        document.body.appendChild(ta)
        ta.select()
        document.execCommand('copy')
        ta.remove()
      }
      btn.innerHTML = `<svg viewBox="0 0 24 24" height="14" width="14" xmlns="http://www.w3.org/2000/svg"><g><path fill="#4ade80" d="M9.707 19.121a.997.997 0 0 1-1.414 0l-5.646-5.647a1.5 1.5 0 0 1 0-2.121l.707-.707a1.5 1.5 0 0 1 2.121 0L9 14.171l9.525-9.525a1.5 1.5 0 0 1 2.121 0l.707.707a1.5 1.5 0 0 1 0 2.121z"/></g></svg>`
      btn.style.opacity = '1'
      setTimeout(() => {
        btn.innerHTML = `<svg viewBox="0 0 6.35 6.35" height="16" width="16" xmlns="http://www.w3.org/2000/svg"><g><path fill="currentColor" d="M2.43.265c-.3 0-.548.236-.573.53h-.328a.74.74 0 0 0-.735.734v3.822a.74.74 0 0 0 .735.734H4.82a.74.74 0 0 0 .735-.734V1.529a.74.74 0 0 0-.735-.735h-.328a.58.58 0 0 0-.573-.53zm0 .529h1.49c.032 0 .049.017.049.049v.431c0 .032-.017.049-.049.049H2.43c-.032 0-.05-.017-.05-.049V.843c0-.032.018-.05.05-.05zm-.901.53h.328c.026.292.274.528.573.528h1.49a.58.58 0 0 0 .573-.529h.328a.2.2 0 0 1 .206.206v3.822a.2.2 0 0 1-.206.205H1.53a.2.2 0 0 1-.206-.205V1.529a.2.2 0 0 1 .206-.206z"/></g></svg>`
        btn.style.opacity = '0'
      }, 2000)
    })
  }
}

/**
 * Remove injected copy buttons and clean up.
 */
export function cleanupInjected(container) {
  if (!container) return
  container.querySelectorAll('.cb-copy-btn').forEach(btn => btn.remove())
  container._copyDelegated = false
}
