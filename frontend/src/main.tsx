import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { GoogleOAuthProvider } from '@react-oauth/google'
import { AuthProvider } from './AuthContext'
import { SiteConfigProvider } from './SiteConfigContext'
import { reunionConfig } from './reunion.config'
import './index.css'
import App from './App.tsx'

// Load Google Analytics dynamically from config
if (reunionConfig.googleAnalyticsId) {
  const script = document.createElement('script')
  script.src = `https://www.googletagmanager.com/gtag/js?id=${reunionConfig.googleAnalyticsId}`
  script.async = true
  document.head.appendChild(script)
  window.dataLayer = window.dataLayer || []
  window.gtag = function (...args: unknown[]) { window.dataLayer!.push(args) }
  window.gtag('js', new Date())
  window.gtag('config', reunionConfig.googleAnalyticsId)
}

const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID || ''

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <GoogleOAuthProvider clientId={googleClientId}>
      <AuthProvider>
        <SiteConfigProvider>
          <App />
        </SiteConfigProvider>
      </AuthProvider>
    </GoogleOAuthProvider>
  </StrictMode>,
)
