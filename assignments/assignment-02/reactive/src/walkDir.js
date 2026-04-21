import { from, merge } from 'rxjs'
import { mergeMap } from 'rxjs/operators'
import { readdir, lstat } from 'node:fs/promises'
import { join } from 'node:path'

export function walkDir(dir) {
  return from(readdir(dir)).pipe(
    mergeMap((entries) =>
      merge(
        ...entries.map((entry) => {
          const fullPath = join(dir, entry)
          return from(lstat(fullPath)).pipe(
            mergeMap((stats) => {
              if (stats.isSymbolicLink()) return []
              if (stats.isDirectory()) return walkDir(fullPath)
              return [{ fullPath, stats }]
            }),
          )
        }),
      ),
    ),
  )
}
