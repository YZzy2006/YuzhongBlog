/**
 * Strip markdown syntax from text for plain-text display (e.g. article summaries)
 */
export function stripMarkdown(text) {
  if (!text) return ''
  return text
    // Code blocks
    .replace(/```[\s\S]*?```/g, '')
    // Inline code
    .replace(/`([^`]*)`/g, '$1')
    // Images
    .replace(/!\[[^\]]*\]\([^)]*\)/g, '')
    // Links
    .replace(/\[([^\]]*)\]\([^)]*\)/g, '$1')
    // Headings
    .replace(/^#{1,6}\s+/gm, '')
    // Bold
    .replace(/\*\*(.+?)\*\*/g, '$1')
    .replace(/__(.+?)__/g, '$1')
    // Italic
    .replace(/\*(.+?)\*/g, '$1')
    .replace(/_(.+?)_/g, '$1')
    // Strikethrough
    .replace(/~~(.+?)~~/g, '$1')
    // Blockquotes
    .replace(/^>\s+/gm, '')
    // Horizontal rules
    .replace(/^[-*_]{3,}\s*$/gm, '')
    // Unordered list markers
    .replace(/^[\s]*[-*+]\s+/gm, '')
    // Ordered list markers
    .replace(/^[\s]*\d+\.\s+/gm, '')
    // HTML tags
    .replace(/<[^>]+>/g, '')
    // Collapse whitespace
    .replace(/\n{2,}/g, ' ')
    .replace(/\n/g, ' ')
    .replace(/\s{2,}/g, ' ')
    .trim()
}
