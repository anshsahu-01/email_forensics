# AGENTS.md - Engineering Handoff Document

## 1. Project Overview
**Purpose**: AI-Powered Email Threat Detection, GeoLocation and Forensic Intelligence Platform.
**Current Functionality**: Parses uploaded `.eml` files, extracts headers, routes, URLs, and indicators. Queries VirusTotal for URL reputation and provides a dashboard interface for visualization.
**Prototype/Demo Status**: A working prototype with a functional backend and Next.js frontend.
**Intended Final Goal**: A fully automated platform featuring AI-powered threat analysis and comprehensive forensic intelligence (AI/ML is NOT yet implemented).

## 2. Actual Tech Stack
**Backend**:
- Java 17
- Spring Boot 3.2.5
- Maven
- PostgreSQL
- JPA/Hibernate
- Jakarta Mail 2.0.2
- jsoup 1.17.2
- Lombok 1.18.38
- HTTP Client: RestTemplate (for VirusTotal)

**Frontend**:
- Next.js 16.3.3
- React 19.2.8
- TypeScript 5
- TailwindCSS v4
- lucide-react 1.38.0

**Infrastructure**:
- PostgreSQL (Neon config present in properties)

## 3. Repository Structure
```text
email_forensics/
├── backend/
│   ├── src/main/java/com/emailForemsic/emailForensic/
│   ├── src/test/java/com/emailForemsic/emailForensic/
│   └── pom.xml
├── apps/
│   └── web/
│       ├── src/app/
│       └── package.json
├── .env.example
├── .gitignore
└── AGENTS.md
```

## 4. Backend Architecture
```text
POST /api/v1/emails/analyze
        ↓
EmailAnalysisController
        ↓
EmailCaseService
        ↓
EmailParserService (parses headers, bodies, routes via Jakarta Mail, Jsoup)
        ↓
EmailParsedResult (DTO)
        ↓
Persistence (EmailCase, EmailHeader, EmailIndicator) via EmailCaseRepository
        ↓
External enrichment (VirusTotalService, GeoLocationService)
        ↓
Response (EmailCase returned to client)
```

## 5. Implemented Features

### Feature 1 — Email Parser
- Support for: `.eml` upload, From, To, CC, Reply-To, Subject, Date, Message-ID, Return-Path, SHA-256 calculation.
- Persistence via `EmailCase` and `EmailHeader`.

### Feature 2 — Received IP
- Received header parsing (regex-based).
- Multiple hops extracted (From, By, timestamp).
- IP pattern matching (IPv4 and IPv6).
- Private/local/loopback detection.
- Originating IP heuristic (finding the oldest public IP).
- Persistence in `EmailCase.receivedHeaders` as JSON and `originatingIp` column.
- Dashboard route display implemented.

### Feature 3 — SPF/DKIM/DMARC
- Parses authentication results from email headers (`Authentication-Results` and `Received-SPF`).
- **Limitation**: This currently ONLY parses results from the headers. It does NOT automatically mean live SPF DNS verification, DKIM cryptographic verification, or DMARC policy evaluation.

### Feature 4 — URL / IOC Extraction
- Plain text URLs and HTML URLs (via Jsoup).
- Multipart handling.
- Normalization and deduplication.
- IP-based URLs detection (in frontend).
- Attachment exclusion.
- Persistence as `EmailIndicator` (type="URL").
- Dashboard display implemented.

