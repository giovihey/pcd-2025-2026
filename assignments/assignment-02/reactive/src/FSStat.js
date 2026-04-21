import { walkDir } from './walkDir.js'
import { scan } from 'rxjs'

export function getFSReport(dir, maxFS, nb) {
  const bandSize = maxFS / nb

  return walkDir(dir).pipe(
    scan(
      (report, { fullPath, stats }) => {
        const band = stats.size > maxFS ? nb : Math.floor(stats.size / bandSize)

        return {
          totalFiles: report.totalFiles + 1,
          latestFile: { path: fullPath, size: stats.size },
          bands: report.bands.map((count, i) => (i === band ? count + 1 : count)),
        }
      },
      { totalFiles: 0, latestFile: null, bands: new Array(nb + 1).fill(0) },
    ),
  )
}
