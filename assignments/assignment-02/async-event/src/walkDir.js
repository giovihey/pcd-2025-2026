import { readdir, stat } from 'node:fs/promises'
import { join } from 'node:path'

export async function walkDir(dir, onFile, onError) {
  let entries
  try {
    entries = await readdir(dir)
  } catch (err) {
    onError(err)
    return
  }

  await Promise.all(
    entries.map(async (entry) => {
      const fullPath = join(dir, entry)
      let stats
      try {
        stats = await stat(fullPath)
      } catch (err) {
        onError(err)
        return
      }
      if (stats.isDirectory()) {
        await walkDir(fullPath, onFile, onError)
      } else {
        onFile(fullPath, stats)
      }
    }),
  )
}
