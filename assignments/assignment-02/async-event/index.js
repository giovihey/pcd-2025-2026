import { getFSReport } from './src/FSStat.js'

const report = getFSReport('C:/Users/giovi/Documents', 1_000_000, 5)

report.on('entry', ({ path, size }) => console.log(path, size))
report.on('error', (err) => console.error(err))
report.on('done', (R) => {
  console.log('Total files:', R.totalFiles)
  console.table(R.bands)
  console.log('Overflow (> MaxFS):', R.overflow)
})
