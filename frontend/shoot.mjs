import { chromium } from 'playwright'

const OUT = 'shots'
const base = 'http://localhost:5173'

const browser = await chromium.launch()
const page = await browser.newPage({ viewport: { width: 1440, height: 900 }, deviceScaleFactor: 2 })

async function shot(name) {
  await page.waitForTimeout(900)
  await page.screenshot({ path: `${OUT}/${name}.png` })
  console.log('shot:', name)
}

// 1) Login
await page.goto(`${base}/login`, { waitUntil: 'networkidle' })
await shot('01-login')

// đăng nhập admin
await page.fill('input', 'admin') // username (first input)
await page.fill('input[type=password]', '123456')
await page.click('button[type=submit]')
await page.waitForURL('**/dashboard', { timeout: 15000 })
await shot('02-dashboard')

const pages = [
  ['products', '03-products'],
  ['inventory', '04-inventory'],
  ['pos', '05-pos'],
  ['invoices', '06-invoices'],
  ['reports', '07-reports'],
  ['promotions', '08-promotions'],
  ['settings', '09-settings'],
]
for (const [route, name] of pages) {
  await page.goto(`${base}/${route}`, { waitUntil: 'networkidle' })
  await shot(name)
}

await browser.close()
console.log('DONE')
