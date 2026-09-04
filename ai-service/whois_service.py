from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from datetime import datetime
import whois
import Levenshtein

app = FastAPI(title="WHOIS Forensic Microservice")

TARGET_BRANDS = ["paypal.com", "microsoft.com", "google.com", "apple.com"]

class DomainRequest(BaseModel):
    domain: str

@app.post("/api/v1/forensics/whois")
def analyze_domain(payload: DomainRequest):
    domain_name = payload.domain.lower().replace("www.", "")
    
    # 1. Typosquatting / Brand impersonation check
    best_match, max_sim = None, 0.0
    for brand in TARGET_BRANDS:
        sim = Levenshtein.ratio(domain_name, brand)
        if 0.70 <= sim < 1.0 and sim > max_sim:
            max_sim = sim
            best_match = brand

    # 2. WHOIS lookup with fallback for unregistered domains
    is_registered = True
    try:
        w = whois.whois(domain_name)
        if not w.domain_name and not w.registrar:
            is_registered = False
            creation_date, registrar, org = None, "Unregistered", "Unregistered"
        else:
            creation_date = w.creation_date[0] if isinstance(w.creation_date, list) else w.creation_date
            registrar = w.registrar or "Unknown"
            org = w.org or "Privacy Redacted"
    except Exception:
        is_registered = False
        creation_date = None
        registrar = "Unregistered / Lookup Failed"
        org = "Unregistered"

    domain_age_days = (datetime.now() - creation_date).days if (creation_date and isinstance(creation_date, datetime)) else None

    return {
        "domain": domain_name,
        "isRegistered": is_registered,
        "registrar": registrar,
        "organization": org,
        "registeredOn": str(creation_date) if creation_date else "N/A",
        "domainAgeDays": domain_age_days,
        "isNewDomain": domain_age_days < 30 if domain_age_days is not None else False,
        "isLookalike": best_match is not None,
        "matchedBrand": best_match or "None",
        "lookalikeSimilarity": round(max_sim * 100, 2)
    }