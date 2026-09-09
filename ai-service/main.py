import os
import json
from typing import List, Optional
from urllib.parse import urlparse

from fastapi import FastAPI
from pydantic import BaseModel, Field

from langchain_chroma import Chroma
from langchain_ollama import OllamaEmbeddings

from services.llm_service import LLMService
from services.whois_service import lookup_domain


# ============================================================
# FASTAPI APP
# ============================================================

app = FastAPI(
    title="Email Forensics AI Service",
    version="5.0.1"
)


# ============================================================
# CONFIGURATION
# ============================================================

CHROMA_DB_DIR = os.getenv(
    "CHROMA_DB_DIR",
    "./chroma_db"
)

CISA_COLLECTION_NAME = "cisa_threat_intelligence"

EMBEDDING_MODEL = "nomic-embed-text"

LLM_MODEL = "llama3.2:3b"


# ============================================================
# OLLAMA EMBEDDINGS
# ============================================================

embeddings = OllamaEmbeddings(
    model=EMBEDDING_MODEL
)


# ============================================================
# LLM SERVICE
# ============================================================

llm_service = LLMService()


# ============================================================
# CHROMA / CISA KNOWLEDGE BASE
# ============================================================

try:

    vector_db = Chroma(
        collection_name=CISA_COLLECTION_NAME,
        persist_directory=CHROMA_DB_DIR,
        embedding_function=embeddings
    )

    print(
        f"[RAG] Connected to Chroma collection: "
        f"{CISA_COLLECTION_NAME}"
    )

except Exception as e:

    print(
        f"[RAG] Failed to initialize Chroma: {e}"
    )

    vector_db = None


# ============================================================
# REQUEST MODEL
# ============================================================

class EmailAnalysisRequest(BaseModel):

    subject: Optional[str] = ""

    bodyText: Optional[str] = ""

    urls: Optional[List[str]] = Field(
        default_factory=list
    )

    senderDomain: Optional[str] = ""


# ============================================================
# RESPONSE MODEL
# ============================================================

class EmailAnalysisResponse(BaseModel):

    threatScore: float

    riskLevel: str

    verdict: str

    confidence: float

    indicators: List[str]

    attackTechniques: List[dict]

    iocs: dict

    originAnalysis: dict

    evidence: List[dict]

    reasoning: List[str]

    summary: str

    ragInsights: str

    whoisAnalysis: dict

    urlAnalysis: dict

    graphs: dict


# ============================================================
# DOMAIN EXTRACTION
# ============================================================

def extract_domain_from_url(
    url: str
) -> str:

    try:

        parsed = urlparse(url)

        hostname = parsed.hostname

        if not hostname:
            return ""

        return hostname.lower().strip(".")

    except Exception:

        return ""


# ============================================================
# ROOT / COMPARABLE DOMAIN
# ============================================================

def get_comparable_domain(
    domain: str
) -> str:

    domain = (
        domain
        .lower()
        .strip()
        .strip(".")
    )

    parts = domain.split(".")

    if len(parts) >= 2:

        return ".".join(parts[-2:])

    return domain


# ============================================================
# URL / DOMAIN ANALYSIS
# ============================================================

def analyze_urls(
    urls: List[str],
    sender_domain: str
) -> dict:

    results = []

    sender_root = get_comparable_domain(
        sender_domain
    )

    for url in urls:

        extracted_domain = extract_domain_from_url(
            url
        )

        url_root = get_comparable_domain(
            extracted_domain
        )

        if not extracted_domain:

            results.append({
                "url": url,
                "domain": "",
                "validDomain": False,
                "matchesSenderDomain": False
            })

            continue

        matches_sender = (
            bool(sender_root)
            and bool(url_root)
            and sender_root == url_root
        )

        results.append({
            "url": url,
            "domain": extracted_domain,
            "validDomain": True,
            "matchesSenderDomain": matches_sender
        })

    mismatches = [
        item
        for item in results
        if item["validDomain"]
        and not item["matchesSenderDomain"]
    ]

    return {
        "urlsAnalyzed": len(urls),
        "results": results,
        "senderDomain": sender_domain,
        "domainMismatchDetected": len(mismatches) > 0
    }


# ============================================================
# INDICATOR DETECTION
# ============================================================

