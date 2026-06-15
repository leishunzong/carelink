/**
 * 简易 markdown 转 HTML，不依赖 npm，供聊天气泡等场景使用
 */
function escapeHtml(s) {
  if (s == null) return ''
  var str = String(s)
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function simpleMarkdownToHtml(content) {
  if (content == null || content === '') return ''
  var s = String(content).trim()
  
  // 预处理：统一换行符，处理连续空行
  s = s.replace(/\r\n/g, '\n').replace(/\r/g, '\n')
  // 将多个连续空行合并为两个空行（段落分隔）
  s = s.replace(/\n{3,}/g, '\n\n')
  
  var lines = s.split('\n')
  var out = []
  var inList = false
  var i = 0
  
  while (i < lines.length) {
    var line = lines[i]
    var trimmed = line.trim()
    
    // 空行处理 - 段落分隔
    if (!trimmed) {
      if (inList) { 
        out.push('</ul>')
        inList = false 
      }
      // 添加段落间距
      if (out.length > 0) {
        out.push('<div style="height: 16rpx;"></div>')
      }
      i++
      continue
    }
    
    // 列表项处理（支持 - 和 * 开头）
    var listMatch = /^[-*]\s+(.*)$/.exec(trimmed)
    if (listMatch) {
      if (!inList) { 
        out.push('<br/>')
        out.push('<ul>') 
        inList = true 
      }
      out.push('<li>' + inlineToHtml(listMatch[1]) + '</li>')
      i++
      continue
    }
    
    // 结束列表
    if (inList) { 
      out.push('</ul>')
      out.push('<br/>')
      inList = false 
    }
    
    // 标题处理
    if (/^#{1,6}\s/.test(trimmed)) {
      var level = 0
      while (trimmed[level] === '#') level++
      var head = trimmed.slice(level).trim()
      out.push('<br/>')
      out.push('<h' + level + '>' + inlineToHtml(head) + '</h' + level + '>')
      i++
      continue
    }
    
    // 普通段落
    out.push('<p>' + inlineToHtml(trimmed) + '</p>')
    i++
  }
  
  if (inList) out.push('</ul>')
  return out.join('')
}

function inlineToHtml(text) {
  if (!text) return ''
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/__(.+?)__/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/_(.+?)_/g, '<em>$1</em>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
}

module.exports = { simpleMarkdownToHtml: simpleMarkdownToHtml }
