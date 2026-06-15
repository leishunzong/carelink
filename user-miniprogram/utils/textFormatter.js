/**
 * 简单的文本格式化，将 Markdown 转为带格式的纯文本
 */

function formatMarkdownText(content) {
  if (!content) return ''
  
  var text = String(content)
  
  // 处理粗体：**text** -> 【text】
  text = text.replace(/\*\*(.+?)\*\*/g, '【$1】')
  text = text.replace(/__(.+?)__/g, '【$1】')
  
  // 处理斜体：*text* -> 〈text〉
  text = text.replace(/\*([^*]+?)\*/g, '〈$1〉')
  text = text.replace(/_([^_]+?)_/g, '〈$1〉')
  
  // 处理代码：`text` -> 「text」
  text = text.replace(/`([^`]+)`/g, '「$1」')
  
  // 处理列表项：在每个 "- " 前添加适当的缩进和项目符号
  text = text.replace(/^[-*]\s+/gm, '  • ')
  
  // 处理标题：# text -> 【text】
  text = text.replace(/^#{1,6}\s+(.+)$/gm, '【$1】')
  
  // 处理多余的空行，但保留必要的段落分隔
  text = text.replace(/\n{3,}/g, '\n\n')
  
  return text
}

module.exports = {
  formatMarkdownText: formatMarkdownText
}