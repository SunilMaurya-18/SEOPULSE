import { Component, type ErrorInfo, type ReactNode } from 'react'
import { AlertTriangle, RefreshCw } from 'lucide-react'

import { Button } from '@/components/ui/Button'
import { captureError } from '@/lib/monitoring'

interface ErrorBoundaryProps {
  children: ReactNode
  /** When this value changes the boundary clears its error (e.g. on navigation). */
  resetKey?: unknown
  fullScreen?: boolean
}

interface ErrorBoundaryState {
  error: Error | null
  resetKey: unknown
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { error: null, resetKey: this.props.resetKey }

  static getDerivedStateFromError(error: Error): Partial<ErrorBoundaryState> {
    return { error }
  }

  static getDerivedStateFromProps(
    props: ErrorBoundaryProps,
    state: ErrorBoundaryState,
  ): Partial<ErrorBoundaryState> | null {
    if (props.resetKey !== state.resetKey) {
      return { error: null, resetKey: props.resetKey }
    }
    return null
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Unhandled UI error', error, info.componentStack)
    captureError(error, { componentStack: info.componentStack })
  }

  private reset = () => this.setState({ error: null })

  render() {
    if (!this.state.error) return this.props.children

    return (
      <div
        role="alert"
        className={
          this.props.fullScreen
            ? 'flex min-h-screen items-center justify-center bg-canvas px-4'
            : 'flex min-h-[50vh] items-center justify-center px-4'
        }
      >
        <div className="w-full max-w-md rounded-lg border border-default bg-surface p-6 text-center">
          <AlertTriangle className="mx-auto h-6 w-6 text-warning" aria-hidden />
          <h1 className="mt-3 font-display text-lg font-semibold text-main">
            Something went wrong
          </h1>
          <p className="mt-1 text-sm text-muted">
            This page hit an unexpected error. Try again, or reload the app if it keeps happening.
          </p>
          <div className="mt-5 flex justify-center gap-2">
            <Button size="sm" variant="secondary" onClick={this.reset}>
              <RefreshCw className="h-3.5 w-3.5" />
              Try again
            </Button>
            <Button size="sm" onClick={() => window.location.reload()}>
              Reload app
            </Button>
          </div>
        </div>
      </div>
    )
  }
}
