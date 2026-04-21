import { getFSReport } from './src/FSStat.js'

try {
  const report = await getFSReport('C:/Users/giovi/Documents', 1_000_000, 5)
  console.log('Total files:', report.totalFiles)
  console.table(report.bands)
  console.log('Overflow (> MaxFS):', report.overflow)
  if (report.errors.length)
    console.warn(
      'Skipped paths:',
      report.errors.map((e) => e.path),
    )
} catch (err) {
  console.error('Fatal error:', err)
}
