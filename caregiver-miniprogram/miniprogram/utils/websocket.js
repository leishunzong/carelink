/**
 * WebSocket 工具类（STOMP 协议简化版）
 * 
 * 后端使用 Spring STOMP over SockJS，端点 /ws
 * 护工订阅 /topic/order/{caregiverId} 接收推送订单
 * 
 * 注意：微信小程序不支持 SockJS，这里直接使用 WebSocket + STOMP 帧协议
 */

const STOMP_COMMANDS = {
  CONNECT: 'CONNECT',
  CONNECTED: 'CONNECTED',
  SUBSCRIBE: 'SUBSCRIBE',
  UNSUBSCRIBE: 'UNSUBSCRIBE',
  SEND: 'SEND',
  MESSAGE: 'MESSAGE',
  DISCONNECT: 'DISCONNECT',
  ERROR: 'ERROR',
  HEARTBEAT: '\n'
}

let socketTask = null
let connected = false
let subscriptions = {}
let subscriptionId = 0
let reconnectTimer = null
let heartbeatTimer = null
let reconnectCount = 0
const MAX_RECONNECT = 5
const RECONNECT_INTERVAL = 5000
const HEARTBEAT_INTERVAL = 10000

/**
 * 构建 STOMP 帧
 */
function buildFrame(command, headers, body) {
  let frame = command + '\n'
  if (headers) {
    Object.keys(headers).forEach(key => {
      frame += key + ':' + headers[key] + '\n'
    })
  }
  frame += '\n'
  if (body) {
    frame += body
  }
  frame += '\0'
  return frame
}

/**
 * 解析 STOMP 帧
 */
function parseFrame(data) {
  if (!data || data === '\n') {
    return { command: 'HEARTBEAT' }
  }
  
  const lines = data.split('\n')
  const command = lines[0]
  const headers = {}
  let i = 1
  
  while (i < lines.length && lines[i] !== '') {
    const colonIndex = lines[i].indexOf(':')
    if (colonIndex > 0) {
      headers[lines[i].substring(0, colonIndex)] = lines[i].substring(colonIndex + 1)
    }
    i++
  }
  i++
  
  let body = ''
  while (i < lines.length) {
    body += lines[i]
    if (i < lines.length - 1) {
      body += '\n'
    }
    i++
  }
  body = body.replace(/\0$/, '')
  
  return { command, headers, body }
}

/**
 * 连接 WebSocket
 */
function connect(wsUrl, token, onConnected) {
  if (socketTask) {
    close()
  }
  
  // 微信小程序的 SockJS 回退方案：直接使用 WebSocket
  // Spring SockJS 的 WebSocket URL 格式: /ws/websocket (不带 SockJS 包装)
  const url = wsUrl.replace(/^http/, 'ws') + '/websocket'
  
  console.log('[WebSocket] 正在连接:', url)
  
  socketTask = wx.connectSocket({
    url: url,
    header: {
      'Authorization': 'Bearer ' + token
    },
    success: () => {
      console.log('[WebSocket] connectSocket 调用成功')
    },
    fail: (err) => {
      console.error('[WebSocket] connectSocket 调用失败:', err)
      scheduleReconnect(wsUrl, token, onConnected)
    }
  })
  
  socketTask.onOpen(() => {
    console.log('[WebSocket] 连接已打开，发送 CONNECT 帧')
    reconnectCount = 0
    
    const connectFrame = buildFrame(STOMP_COMMANDS.CONNECT, {
      'accept-version': '1.1,1.0',
      'heart-beat': '10000,10000'
    })
    
    socketTask.send({
      data: connectFrame,
      fail: (err) => {
        console.error('[WebSocket] 发送 CONNECT 帧失败:', err)
      }
    })
  })
  
  socketTask.onMessage((res) => {
    const frame = parseFrame(res.data)
    
    switch (frame.command) {
      case STOMP_COMMANDS.CONNECTED:
        console.log('[WebSocket] STOMP 连接成功')
        connected = true
        startHeartbeat()
        if (onConnected) {
          onConnected()
        }
        break
        
      case STOMP_COMMANDS.MESSAGE:
        const subId = frame.headers['subscription']
        if (subscriptions[subId] && subscriptions[subId].callback) {
          try {
            const body = frame.body ? JSON.parse(frame.body) : null
            subscriptions[subId].callback(body)
          } catch (e) {
            subscriptions[subId].callback(frame.body)
          }
        }
        break
        
      case STOMP_COMMANDS.ERROR:
        console.error('[WebSocket] STOMP 错误:', frame.body)
        break
        
      case 'HEARTBEAT':
        break
        
      default:
        console.log('[WebSocket] 收到未知帧:', frame.command)
    }
  })
  
  socketTask.onClose((res) => {
    console.log('[WebSocket] 连接已关闭:', res)
    connected = false
    stopHeartbeat()
    
    if (reconnectCount < MAX_RECONNECT) {
      scheduleReconnect(wsUrl, token, onConnected)
    }
  })
  
  socketTask.onError((err) => {
    console.error('[WebSocket] 连接错误:', err)
    connected = false
  })
}

