const btnStart = document.getElementById('btn-start')
const btnStop = document.getElementById('btn-stop')
const statusEl = document.getElementById('status')
const reportEl = document.getElementById('report')
const chartEl = document.getElementById('chart')
const valTotal = document.getElementById('val-total')
const valOverflow = document.getElementById('val-overflow')

let evtSource = null

// ── helpers ──────────────────────────────────────────────────────────────

function setStatus(msg, cls = '') {
  statusEl.className = cls
  if (cls === 'running') {
    statusEl.innerHTML = `<div class="pulse"></div>${msg}`
  } else {
    statusEl.textContent = msg
  }
}

function animateValue(el) {
  el.classList.remove('updated')
  void el.offsetWidth // reflow to restart animation
  el.classList.add('updated')
}

function formatBytes(n) {
  if (n >= 1_073_741_824) return (n / 1_073_741_824).toFixed(1) + ' GB'
  if (n >= 1_048_576) return (n / 1_048_576).toFixed(1) + ' MB'
  if (n >= 1_024) return (n / 1_024).toFixed(1) + ' KB'
  return n + ' B'
}

function buildChart(bands, nb, maxFS) {
  const maxCount = Math.max(1, ...bands)
  const bandSize = maxFS / nb
  chartEl.innerHTML = ''

  bands.forEach((count, i) => {
    const isOverflow = i === nb
    const pct = ((count / maxCount) * 100).toFixed(1)
    const label = isOverflow
      ? `> ${formatBytes(maxFS)}`
      : `${formatBytes(Math.round(i * bandSize))} – ${formatBytes(Math.round((i + 1) * bandSize))}`

    const row = document.createElement('div')
    row.className = 'bar-row'
    row.innerHTML = `
      <div class="bar-label">${label}</div>
      <div class="bar-track">
        <div class="bar-fill ${isOverflow ? 'overflow' : 'regular'}" style="width:${pct}%"></div>
      </div>
      <div class="bar-count">${count.toLocaleString()}</div>
    `
    chartEl.appendChild(row)
  })
}

// ── SSE report update ─────────────────────────────────────────────────────

function applyReport(data) {
  reportEl.classList.add('visible')

  const prev = parseInt(valTotal.textContent.replace(/,/g, ''), 10) || 0
  const curr = data.totalFiles
  valTotal.textContent = curr.toLocaleString()
  if (curr !== prev) animateValue(valTotal)

  const nb = data.bands.length - 1
  const maxFS = parseInt(document.getElementById('in-maxfs').value, 10)
  valOverflow.textContent = (data.bands[nb] ?? 0).toLocaleString()

  buildChart(data.bands, nb, maxFS)
}

// ── Start ─────────────────────────────────────────────────────────────────

function startScan() {
  const dir = document.getElementById('in-dir').value.trim() || 'C:/Users/giovi/Documents'
  const maxFS = parseInt(document.getElementById('in-maxfs').value, 10) || 1_000_000
  const nb = parseInt(document.getElementById('in-nb').value, 10) || 5

  if (evtSource) {
    evtSource.close()
    evtSource = null
  }

  btnStart.disabled = true
  btnStop.classList.add('active')
  setStatus(`Scanning ${dir} …`, 'running')
  reportEl.classList.remove('visible')

  const url = `/report?dir=${encodeURIComponent(dir)}&maxFS=${maxFS}&nb=${nb}`
  evtSource = new EventSource(url)

  evtSource.addEventListener('update', (e) => applyReport(JSON.parse(e.data)))
  evtSource.addEventListener('complete', () => {
    setStatus('Scan complete.', 'done')
    cleanupStream()
  })
  evtSource.addEventListener('error', (e) => {
    try {
      setStatus(`Error: ${JSON.parse(e.data).message}`, 'error')
    } catch {
      setStatus('Stream error.', 'error')
    }
    cleanupStream()
  })
  evtSource.onerror = () => cleanupStream()
}

// ── Stop ──────────────────────────────────────────────────────────────────

async function stopScan() {
  if (evtSource) {
    evtSource.close()
    evtSource = null
  }
  await fetch('/stop', { method: 'POST' }).catch(() => {})
  setStatus('Stopped.', 'error')
  cleanupStream()
}

function cleanupStream() {
  if (evtSource) {
    evtSource.close()
    evtSource = null
  }
  btnStart.disabled = false
  btnStop.classList.remove('active')
}

btnStart.addEventListener('click', startScan)
btnStop.addEventListener('click', stopScan)