def detect_indicators(
    subject: str,
    body: str,
    urls: List[str],
    sender_domain: str,
    url_analysis: dict
) -> List[str]:

    indicators = []

    text = f"{subject} {body}".lower()

    # --------------------------------------------------------
    # URGENCY
    # --------------------------------------------------------

    urgency_keywords = [
        "urgent",
        "immediately",
        "immediate action",
        "act now",
        "as soon as possible",
        "within 24 hours",
        "account suspension",
        "account suspended",
        "final warning"
    ]

    for keyword in urgency_keywords:

        if keyword in text:

            indicators.append(
                f"Urgency or pressure language detected: "
                f"'{keyword}'"
            )

    # --------------------------------------------------------
    # CREDENTIAL REQUEST
    # --------------------------------------------------------

    credential_keywords = [
        "verify your password",
        "enter your password",
        "confirm your password",
        "password reset",
        "verify your account",
        "account verification",
        "login",
        "sign in",
        "credentials",
        "username and password"
    ]

    for keyword in credential_keywords:

        if keyword in text:

            indicators.append(
                f"Potential credential/account request detected: "
                f"'{keyword}'"
            )

    # --------------------------------------------------------
    # FINANCIAL REQUEST
    # --------------------------------------------------------

    financial_keywords = [
        "wire transfer",
        "bank transfer",
        "payment required",
        "invoice",
        "bank account",
        "credit card",
        "payment"
    ]

    for keyword in financial_keywords:

        if keyword in text:

            indicators.append(
                f"Potential financial request detected: "
                f"'{keyword}'"
            )

    # --------------------------------------------------------
    # URL ANALYSIS
    # --------------------------------------------------------

    suspicious_url_keywords = [
        "login",
        "verify",
        "password",
        "account",
        "secure",
        "signin"
    ]

    for url in urls:

        url_lower = url.lower()

        if "@" in url_lower:

            indicators.append(
                f"Suspicious URL structure detected: "
                f"'{url}'"
            )

        for keyword in suspicious_url_keywords:

            if keyword in url_lower:

                indicators.append(
                    f"Suspicious URL keyword detected: "
                    f"'{keyword}'"
                )

                break

    # --------------------------------------------------------
    # URL / SENDER DOMAIN MISMATCH
    # --------------------------------------------------------

    if url_analysis.get(
        "domainMismatchDetected",
        False
    ):

        indicators.append(
            "URL domain does not match the sender domain"
        )

    # --------------------------------------------------------
    # BRAND IMPERSONATION
    # --------------------------------------------------------

    suspicious_tlds = [
        ".xyz",
        ".top",
        ".click",
        ".link",
        ".work",
        ".support",
        ".online"
    ]

    common_brands = [
        "microsoft",
        "google",
        "apple",
        "amazon",
        "paypal",
        "zimbra",
        "office365",
        "microsoft365"
    ]

    domain_lower = sender_domain.lower()

    for brand in common_brands:

        if brand in domain_lower:

            for tld in suspicious_tlds:

                if domain_lower.endswith(tld):

                    indicators.append(
                        f"Possible brand impersonation detected "
                        f"for '{brand}' from domain "
                        f"'{sender_domain}'"
                    )

                    break

    return indicators


# ============================================================
# OBJECTIVE HEURISTIC SCORING
# ============================================================

