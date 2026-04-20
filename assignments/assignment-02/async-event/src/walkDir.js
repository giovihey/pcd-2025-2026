import { readdir, stat } from 'node:fs'
import { join } from 'node:path'

export function walkDir(dir, onFile, onError, onDone) {
  let pending = 0
  function maybeFinish() {
    pending--
    if (pending === 0) onDone()
  }
  function walk(currentDir) {
    pending++
    readdir(currentDir, (err, entries) => {
      if (err) {
        onError(err)
        maybeFinish()
        return
      }
      if (entries.length === 0) {
        maybeFinish()
        return
      }

      entries.forEach((entry) => {
        const fullPath = join(currentDir, entry)
        pending++
        stat(fullPath, (err, stats) => {
          if (err) {
            onError(err)
            maybeFinish()
            return
          }
          stats.isDirectory() ? walk(fullPath) : onFile(fullPath, stats)
          maybeFinish()
        })
      })
      maybeFinish()
    })
  }
  walk(dir)
}
