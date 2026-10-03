import type { RazorpayCheckout, RazorpayPayment } from '@/api/saas'

interface RazorpayOptions {
  key: string
  subscription_id: string
  name: string
  description: string
  prefill: { name: string; email: string }
  theme: { color: string }
  handler: (payment: RazorpayPayment) => void
  modal: { ondismiss: () => void }
}

declare global {
  interface Window {
    Razorpay?: new (options: RazorpayOptions) => { open: () => void }
  }
}

const SCRIPT_URL = 'https://checkout.razorpay.com/v1/checkout.js'

let scriptPromise: Promise<void> | null = null

function loadCheckout(): Promise<void> {
  if (window.Razorpay) return Promise.resolve()
  scriptPromise ??= new Promise<void>((resolve, reject) => {
    const script = document.createElement('script')
    script.src = SCRIPT_URL
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => {
      scriptPromise = null
      reject(new Error('Razorpay Checkout failed to load. Check your connection and try again.'))
    }
    document.head.appendChild(script)
  })
  return scriptPromise
}

export class CheckoutDismissedError extends Error {}

/** Opens Razorpay Checkout; resolves with the signed payment, rejects if the user closes it. */
export async function openRazorpayCheckout(session: RazorpayCheckout, description: string): Promise<RazorpayPayment> {
  await loadCheckout()
  const Razorpay = window.Razorpay
  if (!Razorpay) throw new Error('Razorpay Checkout is unavailable.')
  return new Promise<RazorpayPayment>((resolve, reject) => {
    new Razorpay({
      key: session.keyId,
      subscription_id: session.subscriptionId,
      name: 'SEOPulse',
      description,
      prefill: { name: session.name, email: session.email },
      theme: { color: '#e8413b' },
      handler: resolve,
      modal: { ondismiss: () => reject(new CheckoutDismissedError('Checkout closed')) },
    }).open()
  })
}

/** Visitors in India default to paying in rupees. */
export function prefersInr() {
  try {
    return Intl.DateTimeFormat().resolvedOptions().timeZone === 'Asia/Kolkata'
  } catch {
    return false
  }
}