def calculate_heuristic_score(
    subject: str,
    body: str,
    urls: List[str],
    sender_domain: str,
    url_analysis: dict
) -> float:

    text = f"{subject} {body}".lower()

    score = 10

    urgency_keywords = [
        "urgent",
        "immediately",
        "immediate action",
        "act now",
        "as soon as possible",
        "within 24 hours",
        "account suspension",
        "account suspended",
        "final warning"
    ]

    for keyword in urgency_keywords:

        if keyword in text:

            score += 10

    credential_keywords = [
        "verify your password",
        "enter your password",
        "confirm your password",
        "password reset",
        "verify your account",
        "account verification",
        "login",
        "sign in",
        "credentials",
        "username and password"
    ]

    for keyword in credential_keywords:

        if keyword in text:

            score += 15

    financial_keywords = [
        "wire transfer",
        "bank transfer",
        "payment required",
        "invoice",
        "bank account",
        "credit card",
        "payment"
    ]

    for keyword in financial_keywords:

        if keyword in text:

            score += 15

    if urls:

        score += min(
            len(urls) * 5,
            20
        )

    for url in urls:

        if "@" in url.lower():

            score += 15

    suspicious_url_keywords = [
        "login",
        "verify",
        "password",
        "account",
        "secure",
        "signin"
    ]

    for url in urls:

        url_lower = url.lower()

        if any(
            keyword in url_lower
            for keyword in suspicious_url_keywords
        ):

            score += 10

    if url_analysis.get(
        "domainMismatchDetected",
        False
    ):

        score += 15

    suspicious_tlds = [
        ".xyz",
        ".top",
        ".click",
        ".link",
        ".work",
        ".support",
        ".online"
    ]

    common_brands = [
        "microsoft",
        "google",
        "apple",
        "amazon",
        "paypal",
        "zimbra",
        "office365",
        "microsoft365"
    ]

    domain_lower = sender_domain.lower()

    for brand in common_brands:

        if brand in domain_lower:

            if any(
                domain_lower.endswith(tld)
                for tld in suspicious_tlds
            ):

                score += 25

                break

    return float(
        min(score, 100)
    )


# ============================================================
# WHOIS / RDAP
# ============================================================

def retrieve_whois_context(
    sender_domain: str
):

    if not sender_domain:

        return (
            {},
            "No sender domain provided."
        )

    try:

        whois_data = lookup_domain(
            sender_domain
        )

        if not whois_data:

            return (
                {},
                "WHOIS/RDAP lookup returned no data."
            )

        domain_age = whois_data.get(
            "domainAgeDays"
        )

        if domain_age is not None:

            if domain_age < 30:

                insight = (
                    f"Domain is very young "
                    f"({domain_age} days old). "
                    "This is a supporting risk indicator only."
                )

            elif domain_age < 90:

                insight = (
                    f"Domain is relatively young "
                    f"({domain_age} days old). "
                    "This is a supporting risk indicator only."
                )

            else:

                insight = (
                    f"Domain is {domain_age} days old. "
                    "No additional age-based risk signal applied."
                )

        else:

            insight = (
                "Domain age could not be determined."
            )

        return (
            whois_data,
            insight
        )

    except Exception as e:

        print(
            f"[WHOIS] Lookup error: {e}"
        )

        return (
            {},
            f"WHOIS/RDAP lookup failed: {str(e)}"
        )


# ============================================================
# WHOIS SUPPORTING SCORE
# ============================================================

def calculate_whois_score(
    whois_data: dict
) -> float:

    if not whois_data:

        return 0.0

    domain_age = whois_data.get(
        "domainAgeDays"
    )

    if domain_age is None:

        return 0.0

    if domain_age < 30:

        return 10.0

    if domain_age < 90:

        return 5.0

    return 0.0


# ============================================================
# FINAL SCORE
# ============================================================

def calculate_final_score(
    heuristic_score: float,
    whois_score: float
) -> float:

    return min(
        heuristic_score + whois_score,
        100.0
    )


# ============================================================
# RISK
# ============================================================

def determine_risk_level(
    score: float
) -> str:

    if score >= 70:

        return "HIGH_RISK"

    if score >= 35:

        return "MEDIUM_RISK"

    return "LOW_RISK"


# ============================================================
# VERDICT
# ============================================================

def determine_verdict(
    score: float
) -> str:

    if score >= 70:

        return "MALICIOUS"

    if score >= 35:

        return "SUSPICIOUS"

    return "LEGITIMATE"


# ============================================================
# CONFIDENCE
# ============================================================

def calculate_confidence(
    score: float,
    indicators: List[str],
    whois_data: dict,
    rag_context: str
) -> float:

    confidence = 50.0

    indicator_count = len(indicators)

    if indicator_count >= 1:

        confidence += 5

    if indicator_count >= 3:

        confidence += 5

    if indicator_count >= 5:

        confidence += 5

    if indicator_count >= 8:

        confidence += 5

    if whois_data:

        confidence += 5

    if rag_context:

        confidence += 5

    if score >= 70:

        confidence += 5

    elif score >= 35:

        confidence += 2

    return float(
        min(confidence, 95.0)
    )


