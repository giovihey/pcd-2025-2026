import { getFSReport } from './src/FSStat.js'

getFSReport('C:/Users/giovi/Documents', 1_000_000, 5).subscribe({
  next: (report) => console.log('Final report:', report),
  error: (err) => console.error(err),
  complete: () => console.log('Done'),
})
