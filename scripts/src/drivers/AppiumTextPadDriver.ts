import fs from 'fs'
import path from 'path'
import { remote, type Browser } from 'webdriverio'
import type { Selector } from '../pages/Selectors'
import { ensureArtifactDir } from '../support/config'
import type { AndroidConfig } from '../support/config'
import { logger } from '../support/logger'
import type { Driver } from './Driver'

export class AppiumTextPadDriver implements Driver {
  private client?: Browser

  constructor(
    private readonly serverUrl: string,
    private readonly androidConfig: AndroidConfig,
    private readonly artifactDir: string,
  ) {}

  async init(): Promise<void> {
    const endpoint = new URL(this.serverUrl)
    logger.info(
      `Starting Appium session for ${this.androidConfig.name} (${this.androidConfig.appPackage})`,
    )
    this.client = await remote({
      protocol: endpoint.protocol.replace(':', ''),
      hostname: endpoint.hostname,
      port: Number(endpoint.port || 4723),
      path: endpoint.pathname,
      logLevel: 'info',
      connectionRetryTimeout: 120000,
      connectionRetryCount: 1,
      capabilities: {
        platformName: 'Android',
        'appium:automationName': this.androidConfig.automationName,
        'appium:deviceName': this.androidConfig.name,
        'appium:udid': this.androidConfig.udid,
        'appium:platformVersion': this.androidConfig.platformVersion,
        'appium:appPackage': this.androidConfig.appPackage,
        'appium:appActivity': this.androidConfig.appActivity,
        'appium:appWaitActivity': '*',
        'appium:noReset': true,
        'appium:newCommandTimeout': 180,
        'appium:autoGrantPermissions': true,
      },
    })

    await this.restartApp()
  }

  async dispose(): Promise<void> {
    await this.client?.deleteSession()
  }

  async restartApp(): Promise<void> {
    if (!this.client) {
      throw new Error('Appium session is not initialized')
    }

    await this.stopApp()
    await sleep(1000)
    await this.activateApp()
  }

  async stopApp(): Promise<void> {
    await this.forceStopApp()
  }

  async click(selector: Selector, timeoutMs?: number): Promise<void> {
    const element = await this.findElement(selector, timeoutMs)
    await element.click()
  }

  async fill(selector: Selector, value: string): Promise<void> {
    const element = await this.findElement(selector)
    await element.clearValue()
    await element.setValue(value)
  }

  async text(selector: Selector, timeoutMs?: number): Promise<string> {
    const element = await this.findElement(selector, timeoutMs)
    return (await element.getText()).trim()
  }

  async waitForVisible(selector: Selector, timeoutMs = 10000): Promise<void> {
    const element = await this.findElement(selector, timeoutMs)
    await element.waitForDisplayed({ timeout: timeoutMs })
  }

  async isVisible(selector: Selector, timeoutMs = 1000): Promise<boolean> {
    try {
      await this.waitForVisible(selector, timeoutMs)
      return true
    } catch {
      return false
    }
  }

  async screenshot(name: string): Promise<string> {
    const dir = ensureArtifactDir(this.artifactDir)
    const file = path.join(dir, `${name}.png`)
    await this.client?.saveScreenshot(file)
    return file
  }

  async dumpDebug(name: string): Promise<void> {
    const dir = ensureArtifactDir(this.artifactDir)
    const source = (await this.client?.getPageSource()) || ''
    fs.writeFileSync(path.join(dir, `${name}.xml`), source, 'utf8')
  }

  async readFile(remotePath: string): Promise<string> {
    if (!this.client) {
      throw new Error('Appium session is not initialized')
    }

    const content = await (
      this.client as Browser & { pullFile(path: string): Promise<string> }
    ).pullFile(remotePath)
    return Buffer.from(content, 'base64').toString('utf8')
  }

  async removeFile(remotePath: string): Promise<void> {
    if (!this.client) {
      throw new Error('Appium session is not initialized')
    }

    try {
      await this.client.execute('mobile: shell', {
        command: 'rm',
        args: ['-f', remotePath],
      })
    } catch (error) {
      logger.info(
        `Could not remove ${remotePath}; continuing because cleanup is best-effort. ${String(error)}`,
      )
    }
  }

  protected async findElement(selector: Selector, timeoutMs = 10000) {
    if (!this.client) {
      throw new Error('Appium session is not initialized')
    }

    const query = selectorToQuery(selector)
    const element = await this.client.$(query)
    await element.waitForExist({ timeout: timeoutMs })
    return element
  }

  private async activateApp(): Promise<void> {
    if (!this.client) {
      throw new Error('Appium session is not initialized')
    }

    await (
      this.client as Browser & {
        activateApp(appId: string): Promise<void>
      }
    ).activateApp(this.androidConfig.appPackage)
  }

  private async forceStopApp(): Promise<void> {
    if (!this.client) {
      throw new Error('Appium session is not initialized')
    }

    try {
      await this.client.execute('mobile: shell', {
        command: 'am',
        args: ['force-stop', this.androidConfig.appPackage],
      })
      return
    } catch (error) {
      logger.info(
        `Could not force-stop ${this.androidConfig.appPackage} through mobile shell; falling back to terminateApp. ${String(error)}`,
      )
    }

    try {
      await (
        this.client as Browser & {
          terminateApp(appId: string): Promise<boolean>
        }
      ).terminateApp(this.androidConfig.appPackage)
    } catch (error) {
      logger.info(
        `Could not confirm ${this.androidConfig.appPackage} termination; continuing with activateApp. ${String(error)}`,
      )
    }
  }
}

function sleep(durationMs: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, durationMs))
}

function selectorToQuery(selector: Selector): string {
  switch (selector.kind) {
    case 'accessibilityId':
      return `~${selector.value}`
    case 'className':
      return `class name:${selector.value}`
    case 'id':
      return `id:${selector.value}`
    case 'xpath':
      return selector.value
    case 'text':
      return `//*[@text=${escapeXPathLiteral(selector.value)} or @content-desc=${escapeXPathLiteral(selector.value)}]`
    default:
      throw new Error(`Unsupported selector: ${JSON.stringify(selector)}`)
  }
}

function escapeXPathLiteral(value: string): string {
  if (!value.includes('"')) {
    return `"${value}"`
  }
  if (!value.includes("'")) {
    return `'${value}'`
  }

  const parts = value.split('"')
  return `concat(${parts
    .map((part, index) => {
      const literal = `"${part}"`
      return index < parts.length - 1 ? `${literal}, '"', ` : literal
    })
    .join('')})`
}
