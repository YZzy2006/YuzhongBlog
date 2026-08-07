const cache = new Map()

export function cachedFetch(key, fetcher, ttlMs = 60000) {
  const entry = cache.get(key)
  if (entry && Date.now() - entry.time < ttlMs) return entry.promise
  const promise = fetcher().catch(e => { cache.delete(key); throw e })
  cache.set(key, { promise, time: Date.now() })
  return promise
}

export function invalidateCache(keys) {
  keys.forEach(k => cache.delete(k))
}
