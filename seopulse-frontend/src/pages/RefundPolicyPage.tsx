import { Link } from 'react-router-dom'

import { ContactLink, LegalLayout, LegalSection } from '@/features/legal/LegalLayout'

export function RefundPolicyPage() {
  return (
    <LegalLayout eyebrow="Legal" title="Refund Policy" updated="October 3, 2026">
      <LegalSection title="Free plan">
        <p>
          The Free plan costs nothing and needs no card. You can try SEOPulse there before choosing a paid plan, and Pro
          includes a 14-day trial.
        </p>
      </LegalSection>

      <LegalSection title="Cancelling">
        <p>
          You can cancel a paid plan at any time from <Link to="/settings">Settings</Link> → Billing. Your plan stays
          active until the end of the period you have paid for, and you will not be charged again.
        </p>
      </LegalSection>

      <LegalSection title="Refunds">
        <ul>
          <li>
            <strong>First payment:</strong> if SEOPulse is not right for you, ask for a full refund within 14 days of
            your first paid subscription charge.
          </li>
          <li>
            <strong>Renewals:</strong> monthly and yearly renewals are not refundable, so please cancel before the
            renewal date if you no longer need the plan.
          </li>
          <li>
            <strong>Billing mistakes:</strong> duplicate charges or charges after a confirmed cancellation are always
            refunded in full.
          </li>
        </ul>
      </LegalSection>

      <LegalSection title="How to request a refund">
        <p>
          Email <ContactLink /> from the address on your account, with the workspace name and the date of the charge.
          Approved refunds go back to the original payment method through Stripe, usually within 5 to 10 business days
          depending on your bank.
        </p>
      </LegalSection>

      <LegalSection title="Deleting your account">
        <p>
          Deleting your account cancels any subscriptions on workspaces you own alone, straight away. If you are within
          the 14-day window, request your refund before deleting so we can find the charge.
        </p>
      </LegalSection>
    </LegalLayout>
  )
}
