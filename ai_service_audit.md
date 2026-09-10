# AI Service Audit Report
### Email Forensics Project — `ai-service/`
**Audit Date**: 2026-09-10 | **Status**: Read-Only, No Changes Made

---

## 1. TECHNOLOGY STACK

| Component | Detail |
|---|---|
| **Language** | Python 3.14.0 |
| **Framework** | FastAPI 0.141.1 |
| **ASGI Server** | Uvicorn 0.52.4 |
| **Package manager** | pip (venv at `ai-service/venv/`) — **no `requirements.txt` file exists** |
| **AI/LLM library** | `langchain-core` 1.6.2, `langchain-community` 0.4.2, `langchain-chroma` 1.1.0, `langchain-text-splitters` 1.1.2, `langchain_classic` 1.0.8 |
| **Ollama integration** | `langchain_ollama` — **imported in code but NOT found in venv site-packages** |
| **Vector database** | ChromaDB 1.5.9 (local persistent store) |
| **Embedding model** | `nomic-embed-text` via Ollama |
| **LLM model** | `llama3.2:3b` via Ollama |
| **Data validation** | Pydantic 2.13.5 |
| **HTTP client** | `requests` 2.34.2 (used for RDAP lookups) |
| **External services** | RDAP (`https://rdap.org/domain/{domain}`) — live network call, no API key required |
| **Database** | **None** — the AI service is stateless |
| **ML libraries present in venv** | `scikit-learn` 1.9.0, `xgboost` 3.4.1, `numpy` 2.5.2, `scipy` 1.18.1, `onnxruntime` 1.29.0 — **none used in any source file** |

> [!WARNING]
> `langchain_ollama` is **imported** at the top of both `main.py` and `llm_service.py` but its distribution package (`langchain_ollama-*.dist-info`) is **not present** in `venv/Lib/site-packages`. The service cannot start without it.

---

## 2. PROJECT STRUCTURE

```
ai-service/
├── main.py                      ← FastAPI application, all routing, heuristics, scoring, orchestration
├── services/
│   ├── __init__.py
│   ├── llm_service.py           ← Wraps ChatOllama (llama3.2:3b), prompt invocation, JSON extraction
│   ├── rag_service.py           ← Chroma similarity search helpers (NOT imported by main.py)
│   └── whois_service.py         ← Live RDAP lookup via rdap.org
├── prompts/
│   ├── __init__.py
│   └── forensic_prompt.py       ← System prompt string for the LLM
├── ingestion/
│   └── cisa_ingestion.py        ← One-time script: loads CISA .txt files into ChromaDB
├── data/
│   └── cisa/
│       ├── AA26-204A.txt        ← CISA advisory (phishing / Zimbra)
│       └── AA26-237A.txt        ← CISA advisory (empty file — 0 bytes logged)
├── test_llm.py                  ← Manual smoke test — calls a non-existent function name (BROKEN)
├── test_rag.py                  ← Manual RAG smoke test
├── test_whois.py                ← Manual WHOIS smoke test
└── venv/                        ← Python 3.14 virtual environment
```

> [!NOTE]
> `rag_service.py` is a standalone helper module but is **never imported** by `main.py`. The RAG logic in `main.py` is reimplemented inline and uses `vector_db` directly. `rag_service.py` is dead code in production.

---

## 3. ENTRY POINT

