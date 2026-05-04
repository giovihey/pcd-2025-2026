import { createServer } from 'node:http'
import { readFileSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { getFSReport } from './src/FSStat.js'
import { throttleTime } from 'rxjs'

const __dirname = dirname(fileURLToPath(import.meta.url))
const PORT = 3000

// Only one scan at a time — holds the active SSE response so /stop can close it
let activeResponse = null
let activeSubscription = null

function stopActive() {
  if (activeSubscription) {
    activeSubscription.unsubscribe()
    activeSubscription = null
  }
  if (activeResponse && !activeResponse.writableEnded) {
    activeResponse.end()
    activeResponse = null
  }
}

const server = createServer((req, res) => {
  const url = new URL(req.url, `http://localhost:${PORT}`)

  const staticMap = {
    '/': { file: 'gui.html', mime: 'text/html; charset=utf-8' },
    '/gui.html': { file: 'gui.html', mime: 'text/html; charset=utf-8' },
    '/style.css': { file: 'style.css', mime: 'text/css; charset=utf-8' },
    '/app.js': { file: 'app.js', mime: 'text/javascript; charset=utf-8' },
  }
  if (staticMap[url.pathname]) {
    const { file, mime } = staticMap[url.pathname]
    try {
      const content = readFileSync(join(__dirname, 'public', file))
      res.writeHead(200, { 'Content-Type': mime })
      res.end(content)
    } catch {
      res.writeHead(500)
      res.end(`Could not load public/${file}`)
    }
    return
  }

  if (url.pathname === '/report' && req.method === 'GET') {
    const dir = url.searchParams.get('dir') ?? 'C:/Users/giovi/Documents'
    const maxFS = Math.max(1, parseInt(url.searchParams.get('maxFS') ?? '1000000', 10))
    const nb = Math.max(1, parseInt(url.searchParams.get('nb') ?? '5', 10))

    // Kill any previous scan
    stopActive()

    res.writeHead(200, {
      'Content-Type': 'text/event-stream',
      'Cache-Control': 'no-cache',
      Connection: 'keep-alive',
      'Access-Control-Allow-Origin': '*',
    })
    // Keep Nginx / proxies alive
    res.write(': ping\n\n')

    activeResponse = res

    const send = (event, data) => res.write(`event: ${event}\ndata: ${JSON.stringify(data)}\n\n`)

    send('started', { dir, maxFS, nb })

    activeSubscription = getFSReport(dir, maxFS, nb)
      .pipe(
        // Emit at most once every 100 ms; always emit the first and the last value
        // so the UI feels instant at the start and always shows the final totals.
        throttleTime(100, undefined, { leading: true, trailing: true }),
      )
      .subscribe({
        next: (report) => send('update', report),
        error: (err) => {
          send('error', { message: err.message })
          stopActive()
        },
        complete: () => {
          send('complete', {})
          stopActive()
        },
      })

    req.on('close', () => {
      if (activeSubscription) {
        activeSubscription.unsubscribe()
        activeSubscription = null
        activeResponse = null
      }
    })

    return
  }

  if (url.pathname === '/stop' && req.method === 'POST') {
    stopActive()
    res.writeHead(200, { 'Content-Type': 'application/json' })
    res.end(JSON.stringify({ ok: true }))
    return
  }

  res.writeHead(404)
  res.end('Not found')
})

server.listen(PORT, () => {
  console.log(`\nFSStat GUI  →  http://localhost:${PORT}\n`)
})
