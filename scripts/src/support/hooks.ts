import { After, Before, Status, setDefaultTimeout } from '@cucumber/cucumber'
import { CustomWorld } from '../world/CustomWorld'

setDefaultTimeout(180000)

Before(async function (this: CustomWorld) {
  await this.initSession()
  await this.resetTestFile()
})

After(async function (this: CustomWorld, { result, pickle }) {
  if (result?.status === Status.FAILED) {
    const scenarioName = pickle.name.replace(/\s+/g, '_')
    await this.captureDiagnostics(`${scenarioName}_failure`)
  }

  await this.disposeSession()
})
