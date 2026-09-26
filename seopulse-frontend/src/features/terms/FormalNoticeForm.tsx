import { useState, type FormEvent } from 'react'

export function FormalNoticeForm() {
  const [submitted, setSubmitted] = useState(false)

  function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setSubmitted(true)
    e.currentTarget.reset()
    window.setTimeout(() => setSubmitted(false), 5000)
  }

  return (
    <div className="mt-4 space-y-4 rounded-lg border border-[#27272a] bg-[#101012] p-6 shadow-sm">
      <div className="space-y-1">
        <div className="text-lg font-bold text-white">
          Submit Formal Legal Notice
        </div>
        <p className="text-xs text-zinc-400">
          Use this authenticated telemetry channel to communicate with SEOPulse
          Legal &amp; Data Protection Officers directly.
        </p>
      </div>

      <form className="space-y-4" onSubmit={handleSubmit}>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div className="space-y-1.5">
            <label className="block font-mono text-[11px] text-zinc-300">
              ORGANIZATION / COMPANY
            </label>
            <input
              required
              type="text"
              placeholder="Acme Technologies Inc."
              className="h-9 w-full rounded border border-[#27272a] bg-[#0e0e10] px-3 text-sm text-white placeholder-zinc-600 transition-colors focus:border-[#b91c1c] focus:outline-none"
            />
          </div>
          <div className="space-y-1.5">
            <label className="block font-mono text-[11px] text-zinc-300">
              LEGAL CONTACT EMAIL
            </label>
            <input
              required
              type="email"
              placeholder="counsel@acme.com"
              className="h-9 w-full rounded border border-[#27272a] bg-[#0e0e10] px-3 text-sm text-white placeholder-zinc-600 transition-colors focus:border-[#b91c1c] focus:outline-none"
            />
          </div>
        </div>

        <div className="space-y-1.5">
          <label className="block font-mono text-[11px] text-zinc-300">
            INQUIRY NATURE
          </label>
          <select className="h-9 w-full rounded border border-[#27272a] bg-[#0e0e10] px-3 text-sm text-white transition-colors focus:border-[#b91c1c] focus:outline-none">
            <option value="dpa">
              Custom DPA &amp; Enterprise Security Review
            </option>
            <option value="subpoena">
              Subpoena / Government Inquiry Request
            </option>
            <option value="sla">SLA Claim or Downtime Credit Audit</option>
            <option value="crawling">
              Domain Ownership Validation / Disallow Inquiries
            </option>
            <option value="other">Other Governance Question</option>
          </select>
        </div>

        <div className="space-y-1.5">
          <label className="block font-mono text-[11px] text-zinc-300">
            MEMORANDUM DETAILS
          </label>
          <textarea
            rows={3}
            placeholder="Provide docket numbers, organization domain FQDNs, or relevant clause citations..."
            className="w-full rounded border border-[#27272a] bg-[#0e0e10] p-3 text-sm text-white placeholder-zinc-600 transition-colors focus:border-[#b91c1c] focus:outline-none"
          />
        </div>

        <div className="flex flex-col justify-between gap-2 pt-1 sm:flex-row sm:items-center">
          <div className="flex items-center gap-2">
            <input
              required
              id="legal-cert"
              type="checkbox"
              className="h-4 w-4 rounded border-[#27272a] bg-[#0e0e10] accent-[#b91c1c]"
            />
            <label
              htmlFor="legal-cert"
              className="font-mono text-[11px] text-zinc-300"
            >
              I certify authority to submit on behalf of entity
            </label>
          </div>
          <button
            type="submit"
            className="rounded bg-[#b91c1c] px-4 py-2 text-sm font-semibold text-white shadow-sm transition-colors hover:bg-[#dc2626]"
          >
            Submit Formal Legal Notice
          </button>
        </div>

        {submitted && (
          <div className="rounded border border-emerald-500/40 bg-[#18181b] p-2 text-center font-mono text-sm text-emerald-400">
            ✔ Electronic dispatch registered. Acknowledgment returned within 1
            business day.
          </div>
        )}
      </form>
    </div>
  )
}
