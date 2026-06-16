import axios, {
  AxiosHeaders,
  type InternalAxiosRequestConfig,
} from 'axios'
import Keycloak, { type KeycloakConfig } from 'keycloak-js'
import { reactive } from 'vue'

const AUTH_SESSION_KEY = 'questlog.auth.session'
const authMode = import.meta.env.VITE_AUTH_MODE === 'keycloak' ? 'keycloak' : 'dev'

type StoredSession = {
  token?: string
  refreshToken?: string
  idToken?: string
}

export const authState = reactive({
  mode: authMode,
  authenticated: authMode === 'dev',
  username: authMode === 'dev' ? 'dev-user' : '',
  error: '',
})

let keycloak: Keycloak | null = null

function keycloakConfig(): KeycloakConfig {
  return {
    url: import.meta.env.VITE_KEYCLOAK_URL ?? 'http://localhost:18080',
    realm: import.meta.env.VITE_KEYCLOAK_REALM ?? 'questlog',
    clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'questlog-frontend',
  }
}

function readStoredSession(): StoredSession {
  const value = sessionStorage.getItem(AUTH_SESSION_KEY)
  if (!value) return {}

  try {
    return JSON.parse(value) as StoredSession
  } catch {
    sessionStorage.removeItem(AUTH_SESSION_KEY)
    return {}
  }
}

function persistSession() {
  if (!keycloak?.authenticated || !keycloak.token) {
    sessionStorage.removeItem(AUTH_SESSION_KEY)
    return
  }

  sessionStorage.setItem(
    AUTH_SESSION_KEY,
    JSON.stringify({
      token: keycloak.token,
      refreshToken: keycloak.refreshToken,
      idToken: keycloak.idToken,
    } satisfies StoredSession),
  )
}

function syncAuthState() {
  authState.authenticated = keycloak?.authenticated === true
  authState.username =
    (keycloak?.tokenParsed?.preferred_username as string | undefined) ??
    (keycloak?.tokenParsed?.name as string | undefined) ??
    ''
  persistSession()
}

export async function initializeAuth() {
  if (authState.mode === 'dev') return

  keycloak = new Keycloak(keycloakConfig())
  const stored = readStoredSession()
  keycloak.onAuthSuccess = syncAuthState
  keycloak.onAuthRefreshSuccess = syncAuthState
  keycloak.onAuthLogout = syncAuthState
  keycloak.onTokenExpired = () => {
    void keycloak?.updateToken(0).then(syncAuthState).catch(() => {
      authState.authenticated = false
      persistSession()
    })
  }

  try {
    await keycloak.init({
      onLoad: 'check-sso',
      pkceMethod: 'S256',
      checkLoginIframe: false,
      token: stored.token,
      refreshToken: stored.refreshToken,
      idToken: stored.idToken,
    })
    syncAuthState()
  } catch {
    authState.error = 'Keycloak is unavailable. Start local infrastructure and try again.'
    authState.authenticated = false
  }
}

export async function login() {
  await keycloak?.login({ redirectUri: window.location.href })
}

export async function logout() {
  sessionStorage.removeItem(AUTH_SESSION_KEY)
  await keycloak?.logout({ redirectUri: window.location.origin })
}

async function accessToken() {
  if (authState.mode === 'dev' || !keycloak?.authenticated) return undefined

  await keycloak.updateToken(30)
  syncAuthState()
  return keycloak.token
}

export function attachBearerToken(
  config: InternalAxiosRequestConfig,
  token?: string,
) {
  const headers = AxiosHeaders.from(config.headers)
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  } else {
    headers.delete('Authorization')
  }
  config.headers = headers
  return config
}

export function installAuthInterceptor(
  tokenProvider: () => Promise<string | undefined> = accessToken,
) {
  return axios.interceptors.request.use(async (config) =>
    attachBearerToken(config, await tokenProvider()),
  )
}
