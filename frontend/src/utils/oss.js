/**
 * 阿里云 OSS 图片处理
 * 对 OSS 存储的图片追加 x-oss-process 参数实现 WebP 转码 + 压缩
 */

// 需要追加处理的 OSS URL 关键词（bucket名或域名）
const OSS_MARKERS = ['aliyuncs.com', 'yuzhongblog']

/**
 * 给 OSS 图片 URL 追加图片处理参数（WebP + 90%质量）
 * 非 OSS URL 原样返回（如 B站封面、网易云封面）
 * @param {string} url - 原始图片 URL
 * @returns {string} 处理后的 URL
 */
export function ossImg(url) {
  if (!url || typeof url !== 'string') return url
  // 只对 OSS URL 追加处理
  if (!OSS_MARKERS.some(m => url.includes(m))) return url
  // 已经有处理参数的不重复追加
  if (url.includes('x-oss-process')) return url
  const sep = url.includes('?') ? '&' : '?'
  return `${url}${sep}x-oss-process=image/format,webp/quality,q_90`
}
