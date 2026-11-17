# Security Requirements — Habit Tracker

- Hash passwords using Spring Security's supported adaptive password encoder (Argon2id or BCrypt with calibrated work factor).
- Never store plaintext passwords, reset tokens, or session secrets.
- Use secure, HttpOnly, SameSite cookies if cookie authentication is selected; define CSRF protection.
- If JWT is selected, avoid localStorage for long-lived tokens; implement refresh rotation and revocation.
- Enforce resource ownership in service layer for every habit/completion/export operation.
- Validate and normalize email, strings, dates, enums, and pagination.
- Rate-limit login, registration, and password recovery.
- Use generic password-reset responses to prevent account enumeration.
- Restrict CORS to known frontend origins; do not use wildcard origins with credentials.
- Use TLS, secure headers, dependency scanning, and secret scanning.
- Do not log passwords, tokens, habit content, or full personal exports.
- Protect export and deletion endpoints with re-authentication where appropriate.
- Define retention, backups, and deletion behavior before production release.
- Threat model: account takeover, IDOR, CSRF, XSS, SQL injection, credential stuffing, data leakage, and timezone/date manipulation.
