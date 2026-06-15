/**
 * 将 Markdown 解析为小程序可用的结构化数据
 */

function parseMarkdownToBlocks(content) {
  if (!content) return []
  
  var text = String(content).trim()
  var lines = text.split(/\r?\n/)
  var blocks = []
  var currentList = null
  
  for (var i = 0; i < lines.length; i++) {
    var line = lines[i]
    var trimmed = line.trim()
    
    // 空行处理
    if (!trimmed) {
      if (currentList) {
        blocks.push(currentList)
        currentList = null
      }
      if (blocks.length > 0) {
        blocks.push({ type: 'break' })
      }
      continue
    }
    
    // 列表项处理
    var listMatch = /^[-*]\s+(.+)$/.exec(trimmed)
    if (listMatch) {
      var listContent = parseInlineMarkdown(listMatch[1])
      if (!currentList) {
        currentList = { type: 'list', items: [] }
      }
      currentList.items.push(listContent)
      continue
    }
    
    // 如果之前有列表，先添加到 blocks
    if (currentList) {
      blocks.push(currentList)
      currentList = null
    }
    
    // 标题处理
    var titleMatch = /^(#{1,6})\s+(.+)$/.exec(trimmed)
    if (titleMatch) {
      var level = titleMatch[1].length
      var titleContent = parseInlineMarkdown(titleMatch[2])
      blocks.push({ 
        type: 'title', 
        level: level,
        content: titleContent 
      })
      continue
    }
    
    // 普通段落
    var paragraphContent = parseInlineMarkdown(trimmed)
    blocks.push({ 
      type: 'paragraph', 
      content: paragraphContent 
    })
  }
  
  // 处理最后的列表
  if (currentList) {
    blocks.push(currentList)
  }
  
  return blocks
}

function parseInlineMarkdown(text) {
  if (!text) return ''
  
  var parts = []
  var current = text
  
  // 处理粗体 **text** 和 __text__
  current = current.replace(/\*\*(.+?)\*\*/g, '||BOLD||$1||/BOLD||')
  current = current.replace(/__(.+?)__/g, '||BOLD||$1||/BOLD||')
  
  // 处理斜体 *text* 和 _text_
  current = current.replace(/\*(.+?)\*/g, '||ITALIC||$1||/ITALIC||')
  current = current.replace(/_(.+?)_/g, '||ITALIC||$1||/ITALIC||')
  
  // 处理代码 `text`
  current = current.replace(/`([^`]+)`/g, '||CODE||$1||/CODE||')
  
  // 分割并标记
  var segments = current.split(/\|\|/)
  var result = { text: '', isBold: false, isItalic: false, isCode: false }
  
  for (var i = 0; i < segments.length; i++) {
    var segment = segments[i]
    if (segment === 'BOLD') {
      result.isBold = true
    } else if (segment === '/BOLD') {
      result.isBold = false
    } else if (segment === 'ITALIC') {
      result.isItalic = true
    } else if (segment === '/ITALIC') {
      result.isItalic = false
    } else if (segment === 'CODE') {
      result.isCode = true
    } else if (segment === '/CODE') {
      result.isCode = false
    } else if (segment) {
      result.text += segment
    }
  }
  
  // 简化：直接返回文本，粗体用特殊标记
  return current.replace(/\|\|BOLD\|\|(.+?)\|\|\/BOLD\|\|/g, '【$1】')
                .replace(/\|\|ITALIC\|\|(.+?)\|\|\/ITALIC\|\|/g, '〈$1〉')
                .replace(/\|\|CODE\|\|(.+?)\|\|\/CODE\|\|/g, '`$1`')
}

module.exports = {
  parseMarkdownToBlocks: parseMarkdownToBlocks
}