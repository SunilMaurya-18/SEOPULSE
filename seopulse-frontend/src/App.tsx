import { useState } from 'react'
import { BrowserRouter } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'

import { AppRoutes } from '@/routes/AppRoutes'
import { ThemeProvider } from '@/lib/theme'
import { AuthProvider } from '@/lib/auth'
import { createQueryClient } from '@/lib/queryClient'
import { ToastProvider } from '@/lib/toast'

function App() {
  const [queryClient] = useState(createQueryClient)

  return (
    <ThemeProvider>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <ToastProvider>
            <BrowserRouter>
              <AppRoutes />
            </BrowserRouter>
          </ToastProvider>
        </AuthProvider>
      </QueryClientProvider>
    </ThemeProvider>
  )
}

export default App