### Feature 5 — VirusTotal
- URL reputation lookup via `https://www.virustotal.com/api/v3/urls/`.
- Base64-url encoding of URL identifier.
- Enriching URL indicators with malicious, suspicious, harmless, undetected counts.
- Classification logic (if malicious > 0 -> MALICIOUS, etc).
- Error handling (returns ERROR status on `RestClientException`).
- Non-fatal enrichment behavior (failure doesn't crash email processing).
- Tests exist (`VirusTotalServiceTest.java`).

## 6. API Endpoints
- `POST /api/v1/emails/analyze`: Analyzes uploaded .eml file. Request: `multipart/form-data` with `file`. Response: `201 Created` with `EmailCase`.
- `GET /api/v1/cases`: Fetches all past cases for dashboard. Response: `200 OK` with list of `EmailCase`.
- `GET /api/v1/cases/{id}`: Fetches a single case by ID. Response: `200 OK` with `EmailCase` or `404 Not Found`.

## 7. Database Model
- `EmailCase`: Main entity (fileHash, analysisStatus, threatScore, originatingIp, receivedHeaders, rawBody).
- `EmailHeader`: OneToOne with `EmailCase` (subject, senderFrom, to, cc, replyTo, date, messageId, returnPath, authentication statuses).
- `EmailIndicator`: ManyToOne with `EmailCase` (type, value, details, VirusTotal fields).
- Schema management: Uses `spring.jpa.hibernate.ddl-auto=update`. No Flyway/Liquibase present.

## 8. Environment Variables
Variable NAMES only (verified from `.env.example`):
- `DATABASE_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `PORT`
- `NODE_ENV`
- `CLERK_SECRET_KEY`
- `CLERK_PUBLISHABLE_KEY`
- `VIRUSTOTAL_API_KEY`
- `ABUSEIPDB_API_KEY`
- `REDIS_URL`
- `STORAGE_ENDPOINT`
- `STORAGE_ACCESS_KEY`
- `STORAGE_SECRET_KEY`
- `STORAGE_BUCKET`
- `AI_SERVICE_URL`

## 9. Frontend
Dashboard implemented in `apps/web/src/app/page.tsx`:
- Upload flow posts to `/emails/analyze` and sets loading states.
- Displays case history list.
- Email Route section displays Originating IP and Received Headers hops.
- Authentication Checks display SPF, DKIM, DMARC badges.
- URL / IOC section lists URLs, flags IP hosts, and shows VirusTotal status/counts.
- Appropriate loading/error/empty states handled.

## 10. Testing
**Backend**:
```bash
cd backend
.\mvnw clean test -q
```
Test files exist in `backend/src/test/java/com/emailForemsic/emailForensic/`.

**Frontend**:
```bash
cd apps/web
npm run lint
npm run build
```

**Git**:
```bash
git diff --check
git status
```

## 11. Validation History
- Backend unit tests exist for `EmailCaseService`, `EmailParserService`, and `VirusTotalService`.
- Frontend linting and building are functional.
- VirusTotal API verification is handled via mocked endpoints/tests in the repository.

## 12. Git Workflow
```text
main
 ↓
short feature branch
 ↓
implement ONE feature
 ↓
backend tests
 ↓
API/DB verification
 ↓
minimal frontend integration
 ↓
frontend lint/build
 ↓
git diff --check
 ↓
manual review
 ↓
commit
 ↓
push
 ↓
merge into main
 ↓
delete feature branch
```

## 13. Engineering Rules
1. Do not rewrite working functionality unnecessarily.
2. Reuse existing architecture.
3. Avoid duplicate entities/models.
4. Avoid duplicate indicators.
5. External intelligence services are enrichment layers.
6. External service failures should not normally destroy core email parsing.
7. Never hardcode secrets.
8. Never log secrets.
9. Keep frontend changes minimal.
10. Add tests for every feature.
11. Run full validation before merge.
12. Preserve useful forensic evidence.
13. Do not silently change forensic heuristics.
14. Any heuristic change must be documented and tested.

## 14. Known Limitations
- Authentication headers are merely parsed; no true cryptographic DKIM validation or DNS-based SPF verification is performed.
- VirusTotal dependency could cause rate limit issues (though handled non-fatally).
- Browser automation could not be completed (Playwright not present).
- Missing AI/ML functionality (AI service not integrated).
- Threat scoring is rudimentary and incomplete.

## 15. Remaining Roadmap

### High priority
- AbuseIPDB IP reputation (`NOT IMPLEMENTED`)
- richer IP intelligence (`NOT IMPLEMENTED`)
- DNS/WHOIS (`NOT IMPLEMENTED`)
- forensic timeline (`NOT IMPLEMENTED`)
- threat scoring (`NOT IMPLEMENTED` fully)

### Medium priority
- case management improvements (`NOT IMPLEMENTED`)
- visualization (`NOT IMPLEMENTED`)
- report generation (`NOT IMPLEMENTED`)

### Later
- AI/ML threat analysis (`NOT IMPLEMENTED`)
- automated threat explanation (`NOT IMPLEMENTED`)
- advanced correlation (`NOT IMPLEMENTED`)

## 16. Recommended Next Feature
**AbuseIPDB IP Reputation**
```text
Originating IP
      ↓
AbuseIPDB lookup
      ↓
Abuse confidence / reputation
      ↓
Enrich existing IP indicator
      ↓
Dashboard
```

## 17. Instructions for the Next Agent
1. Read `AGENTS.md` first.
2. Inspect source before modifying it.
3. Check Git status/branch.
4. Work on one feature at a time.
5. Reuse existing architecture.
6. Add tests.
7. Run backend tests.
8. Run frontend lint/build.
9. Run `git diff --check`.
10. Perform safe live verification where applicable.
11. Never expose secrets.
12. Do not commit unless explicitly instructed.
13. Report changed files, validation, limitations, and Git status.
