import type { IWorld } from '@cucumber/cucumber'
import type { CustomWorld } from './CustomWorld'

export function asCustomWorld(world: IWorld): CustomWorld {
  return world as CustomWorld
}