# ============================================================
# RAG QUERY
# ============================================================

def retrieve_rag_context(
    subject: str,
    body: str,
    urls: List[str],
    sender_domain: str
):

    if vector_db is None:

        return (
            "",
            "RAG unavailable."
        )

    query_parts = []

    if subject:

        query_parts.append(
            f"Subject: {subject}"
        )

    if sender_domain:

        query_parts.append(
            f"Sender domain: {sender_domain}"
        )

    if urls:

        query_parts.append(
            f"URLs: {' '.join(urls)}"
        )

    if body:

        query_parts.append(
            f"Email body: {body}"
        )

    query = "\n".join(
        query_parts
    )

    try:

        results = vector_db.similarity_search(
            query,
            k=3
        )

        if not results:

            return (
                "",
                "No historical vector match found."
            )

        contexts = []

        sources = []

        for document in results:

            contexts.append(
                document.page_content
            )

            source = document.metadata.get(
                "source",
                "CISA knowledge base"
            )

            sources.append(
                source
            )

        combined_context = (
            "\n\n---\n\n".join(
                contexts
            )
        )

        unique_sources = list(
            dict.fromkeys(sources)
        )

        rag_insight = (
            f"Retrieved {len(results)} relevant "
            f"historical threat intelligence records "
            f"from the CISA knowledge base. "
            f"Sources: {', '.join(unique_sources)}"
        )

        return (
            combined_context,
            rag_insight
        )

    except Exception as e:

        print(
            f"[RAG] Retrieval error: {e}"
        )

        return (
            "",
            f"RAG retrieval failed: {str(e)}"
        )


# ============================================================
# ATTACK TECHNIQUE EXTRACTION
# ============================================================

def build_attack_techniques(
    indicators: List[str],
    subject: str,
    body: str,
    urls: List[str]
) -> List[dict]:

    text = f"{subject} {body}".lower()

    techniques = []

    if any(
        keyword in text
        for keyword in [
            "urgent",
            "immediately",
            "account suspension",
            "final warning"
        ]
    ):

        techniques.append({
            "technique": "Phishing / Social Engineering",
            "mitreId": "T1566",
            "evidence": (
                "Urgency or account-pressure language detected"
            ),
            "confidence": 0.90
        })

    if any(
        keyword in text
        for keyword in [
            "password",
            "credentials",
            "username and password",
            "sign in",
            "login"
        ]
    ):

        techniques.append({
            "technique": (
                "Credential Access / Phishing "
                "Credential Request"
            ),
            "mitreId": "T1056.002",
            "evidence": (
                "Credential or password collection "
                "language detected"
            ),
            "confidence": 0.80
        })

    if urls:

        techniques.append({
            "technique": "Phishing Link",
            "mitreId": "T1566.002",
            "evidence": "URL supplied in the email",
            "confidence": 0.80
        })

    if any(
        "domain does not match" in indicator.lower()
        for indicator in indicators
    ):

        techniques.append({
            "technique": (
                "Spearphishing Link / Domain Mismatch"
            ),
            "mitreId": "T1566.002",
            "evidence": (
                "URL domain differs from sender domain"
            ),
            "confidence": 0.90
        })

    return techniques


# ============================================================
# IOC EXTRACTION
# ============================================================

def extract_iocs(
    sender_domain: str,
    urls: List[str],
    body: str
) -> dict:

    domains = []

    if sender_domain:

        domains.append(
            sender_domain
        )

    for url in urls:

        domain = extract_domain_from_url(
            url
        )

        if domain and domain not in domains:

            domains.append(
                domain
            )

    return {
        "domains": domains,
        "urls": urls,
        "ipAddresses": [],
        "emailAddresses": [],
        "hashes": [],
        "other": []
    }


# ============================================================
# EVIDENCE BUILDING
# ============================================================

