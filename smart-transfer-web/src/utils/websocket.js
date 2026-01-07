/**
 * WebSocket 监控服务
 */
class MonitorWebSocket {
  constructor() {
    this.ws = null
    this.url = null
    this.reconnectTimer = null
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
    this.reconnectDelay = 3000
    this.listeners = new Set()
    this.isConnecting = false
  }

  /**
   * 连接WebSocket
   */
  connect() {
    if (this.ws?.readyState === WebSocket.OPEN || this.isConnecting) {
      return
    }

    this.isConnecting = true
    
    // 构建WebSocket URL
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = import.meta.env.VITE_WS_HOST || window.location.hostname
    const port = import.meta.env.VITE_WS_PORT || '8081'
    this.url = `${protocol}//${host}:${port}/ws/monitor`

    try {
      this.ws = new WebSocket(this.url)

      this.ws.onopen = () => {
        this.isConnecting = false
        this.reconnectAttempts = 0
        this.notifyListeners({ type: 'connected' })
      }

      this.ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          this.notifyListeners({ type: 'message', data })
        } catch {
          // 解析消息失败，静默处理
        }
      }

      this.ws.onclose = () => {
        this.isConnecting = false
        this.notifyListeners({ type: 'disconnected' })
        this.scheduleReconnect()
      }

      this.ws.onerror = (error) => {
        this.isConnecting = false
        this.notifyListeners({ type: 'error', error })
      }
    } catch {
      this.isConnecting = false
      this.scheduleReconnect()
    }
  }

  /**
   * 断开连接
   */
  disconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    
    this.reconnectAttempts = 0
  }

  /**
   * 发送消息
   */
  send(message) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(typeof message === 'string' ? message : JSON.stringify(message))
    }
  }

  /**
   * 手动刷新数据
   */
  refresh() {
    this.send('refresh')
  }

  /**
   * 安排重连
   */
  scheduleReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      return
    }

    this.reconnectAttempts++
    const delay = this.reconnectDelay * this.reconnectAttempts

    this.reconnectTimer = setTimeout(() => {
      this.connect()
    }, delay)
  }

  /**
   * 添加监听器
   */
  addListener(callback) {
    this.listeners.add(callback)
    return () => this.listeners.delete(callback)
  }

  /**
   * 通知所有监听器
   */
  notifyListeners(event) {
    this.listeners.forEach(callback => {
      try {
        callback(event)
      } catch {
        // 监听器回调错误，静默处理
      }
    })
  }

  /**
   * 获取连接状态
   */
  get isConnected() {
    return this.ws?.readyState === WebSocket.OPEN
  }
}

// 单例导出
export const monitorWs = new MonitorWebSocket()
