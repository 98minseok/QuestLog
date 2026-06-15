import { config } from '@vue/test-utils'

config.global.stubs = {
  VAlert: { template: '<div><slot /></div>' },
  VApp: { template: '<div><slot /></div>' },
  VMain: { template: '<main><slot /></main>' },
  VProgressLinear: { template: '<div />' },
  VSkeletonLoader: { template: '<div />' },
}