def build_evidence(
    indicators: List[str],
    url_analysis: dict,
    whois_data: dict,
    rag_insights: str
) -> List[dict]:

    evidence = []

    for indicator in indicators:

        evidence.append({
            "type": "EMAIL_INDICATOR",
            "source": "Submitted email",
            "finding": indicator,
            "directEvidence": True
        })

    if url_analysis.get(
        "urlsAnalyzed",
        0
    ) > 0:

        evidence.append({
            "type": "URL_ANALYSIS",
            "source": "URL parser",
            "finding": url_analysis,
            "directEvidence": True
        })

    if whois_data:

        evidence.append({
            "type": "WHOIS_RDAP",
            "source": "RDAP",
            "finding": whois_data,
            "directEvidence": True
        })

    if rag_insights:

        evidence.append({
            "type": "THREAT_INTELLIGENCE",
            "source": "CISA knowledge base",
            "finding": rag_insights,
            "directEvidence": False,
            "contextOnly": True
        })

    return evidence


# ============================================================
# ORIGIN ANALYSIS
# ============================================================

def build_origin_analysis(
    sender_domain: str,
    whois_data: dict,
    url_analysis: dict
) -> dict:

    domain_age = whois_data.get(
        "domainAgeDays"
    )

    if domain_age is None:

        age_assessment = (
            "Domain age unavailable"
        )

    elif domain_age < 30:

        age_assessment = (
            "Very recently registered domain"
        )

    elif domain_age < 90:

        age_assessment = (
            "Relatively recently registered domain"
        )

    else:

        age_assessment = (
            "Long-established domain"
        )

    mismatch = url_analysis.get(
        "domainMismatchDetected",
        False
    )

    return {

        "senderDomain": sender_domain,

        "domainAgeAssessment": age_assessment,

        "urlSenderRelationship": (
            "Mismatch detected"
            if mismatch
            else "No mismatch detected"
        ),

        "registrar": whois_data.get(
            "registrar"
        ),

        "nameservers": whois_data.get(
            "nameservers",
            []
        ),

        "attribution": (
            "Cannot be determined from available evidence"
        )
    }


# ============================================================
# GRAPH DATA
# ============================================================

def build_graph_data(
    sender_domain: str,
    urls: List[str],
    whois_data: dict,
    indicators: List[str]
) -> dict:

    nodes = []

    edges = []

    # --------------------------------------------------------
    # EMAIL
    # --------------------------------------------------------

    nodes.append({
        "id": "email",
        "type": "email",
        "label": "Analyzed Email"
    })

    # --------------------------------------------------------
    # SENDER DOMAIN
    # --------------------------------------------------------

    if sender_domain:

        nodes.append({
            "id": f"domain:{sender_domain}",
            "type": "domain",
            "label": sender_domain
        })

        edges.append({
            "source": "email",
            "target": f"domain:{sender_domain}",
            "relationship": "sent-from"
        })

    # --------------------------------------------------------
    # URLS
    # --------------------------------------------------------

    for index, url in enumerate(urls):

        node_id = f"url:{index}"

        nodes.append({
            "id": node_id,
            "type": "url",
            "label": url
        })

        edges.append({
            "source": "email",
            "target": node_id,
            "relationship": "contains-link"
        })

        domain = extract_domain_from_url(
            url
        )

        if domain:

            if (
                sender_domain
                and get_comparable_domain(domain)
                == get_comparable_domain(sender_domain)
            ):

                domain_id = f"domain:{sender_domain}"

            else:

                domain_id = f"url-domain:{domain}"

            existing = any(
                node["id"] == domain_id
                for node in nodes
            )

            if not existing:

                nodes.append({
                    "id": domain_id,
                    "type": "domain",
                    "label": domain
                })

            edges.append({
                "source": node_id,
                "target": domain_id,
                "relationship": "resolves-to"
            })

    # --------------------------------------------------------
    # WHOIS
    # --------------------------------------------------------

    if whois_data and sender_domain:

        registrar = whois_data.get(
            "registrar"
        )

        if registrar:

            registrar_id = (
                f"registrar:{registrar}"
            )

            existing = any(
                node["id"] == registrar_id
                for node in nodes
            )

            if not existing:

                nodes.append({
                    "id": registrar_id,
                    "type": "registrar",
                    "label": registrar
                })

            edges.append({
                "source": f"domain:{sender_domain}",
                "target": registrar_id,
                "relationship": "registered-with"
            })

    # --------------------------------------------------------
    # INDICATORS
    # --------------------------------------------------------

    for index, indicator in enumerate(
        indicators
    ):

        indicator_id = f"indicator:{index}"

        nodes.append({
            "id": indicator_id,
            "type": "indicator",
            "label": indicator
        })

        edges.append({
            "source": "email",
            "target": indicator_id,
            "relationship": "has-indicator"
        })

    return {
        "nodes": nodes,
        "edges": edges
    }


