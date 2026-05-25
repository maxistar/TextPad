import type { Selector } from '../pages/Selectors'

export interface Driver {
  init(): Promise<void>
  dispose(): Promise<void>
  click(selector: Selector, timeoutMs?: number): Promise<void>
  fill(selector: Selector, value: string): Promise<void>
  text(selector: Selector, timeoutMs?: number): Promise<string>
  waitForVisible(selector: Selector, timeoutMs?: number): Promise<void>
  isVisible(selector: Selector, timeoutMs?: number): Promise<boolean>
  screenshot(name: string): Promise<string>
  dumpDebug(name: string): Promise<void>
  readFile(remotePath: string): Promise<string>
  removeFile(remotePath: string): Promise<void>
}

export type { Selector }
