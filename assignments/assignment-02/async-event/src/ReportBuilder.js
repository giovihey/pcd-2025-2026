export class ReportBuilder {
  #totalFiles = 0
  #bands
  #overflow = 0
  #bandWidth

  constructor(MaxFS, NB) {
    this.#bandWidth = MaxFS / NB
    this.#bands = Array.from({ length: NB }, (_, i) => ({
      range: [Math.round(i * this.#bandWidth), Math.round((i + 1) * this.#bandWidth)],
      count: 0,
    }))
  }

  recordFile(filePath, stats) {
    this.#totalFiles++
    const index = Math.floor(stats.size / this.#bandWidth)
    index >= this.#bands.length ? this.#overflow++ : this.#bands[index].count++
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
