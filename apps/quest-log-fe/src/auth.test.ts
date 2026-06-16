import { AxiosHeaders, type InternalAxiosRequestConfig } from 'axios'
import { describe, expect, it, vi } from 'vitest'
import { attachBearerToken, installAuthInterceptor } from './auth'

function requestConfig(): InternalAxiosRequestConfig {
  return {
    headers: new AxiosHeaders(),
  } as InternalAxiosRequestConfig
}

describe('frontend authentication', () => {
  it('attaches the current access token to BFF requests', () => {
    const config = attachBearerToken(requestConfig(), 'access-token')

    expect(config.headers.get('Authorization')).toBe('Bearer access-token')
  })

  it('removes stale authorization in development fallback mode', () => {
    const config = requestConfig()
    config.headers.set('Authorization', 'Bearer stale-token')

    attachBearerToken(config)

    expect(config.headers.has('Authorization')).toBe(false)
  })

  it('registers an async axios request interceptor', async () => {
    const interceptorId = installAuthInterceptor(
      vi.fn().mockResolvedValue('forwarded-token'),
    )

    expect(interceptorId).toBeTypeOf('number')
  })
})
