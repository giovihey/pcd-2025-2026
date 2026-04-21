import { walkDir } from './walkDir.js'
import { ReportBuilder } from './ReportBuilder.js'

export async function getFSReport(D, maxFs, NB) {
  const builder = new ReportBuilder(maxFs, NB)
  const errors = []

  await walkDir(
    D,
    (filePath, stats) => builder.recordFile(filePath, stats),
    (err) => errors.push(err),
  )

  return { ...builder.buildReport(), errors }
}