**File**: [`main.py`](file:///d:/email_forensics/ai-service/main.py#L1769-L1778)

```python
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
```

**To start** (after activating venv and installing missing packages):
```bash
cd ai-service
venv\Scripts\activate
python main.py
# OR
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

The server binds to **port 8000** (matches `AI_SERVICE_URL=http://localhost:8000` in `.env.example`).

**Startup side-effects**:
- Instantiates `LLMService()` — creates a `ChatOllama` connection to local Ollama on startup.
- Tries to connect to ChromaDB at `./chroma_db` (relative to working directory). If ChromaDB is absent, `vector_db` is set to `None` and RAG silently degrades.
- Both of these happen at **module load time** (import time), so a missing Ollama instance will cause startup failure.

---

## 4. API ENDPOINTS

### `GET /`  — Health Check

| | |
|---|---|
| **Purpose** | Liveness check |
| **Request** | None |
| **Response** | `{"service": "Email Forensics AI Service", "status": "running", "llm": "llama3.2:3b", "embedding_model": "nomic-embed-text", "rag_collection": "cisa_threat_intelligence"}` |
| **Source** | [`main.py:1743`](file:///d:/email_forensics/ai-service/main.py#L1743-L1762) |

---

### `POST /api/v1/analyze`  — Core Analysis Endpoint

| | |
|---|---|
| **Purpose** | Full email forensic analysis pipeline |
| **Source** | [`main.py:1334`](file:///d:/email_forensics/ai-service/main.py#L1334-L1736) |

**Request body** (`application/json`):
```json
{
  "subject": "string (optional, default '')",
  "bodyText": "string (optional, default '')",
  "urls": ["string", "..."] ,
  "senderDomain": "string (optional, default '')"
}
```

**Response body** (`200 OK`):
```json
{
  "threatScore": 45.0,
  "riskLevel": "MEDIUM_RISK | HIGH_RISK | LOW_RISK",
  "verdict": "MALICIOUS | SUSPICIOUS | LEGITIMATE",
  "confidence": 72.0,
  "indicators": ["Urgency or pressure language detected: 'urgent'", "..."],
  "attackTechniques": [
    {
      "technique": "Phishing / Social Engineering",
      "mitreId": "T1566",
      "evidence": "...",
      "confidence": 0.90
    }
  ],
  "iocs": {
    "domains": ["example.com"],
    "urls": ["https://example.com/verify"],
    "ipAddresses": [],
    "emailAddresses": [],
    "hashes": [],
    "other": []
  },
  "originAnalysis": {
    "senderDomain": "example.com",
    "domainAgeAssessment": "Long-established domain",
    "urlSenderRelationship": "No mismatch detected",
    "registrar": "...",
    "nameservers": [],
    "attribution": "Cannot be determined from available evidence"
  },
  "evidence": [
    {
      "type": "EMAIL_INDICATOR | URL_ANALYSIS | WHOIS_RDAP | THREAT_INTELLIGENCE",
      "source": "Submitted email | URL parser | RDAP | CISA knowledge base",
      "finding": "...",
      "directEvidence": true
    }
  ],
  "reasoning": ["evidence-based reason 1", "evidence-based reason 2"],
  "summary": "short forensic summary (LLM-generated)",
  "ragInsights": "Retrieved N relevant historical threat intelligence records...",
  "whoisAnalysis": {
    "success": true,
    "domain": "example.com",
    "registrar": "...",
    "createdDate": "...",
    "updatedDate": "...",
    "expirationDate": "...",
    "domainAgeDays": 1234,
    "nameservers": [],
    "status": [],
    "rdapUrl": "https://rdap.org/domain/example.com"
  },
  "urlAnalysis": {
    "urlsAnalyzed": 1,
    "results": [
      {
        "url": "https://example.com/verify",
        "domain": "example.com",
        "validDomain": true,
        "matchesSenderDomain": true
      }
    ],
    "senderDomain": "example.com",
    "domainMismatchDetected": false
  },
  "graphs": {
    "nodes": [{"id": "email", "type": "email", "label": "Analyzed Email"}, "..."],
    "edges": [{"source": "email", "target": "domain:example.com", "relationship": "sent-from"}, "..."]
  }
}
```

**Error response** (pipeline exception): Same structure with `threatScore: 0.0`, `verdict: "UNKNOWN"`, and error message in `reasoning[0]`.

---

## 5. AI/LLM FLOW

```
POST /api/v1/analyze
       │
       ▼
1. NORMALIZE inputs (subject, bodyText, urls, senderDomain)
       │
       ▼
2. URL ANALYSIS  [main.py: analyze_urls()]
   - Extract domains from each URL
   - Compare URL root domain vs sender domain
   - Detect domain mismatches
       │
       ▼
3. INDICATOR DETECTION  [main.py: detect_indicators()]
   - Keyword scan of subject + body: urgency, credentials, financial terms
   - URL structure checks (@ in URL, suspicious keywords)
   - Brand impersonation check on sender domain
       │
       ▼
4. HEURISTIC SCORING  [main.py: calculate_heuristic_score()]
   - Base score: 10
   - Keyword hits add 10–25 points each
   - URL mismatches, suspicious TLDs add points
   - Capped at 100
       │
       ▼
5. WHOIS / RDAP  [whois_service.py: lookup_domain()]
   - Live HTTP GET to https://rdap.org/domain/{domain}
   - Extracts: registrar, creation/expiry dates, nameservers
   - Calculates domain age in days
   - WHOIS score: +10 (<30 days), +5 (<90 days), +0 otherwise
       │
       ▼
6. FINAL SCORE = heuristic_score + whois_score (capped 100)
   RISK LEVEL: >=70 → HIGH_RISK, >=35 → MEDIUM_RISK, else LOW_RISK
   VERDICT:    >=70 → MALICIOUS, >=35 → SUSPICIOUS, else LEGITIMATE
       │
       ▼
7. RAG RETRIEVAL  [main.py: retrieve_rag_context()]
   - Builds a text query from subject + domain + urls + body
   - Calls ChromaDB.similarity_search(query, k=3)
   - Uses OllamaEmbeddings("nomic-embed-text") to embed the query
   - Retrieves up to 3 CISA advisory chunks
   - Returns combined text + source attribution string
       │
       ▼
8. CONFIDENCE CALCULATION  [main.py: calculate_confidence()]
   - Base: 50
   - +5 per indicator threshold (1, 3, 5, 8 indicators)
   - +5 if WHOIS data available
   - +5 if RAG context found
   - +5 if score >= 70, +2 if score >= 35
   - Capped at 95
       │
       ▼
9. BUILD ANCILLARY ARTIFACTS
   - attack_techniques (MITRE ATT&CK mappings, hardcoded rules)
   - iocs (domains/urls extracted)
   - evidence (structured list)
   - origin_analysis (WHOIS-derived summary)
   - graphs (nodes/edges for visualization)
       │
       ▼
10. LLM CALL  [llm_service.py: LLMService.analyze()]
    - Uses ChatOllama(model="llama3.2:3b", temperature=0.1, format="json")
    - System prompt: FORENSIC_STRUCTURED_PROMPT (forensic_prompt.py)
    - Human prompt: supplies all authoritative scores + indicators + WHOIS + RAG
    - LLM task: write ≤2 reasoning items + 1 summary sentence
    - LLM must NOT change the score/verdict
    - Response parsed via _extract_json_object() with markdown-fence stripping
    - If LLM returns empty/invalid reasoning → deterministic fallback from indicators
       │
       ▼
11. RETURN EmailAnalysisResponse (all 14 fields)
```

---

## 6. OLLAMA STATUS

| Item | Status |
|---|---|
| **Ollama integrated?** | Yes — `langchain_ollama.ChatOllama` and `langchain_ollama.OllamaEmbeddings` are used |
| **LLM model expected** | `llama3.2:3b` |
| **Embedding model expected** | `nomic-embed-text` |
| **LLM model configured at** | Hardcoded in two places: `main.py:39` (`LLM_MODEL = "llama3.2:3b"`) and `llm_service.py:14` (`ChatOllama(model="llama3.2:3b", ...)`) |
| **Embedding model configured at** | Hardcoded: `main.py:37` and `rag_service.py:20` |
| **Ollama URL** | Default Ollama URL (`http://localhost:11434`) — never explicitly configured |
| **Local-only or configurable?** | **Local-only.** No environment variable controls the Ollama host or port. |
| **`langchain_ollama` installed?** | **No** — the package is imported but not found in `venv/Lib/site-packages` |

**What you need to run it locally**:
1. Install [Ollama](https://ollama.com) and ensure it is running: `ollama serve`
2. Pull both required models:
   ```bash
   ollama pull llama3.2:3b
   ollama pull nomic-embed-text
   ```
3. Install the missing Python package:
   ```bash
   venv\Scripts\activate
   pip install langchain-ollama
   ```
4. Run the ingestion script to populate ChromaDB (one-time):
   ```bash
   python ingestion/cisa_ingestion.py
   ```
5. Start the service:
   ```bash
   python main.py
   ```

---

## 7. INPUT EXPECTATIONS

The AI service currently accepts **only 4 fields**:

| Field | Type | Required | Description |
|---|---|---|---|
| `subject` | `string` | No | Email subject line |
| `bodyText` | `string` | No | Plain text body |
| `urls` | `List[string]` | No | URLs extracted from the email |
| `senderDomain` | `string` | No | Domain part of the sender address |

**What it does NOT receive from Spring Boot** (despite being available):

| Available in Spring Boot | Sent to AI? |
|---|---|
| Raw email (EML) | ❌ No |
| Originating/connecting IP | ❌ No |
| SPF / DKIM / DMARC results | ❌ No |
| VirusTotal URL enrichment | ❌ No |
| AbuseIPDB IP reputation | ❌ No |
| MaxMind GeoIP / ASN | ❌ No |
| RDAP / WHOIS from Spring | ❌ No |
| Spoofing analysis results | ❌ No |
| Spring-computed threat score | ❌ No |
| Received-chain / hop data | ❌ No |
| SHA-256 file hash | ❌ No |

The AI service **independently re-runs** its own RDAP lookup on the sender domain. It does not consume the richer forensic data already computed by Spring Boot.

---

## 8. OUTPUT

The full JSON response structure returned by `POST /api/v1/analyze`:

```json
{
  "threatScore": 45.0,
  "riskLevel": "MEDIUM_RISK",
  "verdict": "SUSPICIOUS",
  "confidence": 72.0,
  "indicators": ["string", "..."],
  "attackTechniques": [
    {
      "technique": "string",
      "mitreId": "string",
      "evidence": "string",
      "confidence": 0.90
    }
  ],
  "iocs": {
    "domains": [],
    "urls": [],
    "ipAddresses": [],
    "emailAddresses": [],
    "hashes": [],
    "other": []
  },
  "originAnalysis": {
    "senderDomain": "string",
    "domainAgeAssessment": "string",
    "urlSenderRelationship": "string",
    "registrar": "string | null",
    "nameservers": [],
    "attribution": "Cannot be determined from available evidence"
  },
  "evidence": [
    {
      "type": "string",
      "source": "string",
      "finding": "string | object",
      "directEvidence": true,
      "contextOnly": false
    }
  ],
  "reasoning": ["string", "string"],
  "summary": "string",
  "ragInsights": "string",
  "whoisAnalysis": { ... },
  "urlAnalysis": { ... },
  "graphs": {
    "nodes": [{ "id": "string", "type": "string", "label": "string" }],
    "edges": [{ "source": "string", "target": "string", "relationship": "string" }]
  }
}
```

**Fields used by Spring Boot** (stored as columns in `EmailCase`):
`threatScore`, `riskLevel`, `verdict`, `confidence`, `summary`, `reasoning`, `indicators`, `attackTechniques`, `iocs`, `originAnalysis`, `ragInsights`, `whoisAnalysis`, `urlAnalysis`, `graphs`, `error`

---

## 9. SPRING BOOT INTEGRATION READINESS

**Integration is architecturally complete but the AI service cannot start due to a missing dependency.**

### What IS wired up ✅

- [`AiServiceClient.java`](file:///d:/email_forensics/backend/src/main/java/com/emailForemsic/emailForensic/service/AiServiceClient.java) — Spring `@Service` that HTTP-POSTs to `{ai.service.url}/api/v1/analyze`
- [`AiAnalysisRequest.java`](file:///d:/email_forensics/backend/src/main/java/com/emailForemsic/emailForensic/dto/AiAnalysisRequest.java) — DTO with `subject`, `bodyText`, `urls`, `senderDomain` — **perfectly matches** the Python `EmailAnalysisRequest` model
- [`AiAnalysisResponse.java`](file:///d:/email_forensics/backend/src/main/java/com/emailForemsic/emailForensic/dto/AiAnalysisResponse.java) — DTO with all 14 response fields — matches the Python `EmailAnalysisResponse` model
- [`EmailCaseService.java:497`](file:///d:/email_forensics/backend/src/main/java/com/emailForemsic/emailForensic/service/EmailCaseService.java#L497-L638) — calls `aiServiceClient.analyze()` after parsing and persists all AI fields into `EmailCase`
- `ai.service.url` — Spring config property, defaults to `http://localhost:8000`

### What is blocking ❌

1. **`langchain_ollama` not installed** — `main.py` fails to import at startup
2. **Ollama not necessarily running** — no local Ollama process means startup failure
3. **ChromaDB not populated** — `./chroma_db` directory does not exist (no ingestion has been run)
4. **No `requirements.txt`** — cannot reliably reproduce the environment

### Expected request from Spring Boot

```
POST http://localhost:8000/api/v1/analyze
Content-Type: application/json

{
  "subject": "...",
  "bodyText": "...",
  "urls": ["https://..."],
  "senderDomain": "example.com"
}
```

---

## 10. PDF REPORT READINESS

Fields from the AI response that are suitable for inclusion in a forensic PDF report:

| Field | PDF Suitability | Notes |
|---|---|---|
| `summary` | ✅ **Excellent** | LLM-generated, ≤40 words, human-readable. Primary field for PDF |
| `reasoning` | ✅ **Good** | 1–2 evidence-backed statements. Suitable as bullet points under the summary |
| `verdict` | ✅ **Good** | `MALICIOUS / SUSPICIOUS / LEGITIMATE` — clear label for a threat badge |
| `riskLevel` | ✅ **Good** | `HIGH_RISK / MEDIUM_RISK / LOW_RISK` |
| `threatScore` | ✅ **Good** | 0–100 numeric, suitable for a risk gauge visualization |
| `confidence` | ✅ **Good** | 0–95, shows reliability of the assessment |
| `indicators` | ✅ **Good** | List of detected phishing indicators |
| `attackTechniques` | ✅ **Good** | MITRE ATT&CK IDs — suitable for a technical annex |
| `iocs` | ✅ **Good** | Domains, URLs — suitable for IOC table |
| `whoisAnalysis` | ✅ **Good** | Domain registration metadata |
| `originAnalysis` | ✅ **Good** | Sender domain assessment |
| `ragInsights` | ⚠️ **Supporting** | CISA source citation — useful as footnote |
| `graphs` | ⚠️ **Complex** | Node/edge data needs rendering library to be useful in PDF |
| `urlAnalysis` | ⚠️ **Supporting** | Per-URL domain match results |

**Not yet available for PDF** (not sent to AI service):
- IP intelligence (GeoIP, ASN, AbuseIPDB)
- SPF/DKIM/DMARC results
- Received-chain / hop analysis
- VirusTotal per-URL verdicts

---

## 11. CONFIGURATION

**Environment variable names only** (no values):

| Variable | Where Used | Notes |
|---|---|---|
| `CHROMA_DB_DIR` | `main.py:30–33` | ChromaDB persist path; defaults to `./chroma_db` |
| `ai.service.url` | Spring Boot `AiServiceClient.java:22` | AI service base URL; defaults to `http://localhost:8000` |

**Hardcoded values that should be environment variables**:

| Value | Location | Current Value |
|---|---|---|
| LLM model name | `main.py:39`, `llm_service.py:14` | `llama3.2:3b` |
| Embedding model | `main.py:37`, `rag_service.py:20` | `nomic-embed-text` |
| Ollama base URL | Not configured — LangChain default | `http://localhost:11434` |
| RDAP base URL | `whois_service.py:101` | `https://rdap.org/domain/` |
| Uvicorn port | `main.py:1776` | `8000` |
| ChromaDB collection name | `main.py:35`, `rag_service.py:12`, `ingestion/cisa_ingestion.py:15` | `cisa_threat_intelligence` |
| CISA data directory | `ingestion/cisa_ingestion.py:12` | `./data/cisa` |
| LLM temperature | `llm_service.py:15` | `0.1` |

---

## 12. DEPENDENCIES

| Package | Version | Purpose |
|---|---|---|
| `fastapi` | 0.141.1 | Web framework — API routing, request/response validation |
| `uvicorn` | 0.52.4 | ASGI server to run FastAPI |
| `pydantic` | 2.13.5 | Request/response schema validation |
| `langchain-core` | 1.6.2 | LangChain base primitives (prompts, messages) |
| `langchain-community` | 0.4.2 | LangChain community integrations |
| `langchain-chroma` | 1.1.0 | ChromaDB vector store integration |
| `langchain-text-splitters` | 1.1.2 | Chunking CISA documents for ingestion |
| `langchain_ollama` | **MISSING** | ChatOllama + OllamaEmbeddings — **must be installed** |
| `chromadb` | 1.5.9 | Local persistent vector database for CISA RAG |
| `requests` | 2.34.2 | HTTP client for RDAP lookups in `whois_service.py` |
| `python-dotenv` | 1.2.3 | `.env` file loading (present in venv, not used in code) |
| `scikit-learn` | 1.9.0 | **Unused** — installed but no imports in any source file |
| `xgboost` | 3.4.1 | **Unused** — installed but no imports in any source file |
| `numpy` | 2.5.2 | Indirect dependency of ChromaDB/sklearn |
| `onnxruntime` | 1.29.0 | **Unused directly** — ChromaDB dependency |

---

## 13. TESTING

### Existing Tests

| File | What It Tests | Quality |
|---|---|---|
| `test_llm.py` | Calls `analyze_with_llama()` from `llm_service.py` | ❌ **BROKEN** — the function `analyze_with_llama` does not exist; `LLMService` is a class with an `analyze()` method |
| `test_rag.py` | Calls `build_rag_context()` from `rag_service.py` | ✅ Works as a manual smoke test if ChromaDB is populated and Ollama is running |
| `test_whois.py` | Calls `lookup_domain("google.com")` | ✅ Works as a live network test |

### What Is NOT Tested

- The main `/api/v1/analyze` endpoint (no integration test)
- `detect_indicators()` — no unit tests
- `calculate_heuristic_score()` — no unit tests
- `calculate_confidence()` — no unit tests
- `build_attack_techniques()` — no unit tests
- `analyze_urls()` / `get_comparable_domain()` — no unit tests
- `build_graph_data()` — no unit tests
- Error/exception paths in the main endpoint
- LLM fallback behavior when Ollama is unavailable
- Chroma initialization failure path
- No `pytest`-based test suite exists at all

---

## 14. RISKS / PROBLEMS

### 🔴 Critical

| # | Issue | Location | Detail |
|---|---|---|---|
| 1 | **Missing `langchain_ollama` package** | `main.py:10`, `llm_service.py:4` | The package is imported but not installed. The service cannot start. |
| 2 | **No `requirements.txt`** | Root of `ai-service/` | Cannot reproduce the environment. No pinned dependency manifest. |
| 3 | **`test_llm.py` calls non-existent function** | `test_llm.py:1` | Imports `analyze_with_llama` which does not exist — `LLMService.analyze()` is the correct method |
| 4 | **RDAP live network call on every request** | `whois_service.py:105` | No caching, no rate limiting. External dependency on `rdap.org`. Will add ~300–3000ms latency per request |

### 🟠 High

| # | Issue | Location | Detail |
|---|---|---|---|
| 5 | **Hardcoded model names** | `main.py:37,39`, `llm_service.py:14` | LLM and embedding model names are hardcoded in two separate places; inconsistency risk |
| 6 | **No request timeout on `LLMService`** | `llm_service.py:260` | `self.llm.invoke()` has no timeout configured. A slow or hung Ollama will block a FastAPI thread indefinitely |
| 7 | **RDAP `timeout=10`** | `whois_service.py:107` | 10 seconds is generous but the caller (main endpoint) has no overall request timeout |
| 8 | **`rag_service.py` is dead code** | `services/rag_service.py` | Completely reimplemented inline in `main.py`. The module also initializes `OllamaEmbeddings` at import time — if imported it would duplicate the Ollama connection |
| 9 | **`AA26-237A.txt` is 0 bytes** | `data/cisa/AA26-237A.txt` | The ingestion script will skip it with a warning. Only one CISA advisory is actually loaded |
| 10 | **No input validation / sanitization** | `main.py:88–99` | All fields are optional with no max-length constraint. A very large email body will be sent directly to the LLM |

### 🟡 Medium

| # | Issue | Location | Detail |
|---|---|---|---|
| 11 | **Duplicate debug `print` statements** | `llm_service.py:250–348` | Heavy stdout logging in production code. The same "SENDING EVIDENCE TO LLAMA" banner is printed twice (once in `main.py:1572`, once in `llm_service.py:252`) |
| 12 | **`graphs` field adds latency without clear use** | `main.py:1561` | Node/edge graph data is computed for every request but is not currently rendered by the frontend |
| 13 | **Attribution always `"Cannot be determined"`** | `main.py:1161` | Hardcoded string in `build_origin_analysis()` — never dynamic |
| 14 | **`iocs.ipAddresses` always empty** | `main.py:1034` | Hardcoded `"ipAddresses": []` — no IP extraction from the body or URLs |
| 15 | **Confidence capped at 95 but base is 50** | `main.py:759` | Starting at 50 is high for an email with zero indicators. Can return 50% confidence for completely benign emails |
| 16 | **Ollama base URL not configurable** | All Ollama classes | If Ollama runs on a different host (Docker, remote), there is no environment variable to change it |
| 17 | **`rag_service.py` initializes at import** | `rag_service.py:19,28` | `OllamaEmbeddings` and `Chroma` are instantiated at module-level, meaning importing the module tries to contact Ollama immediately — this will fail if Ollama is down |

### 🔵 Low

| # | Issue | Location | Detail |
|---|---|---|---|
| 18 | **Chroma dir is relative path** | `main.py:31` | `./chroma_db` is relative to the working directory. Depends on where the process is launched from |
| 19 | **`CISA_COLLECTION_NAME` defined as plain string, not constant** | `main.py:35`, `rag_service.py:12`, `ingestion/...` | Three separate definitions — drift risk |
| 20 | **No HTTP/CORS configuration** | `main.py:20–23` | No CORS middleware configured — technically any origin can call this. Acceptable for a localhost service, but worth noting |

---

## 15. RECOMMENDED NEXT STEPS

> [!IMPORTANT]
> Do not implement anything from this section — these are recommendations only, for planning purposes.

### A. Run AI Service Locally with Ollama

1. Create `ai-service/requirements.txt` by running `pip freeze` in the active venv
2. Install missing `langchain-ollama`: `pip install langchain-ollama`
3. Install Ollama, run `ollama serve`
4. Pull models: `ollama pull llama3.2:3b && ollama pull nomic-embed-text`
5. Run ingestion: `python ingestion/cisa_ingestion.py`
6. Start service: `python main.py`

### B. Connect Spring Boot to It

Integration is already wired — `AiServiceClient`, DTOs, and `EmailCaseService` call are all present.  
Only requires the AI service to be reachable at `http://localhost:8000`.  
Set `ai.service.url` property if running on a non-default port.

### C. Generate a Better Investigation Summary

Currently the LLM only sees `subject`, `bodyText`, `urls`, and `senderDomain`. To get a more forensically complete summary, Spring Boot should expand `AiAnalysisRequest` to pass:
- SPF / DKIM / DMARC status
- Originating IP + AbuseIPDB score + GeoIP country
- VirusTotal URL verdicts
- Spring-computed threat score

This lets the LLM narrate all of Spring Boot's enrichment in the summary.

### D. Include AI Summary in Forensic PDF

The fields most ready for immediate PDF inclusion are:
- `summary` (primary narrative paragraph)
- `reasoning` (bullet point evidence)
- `verdict` + `riskLevel` + `threatScore` (header badge)
- `indicators` (findings list)
- `attackTechniques` with MITRE IDs (technical annex)
- `iocs.domains` + `iocs.urls` (IOC table)
- `whoisAnalysis` (domain intelligence section)

No additional AI output format changes are needed to support PDF generation — the existing fields are sufficient.

---

## === AI SERVICE AUDIT SUMMARY ===

| | |
|---|---|
| **Technology** | Python 3.14 · FastAPI 0.141.1 · Uvicorn · ChromaDB 1.5.9 · LangChain stack |
| **Entry point** | `python main.py` → Uvicorn on `0.0.0.0:8000` |
| **Current API** | `GET /` (health), `POST /api/v1/analyze` (full forensic pipeline) |
| **Current AI provider/model** | Ollama (local) · LLM: `llama3.2:3b` · Embeddings: `nomic-embed-text` |
| **Ollama status** | **Integrated in code, NOT installed in venv.** `langchain_ollama` package is missing from site-packages. Requires Ollama running locally on default port. |
| **Expected input** | `subject`, `bodyText`, `urls[]`, `senderDomain` (4 fields only — minimal subset of what Spring Boot has) |
| **Current output** | 14-field JSON: threatScore, riskLevel, verdict, confidence, indicators, attackTechniques, iocs, originAnalysis, evidence, reasoning, summary, ragInsights, whoisAnalysis, urlAnalysis, graphs |
| **Spring Boot integration status** | **Architecturally complete, but blocked.** `AiServiceClient.java`, DTOs, and `EmailCaseService` call are all implemented. Service cannot start due to missing `langchain_ollama`. |
| **PDF integration readiness** | **`summary` + `reasoning` + `verdict` + `threatScore` + `indicators` are ready for PDF.** IP intelligence, SPF/DKIM/DMARC, and VirusTotal verdicts are absent from the AI summary because they are not passed to the AI service. |
| **Critical issues** | 1) `langchain_ollama` not installed 2) No `requirements.txt` 3) `test_llm.py` broken (wrong function name) 4) Live RDAP on every request, no cache 5) No LLM timeout 6) `AA26-237A.txt` is empty |
| **Recommended next feature** | Install `langchain_ollama`, create `requirements.txt`, expand `AiAnalysisRequest` to include SPF/DKIM/DMARC + IP intelligence so the LLM summary covers the full forensic picture |