/**
 * 订阅主题
 */
function subscribe(destination, callback) {
  const id = 'sub-' + (++subscriptionId)
  
  subscriptions[id] = {
    destination: destination,
    callback: callback
  }
  
  if (connected && socketTask) {
    const frame = buildFrame(STOMP_COMMANDS.SUBSCRIBE, {
      'id': id,
      'destination': destination
    })
    
    socketTask.send({
      data: frame,
      success: () => {
        console.log('[WebSocket] 订阅成功:', destination)
      },
      fail: (err) => {
        console.error('[WebSocket] 订阅失败:', err)
      }
    })
  }
  
  return id
}

/**
 * 取消订阅
 */
function unsubscribe(id) {
  if (connected && socketTask && subscriptions[id]) {
    const frame = buildFrame(STOMP_COMMANDS.UNSUBSCRIBE, {
      'id': id
    })
    
    socketTask.send({
      data: frame,
      fail: (err) => {
        console.error('[WebSocket] 取消订阅失败:', err)
      }
    })
  }
  
  delete subscriptions[id]
}

/**
 * 重新订阅所有主题（重连后）
 */
function resubscribeAll() {
  Object.keys(subscriptions).forEach(id => {
    const sub = subscriptions[id]
    if (connected && socketTask) {
      const frame = buildFrame(STOMP_COMMANDS.SUBSCRIBE, {
        'id': id,
        'destination': sub.destination
      })
      
      socketTask.send({
        data: frame,
        success: () => {
          console.log('[WebSocket] 重新订阅成功:', sub.destination)
        }
      })
    }
  })
}

/**
 * 心跳
 */
function startHeartbeat() {
  stopHeartbeat()
  heartbeatTimer = setInterval(() => {
    if (connected && socketTask) {
      socketTask.send({
        data: '\n',
        fail: () => {}
      })
    }
  }, HEARTBEAT_INTERVAL)
}

function stopHeartbeat() {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

/**
 * 重连调度
 */
function scheduleReconnect(wsUrl, token, onConnected) {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
  }
  
  reconnectCount++
  const delay = RECONNECT_INTERVAL * reconnectCount
  console.log(`[WebSocket] ${delay / 1000}秒后尝试第${reconnectCount}次重连...`)
  
  reconnectTimer = setTimeout(() => {
    connect(wsUrl, token, () => {
      resubscribeAll()
      if (onConnected) {
        onConnected()
      }
    })
  }, delay)
}

/**
 * 关闭连接
 */
function close() {
  reconnectCount = MAX_RECONNECT
  
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  
  stopHeartbeat()
  
  if (connected && socketTask) {
    const frame = buildFrame(STOMP_COMMANDS.DISCONNECT, {})
    socketTask.send({
      data: frame,
      complete: () => {
        socketTask.close({})
        socketTask = null
        connected = false
      }
    })
  } else if (socketTask) {
    socketTask.close({})
    socketTask = null
  }
  
  connected = false
  subscriptions = {}
  subscriptionId = 0
  reconnectCount = 0
}

/**
 * 是否已连接
 */
function isConnected() {
  return connected
}

module.exports = {
  connect,
  subscribe,
  unsubscribe,
  close,
  isConnected
}