# ============================================================
# ANALYZE ENDPOINT
# ============================================================

@app.post(
    "/api/v1/analyze",
    response_model=EmailAnalysisResponse
)
def analyze_email(
    request: EmailAnalysisRequest
):

    # ========================================================
    # DEBUG: CONFIRM SPRING REQUEST REACHED FASTAPI
    # ========================================================

    print("\n========================================")
    print("AI SERVICE RECEIVED REQUEST")
    print("========================================")

    print(
        "Subject:",
        request.subject
    )

    print(
        "Body length:",
        len(request.bodyText or "")
    )

    print(
        "URLs:",
        request.urls
    )

    print(
        "Sender domain:",
        request.senderDomain
    )

    print("========================================\n")

    try:

        # ----------------------------------------------------
        # NORMALIZE
        # ----------------------------------------------------

        subject = (
            request.subject or ""
        ).strip()

        body = (
            request.bodyText or ""
        ).strip()

        urls = request.urls or []

        sender_domain = (
            request.senderDomain or ""
        ).strip().lower()

        # ----------------------------------------------------
        # URL ANALYSIS
        # ----------------------------------------------------

        print("[AI] Running URL analysis...")

        url_analysis = analyze_urls(
            urls=urls,
            sender_domain=sender_domain
        )

        # ----------------------------------------------------
        # INDICATORS
        # ----------------------------------------------------

        print("[AI] Detecting indicators...")

        indicators = detect_indicators(
            subject=subject,
            body=body,
            urls=urls,
            sender_domain=sender_domain,
            url_analysis=url_analysis
        )

        # ----------------------------------------------------
        # HEURISTIC
        # ----------------------------------------------------

        print("[AI] Calculating heuristic score...")

        heuristic_score = calculate_heuristic_score(
            subject=subject,
            body=body,
            urls=urls,
            sender_domain=sender_domain,
            url_analysis=url_analysis
        )

        # ----------------------------------------------------
        # WHOIS
        # ----------------------------------------------------

        print("[AI] Running domain intelligence...")

        whois_data, whois_insight = (
            retrieve_whois_context(
                sender_domain
            )
        )

        # ----------------------------------------------------
        # WHOIS SCORE
        # ----------------------------------------------------

        whois_score = calculate_whois_score(
            whois_data
        )

        # ----------------------------------------------------
        # FINAL SCORE
        # ----------------------------------------------------

        threat_score = calculate_final_score(
            heuristic_score=heuristic_score,
            whois_score=whois_score
        )

        print(
            f"[AI] Threat score: {threat_score}"
        )

        # ----------------------------------------------------
        # RISK + VERDICT
        # ----------------------------------------------------

        risk_level = determine_risk_level(
            threat_score
        )

        verdict = determine_verdict(
            threat_score
        )

        print(
            f"[AI] Risk: {risk_level}"
        )

        print(
            f"[AI] Verdict: {verdict}"
        )

        # ----------------------------------------------------
        # RAG
        # ----------------------------------------------------

        print("[AI] Querying CISA RAG...")

        rag_context, rag_insights = (
            retrieve_rag_context(
                subject=subject,
                body=body,
                urls=urls,
                sender_domain=sender_domain
            )
        )

        print(
            f"[AI] RAG context length: "
            f"{len(rag_context)}"
        )

        # ----------------------------------------------------
        # CONFIDENCE
        # ----------------------------------------------------

        confidence = calculate_confidence(
            score=threat_score,
            indicators=indicators,
            whois_data=whois_data,
            rag_context=rag_context
        )

        # ----------------------------------------------------
        # ATTACK TECHNIQUES
        # ----------------------------------------------------

        attack_techniques = build_attack_techniques(
            indicators=indicators,
            subject=subject,
            body=body,
            urls=urls
        )

        # ----------------------------------------------------
        # IOCS
        # ----------------------------------------------------

        iocs = extract_iocs(
            sender_domain=sender_domain,
            urls=urls,
            body=body
        )

        # ----------------------------------------------------
        # EVIDENCE
        # ----------------------------------------------------

        evidence = build_evidence(
            indicators=indicators,
            url_analysis=url_analysis,
            whois_data=whois_data,
            rag_insights=rag_insights
        )

        # ----------------------------------------------------
        # ORIGIN
        # ----------------------------------------------------

        origin_analysis = build_origin_analysis(
            sender_domain=sender_domain,
            whois_data=whois_data,
            url_analysis=url_analysis
        )

        # ----------------------------------------------------
        # GRAPH
        # ----------------------------------------------------

        graphs = build_graph_data(
            sender_domain=sender_domain,
            urls=urls,
            whois_data=whois_data,
            indicators=indicators
        )

        # ----------------------------------------------------
        # LLM ANALYSIS
        # ----------------------------------------------------

        print("\n========================================")
        print("SENDING EVIDENCE TO LLAMA")
        print("========================================")

        print(
            "LLM model:",
            LLM_MODEL
        )

        print(
            "Indicators:",
            len(indicators)
        )

        print(
            "RAG context:",
            len(rag_context)
        )

        print("========================================\n")

        llama_analysis = llm_service.analyze(
            threat_score=threat_score,
            risk_level=risk_level,
            verdict=verdict,
            confidence=confidence,
            subject=subject,
            sender_domain=sender_domain,
            body_text=body,
            detected_indicators=indicators,
            url_analysis=url_analysis,
            whois_analysis=whois_data,
            rag_context=rag_context
        )

        print("\n========================================")
        print("LLAMA ANALYSIS COMPLETE")
        print("========================================")

        print(
            "Summary:",
            llama_analysis.get(
                "summary",
                ""
            )
        )

        print(
            "Reasoning:",
            llama_analysis.get(
                "reasoning",
                []
            )
        )

        print("========================================\n")

        # ----------------------------------------------------
        # FINAL SUMMARY
        # ----------------------------------------------------

        summary = llama_analysis.get(
            "summary",
            (
                f"Email classified as {verdict} "
                f"with threat score {threat_score:.1f}."
            )
        )

        reasoning = llama_analysis.get(
            "reasoning",
            []
        )

        # ----------------------------------------------------
        # RETURN
        # ----------------------------------------------------

        return EmailAnalysisResponse(

            threatScore=threat_score,

            riskLevel=risk_level,

            verdict=verdict,

            confidence=confidence,

            indicators=indicators,

            attackTechniques=attack_techniques,

            iocs=iocs,

            originAnalysis=origin_analysis,

            evidence=evidence,

            reasoning=reasoning,

            summary=summary,

            ragInsights=rag_insights,

            whoisAnalysis=whois_data,

            urlAnalysis=url_analysis,

            graphs=graphs
        )

    except Exception as e:

        print(
            f"[API] Analysis pipeline error: {e}"
        )

        return EmailAnalysisResponse(

            threatScore=0.0,

            riskLevel="UNKNOWN",

            verdict="UNKNOWN",

            confidence=0.0,

            indicators=[],

            attackTechniques=[],

            iocs={
                "domains": [],
                "urls": [],
                "ipAddresses": [],
                "emailAddresses": [],
                "hashes": [],
                "other": []
            },

            originAnalysis={
                "attribution": "Analysis failed"
            },

            evidence=[],

            reasoning=[
                f"Analysis pipeline error: {str(e)}"
            ],

            summary=(
                f"Analysis pipeline error: {str(e)}"
            ),

            ragInsights="Execution failed.",

            whoisAnalysis={},

            urlAnalysis={},

            graphs={
                "nodes": [],
                "edges": []
            }
        )


# ============================================================
# HEALTH CHECK
# ============================================================

@app.get("/")
def health_check():

    return {

        "service":
            "Email Forensics AI Service",

        "status":
            "running",

        "llm":
            LLM_MODEL,

        "embedding_model":
            EMBEDDING_MODEL,

        "rag_collection":
            CISA_COLLECTION_NAME
    }


# ============================================================
# RUN SERVER
# ============================================================

if __name__ == "__main__":

    import uvicorn

    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=True
    )