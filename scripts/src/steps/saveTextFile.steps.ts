import { Given, Then, When } from '@cucumber/cucumber'
import { selectors } from '../pages/Selectors'
import { CustomWorld } from '../world/CustomWorld'

Given(
  'the TextPad app is open and ready for editing',
  async function (this: CustomWorld) {
    await this.app.waitForVisible(
      selectors.editorInput,
      this.config.waitTimeoutMs,
    )
  },
)

When(
  'I enter the text {string}',
  async function (this: CustomWorld, text: string) {
    await this.app.fill(selectors.editorInput, text)
  },
)

When(
  'I save the text as a generated test file',
  async function (this: CustomWorld) {
    await this.app.click(selectors.overflowMenu, this.config.waitTimeoutMs)
    await this.app.click(selectors.saveAction, this.config.waitTimeoutMs)

    if (
      await this.app.isVisible(
        selectors.androidFileNameInput,
        this.config.waitTimeoutMs,
      )
    ) {
      await this.app.fill(selectors.androidFileNameInput, this.testFileName)
      await this.app.click(
        selectors.androidConfirmButton,
        this.config.waitTimeoutMs,
      )
      return
    }

    await this.app.fill(selectors.legacyFileNameInput, this.testFileName)
    await this.app.click(selectors.legacyCreateButton, this.config.waitTimeoutMs)
  },
)

Then('the editor remains ready after saving', async function (this: CustomWorld) {
  await this.app.waitForVisible(selectors.editorInput, this.config.waitTimeoutMs)
})

Then(
  'the saved test file contains {string}',
  async function (this: CustomWorld, expected: string) {
    const actual = await waitForFileContent(this, this.testFilePath)
    if (actual !== expected) {
      throw new Error(
        `Expected ${this.testFilePath} to contain ${JSON.stringify(
          expected,
        )}, but found ${JSON.stringify(actual)}`,
      )
    }
  },
)

async function waitForFileContent(
  world: CustomWorld,
  remotePath: string,
): Promise<string> {
  const deadline = Date.now() + world.config.waitTimeoutMs
  let lastError: unknown

  while (Date.now() < deadline) {
    try {
      return await world.app.readFile(remotePath)
    } catch (error) {
      lastError = error
      await sleep(500)
    }
  }

  throw new Error(
    `Could not read saved file ${remotePath}. Configure TEXTPAD_E2E_TEST_FILE_DIR if the Android document picker saved the file elsewhere. Last error: ${String(
      lastError,
    )}`,
  )
}

function sleep(durationMs: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, durationMs))
}
