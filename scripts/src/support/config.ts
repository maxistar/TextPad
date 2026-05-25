import fs from 'fs'
import path from 'path'

loadDotEnv()

export interface AndroidConfig {
  name: string
  udid?: string
  platformVersion?: string
  appPackage: string
  appActivity: string
  automationName: string
}

export interface TestConfig {
  serverUrl: string
  waitTimeoutMs: number
  android: AndroidConfig
  testFileDirectory: string
  testFilePrefix: string
}

export function loadTestConfig(): TestConfig {
  return {
    serverUrl: process.env.APPIUM_SERVER_URL || 'http://127.0.0.1:4723',
    waitTimeoutMs: parseNumber(process.env.APPIUM_WAIT_TIMEOUT_MS, 20000),
    android: {
      name: process.env.ANDROID_DEVICE_NAME || 'Android',
      udid: process.env.ANDROID_UDID,
      platformVersion: process.env.ANDROID_PLATFORM_VERSION,
      appPackage: process.env.TEXTPAD_APP_PACKAGE || 'com.maxistar.textpad',
      appActivity:
        process.env.TEXTPAD_APP_ACTIVITY ||
        'com.maxistar.textpad.activities.EditorActivity',
      automationName: process.env.ANDROID_AUTOMATION_NAME || 'UiAutomator2',
    },
    testFileDirectory:
      process.env.TEXTPAD_E2E_TEST_FILE_DIR || '/sdcard/Download',
    testFilePrefix: process.env.TEXTPAD_E2E_TEST_FILE_PREFIX || 'textpad-e2e-save',
  }
}

export function artifactPath(scope: string): string {
  const stamp = new Date().toISOString().replace(/[:.]/g, '-')
  return path.join(process.cwd(), 'artifacts', scope, stamp)
}

export function ensureArtifactDir(base: string): string {
  if (!fs.existsSync(base)) {
    fs.mkdirSync(base, { recursive: true })
  }
  return base
}

export function makeTestFileName(prefix: string): string {
  const stamp = new Date().toISOString().replace(/[^0-9]/g, '').slice(0, 14)
  return `${prefix}-${stamp}.txt`
}

export function remotePath(directory: string, fileName: string): string {
  return `${directory.replace(/\/+$/, '')}/${fileName}`
}

function parseNumber(rawValue: string | undefined, fallback: number): number {
  const parsed = Number(rawValue)
  return Number.isFinite(parsed) ? parsed : fallback
}

function loadDotEnv(): void {
  const candidates = [
    path.resolve(process.cwd(), '.env'),
    path.resolve(__dirname, '../../.env'),
  ]
  const envPath = candidates.find((candidate) => fs.existsSync(candidate))
  if (!envPath) return

  const content = fs.readFileSync(envPath, 'utf8')
  const lines = content.split(/\r?\n/)

  for (const rawLine of lines) {
    const line = rawLine.trim()
    if (!line || line.startsWith('#')) continue

    const match = line.match(
      /^(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$/,
    )
    if (!match) continue

    const key = match[1]
    let value = match[2]

    if (
      (value.startsWith('"') && value.endsWith('"')) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1)
    }

    if (process.env[key] === undefined) {
      process.env[key] = value
    }
  }
}
