import { EventEmitter } from 'node:events'

export class ReportEmitter extends EventEmitter {
  #totalFiles = 0
  #bands
  #overflow = 0
  #bandWidth

  constructor(MaxFS, NB) {
    super()
    this.#bandWidth = MaxFS / NB
    this.#bands = Array.from({ length: NB }, (_, i) => ({
      range: [Math.round(i * this.#bandWidth), Math.round((i + 1) * this.#bandWidth)],
      count: 0,
    }))
  }

  recordFile(filePath, stats) {
    this.#totalFiles++

    const index = Math.floor(stats.size / this.#bandWidth)

    if (index >= this.#bands.length) {
      this.#overflow++
    } else {
      this.#bands[index].count++
    }
    this.emit('entry', { path: filePath, size: stats.size })
  }

  buildReport() {
    return {
      totalFiles: this.#totalFiles,
      bands: this.#bands.map((b) => ({ ...b })),
      overflow: {
        range: [Math.round(this.#bands.length * this.#bandWidth), Infinity],
        count: this.#overflow,
      },
    }
  }
}
