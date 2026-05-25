import { IWorldOptions, World, setWorldConstructor } from '@cucumber/cucumber'
import { AppiumTextPadDriver } from '../drivers/AppiumTextPadDriver'
import {
  artifactPath,
  loadTestConfig,
  makeTestFileName,
  remotePath,
  type TestConfig,
} from '../support/config'
import { logger } from '../support/logger'

export class CustomWorld extends World {
  public readonly config: TestConfig
  public readonly artifactDir: string
  public readonly app: AppiumTextPadDriver
  public testFileName: string
  public testFilePath: string

  constructor(options: IWorldOptions) {
    super(options)
    this.config = loadTestConfig()
    this.artifactDir = artifactPath('save-flow')
    this.app = new AppiumTextPadDriver(
      this.config.serverUrl,
      this.config.android,
      this.artifactDir,
    )
    this.testFileName = makeTestFileName(this.config.testFilePrefix)
    this.testFilePath = remotePath(
      this.config.testFileDirectory,
      this.testFileName,
    )
  }

  async initSession(): Promise<void> {
    logger.info('Initializing TextPad session')
    await this.app.init()
  }

  async prepareApp(): Promise<void> {
    logger.info('Restarting TextPad before scenario')
    await this.app.restartApp()
  }

  async disposeSession(): Promise<void> {
    await this.app.dispose()
  }

  async resetTestFile(): Promise<void> {
    await this.app.removeFile(this.testFilePath)
  }

  async stopApp(): Promise<void> {
    logger.info('Stopping TextPad after scenario')
    await this.app.stopApp()
  }

  async captureDiagnostics(baseName: string): Promise<void> {
    await this.app.screenshot(baseName)
    await this.app.dumpDebug(baseName)
  }
}

setWorldConstructor(CustomWorld)
