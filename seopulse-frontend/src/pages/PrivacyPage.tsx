import { Link } from 'react-router-dom'

import { ContactLink, LegalLayout, LegalSection } from '@/features/legal/LegalLayout'

export function PrivacyPage() {
  return (
    <LegalLayout eyebrow="Legal" title="Privacy Policy" updated="October 3, 2026">
      <LegalSection title="Who we are">
        <p>
          SEOPulse is a website auditing service. This policy explains what personal data we collect when you use
          SEOPulse, why we collect it, and the choices you have. Questions can be sent to <ContactLink />.
        </p>
      </LegalSection>

      <LegalSection title="Data we collect">
        <ul>
          <li>
            <strong>Account details:</strong> your name, email address and a hashed password. We never store your
            password in readable form.
          </li>
          <li>
            <strong>Workspace data:</strong> workspace names, team members and invitations, the websites you add, audit
            schedules, alert settings and report branding.
          </li>
          <li>
            <strong>Audit results:</strong> scores, issues and page details produced when we crawl the websites you
            submit. We only fetch publicly reachable pages and respect robots.txt.
          </li>
          <li>
            <strong>Billing details:</strong> your plan and subscription status. Card details are collected and stored
            by Stripe, not by us.
          </li>
          <li>
            <strong>Mailing list:</strong> if you join our mailing list, your email address and the time you gave
            consent.
          </li>
          <li>
            <strong>Technical data:</strong> IP addresses are used briefly for rate limiting and abuse prevention, and
            error reports may include browser and request details.
          </li>
        </ul>
      </LegalSection>

      <LegalSection title="How we use it">
        <ul>
          <li>To provide the service: run audits, show results, send alerts and generate reports.</li>
          <li>To secure accounts: verify email addresses, reset passwords and prevent abuse.</li>
          <li>To bill paid plans and send receipts and account notices.</li>
          <li>To send product news, only if you joined the mailing list and confirmed your address.</li>
        </ul>
        <p>We do not sell your personal data or use it for third-party advertising.</p>
      </LegalSection>

      <LegalSection title="Service providers">
        <p>We share data only with providers that help us run SEOPulse, and only as needed:</p>
        <ul>
          <li>
            <strong>Stripe</strong> for payments and subscription management.
          </li>
          <li>
            <strong>Our email provider</strong> to deliver verification, alert and account emails.
          </li>
          <li>
            <strong>Sentry</strong> for error monitoring, when enabled.
          </li>
          <li>
            <strong>Our hosting provider</strong>, which stores the database and runs the application.
          </li>
        </ul>
        <p>Some providers may process data outside your country, under safeguards required by applicable law.</p>
      </LegalSection>

      <LegalSection title="Cookies and local storage">
        <p>
          We use a single essential cookie to keep you signed in. It is HTTP-only and cannot be read by scripts. Your
          theme preference is saved in your browser&apos;s local storage. We do not use advertising or tracking
          cookies.
        </p>
      </LegalSection>

      <LegalSection title="How long we keep data">
        <ul>
          <li>
            Page-level audit details are kept for 30 days on Free, 180 days on Pro and 365 days on Agency. Scores and
            trends are kept while your workspace exists.
          </li>
          <li>Account and workspace data is kept while your account is active.</li>
          <li>
            When you delete your account, your data is removed straight away. Encrypted backups are deleted on a rolling
            schedule within six months, and we may keep billing records where the law requires it.
          </li>
        </ul>
      </LegalSection>

      <LegalSection title="Your rights">
        <p>Depending on where you live, including under India&apos;s DPDP Act and the EU and UK GDPR, you can:</p>
        <ul>
          <li>
            <strong>Access and export</strong> your data from <Link to="/settings">Settings</Link> → Privacy &amp; data.
          </li>
          <li>
            <strong>Delete</strong> your account and data from the same page.
          </li>
          <li>
            <strong>Correct</strong> inaccurate details by contacting us.
          </li>
          <li>
            <strong>Withdraw consent</strong> to marketing emails with the unsubscribe link in any email.
          </li>
          <li>
            <strong>Complain</strong> to your local data protection authority.
          </li>
        </ul>
        <p>
          For any request or grievance, email <ContactLink />. We reply within 30 days.
        </p>
      </LegalSection>

      <LegalSection title="Children">
        <p>SEOPulse is not intended for anyone under 18, and we do not knowingly collect their data.</p>
      </LegalSection>

      <LegalSection title="Changes to this policy">
        <p>
          If we make material changes, we will update the date above and notify account holders by email before the
          changes take effect.
        </p>
      </LegalSection>
    </LegalLayout>
  )
}
