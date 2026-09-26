import { Outlet, useLocation } from 'react-router-dom'
import { useEffect, useState } from 'react'

import { Sidebar } from '@/components/layout/Sidebar'
import { Topbar } from '@/components/layout/Topbar'

export function AppLayout() {
  const location = useLocation()
  const [enterKey, setEnterKey] = useState(location.pathname)

  useEffect(() => {
    setEnterKey(location.pathname)
  }, [location.pathname])

  return (
    <div className="app-canvas flex h-dvh overflow-hidden">
      <Sidebar />
      <div className="flex min-h-0 min-w-0 flex-1 flex-col">
        <Topbar />
        <main className="min-h-0 flex-1 overflow-y-auto overscroll-contain">
          <div
            key={enterKey}
            className="animate-page-enter mx-auto w-full max-w-[1440px] px-3 py-5 sm:px-5 lg:px-6 lg:py-6"
          >
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}
