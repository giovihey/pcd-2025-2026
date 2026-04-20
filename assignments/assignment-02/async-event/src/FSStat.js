import { walkDir } from './walkDir.js'
import { ReportEmitter } from './ReportEmitter.js'

/**
 * @param {string} D
 * @param {number} maxFs
 * @param {number} NB
 * @returns {ReportEmitter}
 */
export function getFSReport(D, maxFs, NB) {
  const emitter = new ReportEmitter(maxFs, NB)

  walkDir(
    D,
    (filePath, stats) => emitter.recordFile(filePath, stats),
    (err) => emitter.emit('error', err),
    () => emitter.emit('done', emitter.buildReport()),
  )

  return emitter
}
