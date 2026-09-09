import requests
from datetime import datetime, timezone
from typing import Any, Dict, Optional


def calculate_domain_age(created_date: Optional[str]) -> Optional[int]:
    """
    Calculate domain age in days from an ISO/RDAP creation date.
    """

    if not created_date:
        return None

    try:
        created = datetime.fromisoformat(
            created_date.replace("Z", "+00:00")
        )

        now = datetime.now(timezone.utc)

        return max(0, (now - created).days)

    except (ValueError, TypeError):
        return None


def extract_event(
    data: Dict[str, Any],
    event_name: str
) -> Optional[str]:
    """
    Extract a specific event date from RDAP response.
    """

    for event in data.get("events", []):

        if event.get("eventAction") == event_name:
            return event.get("eventDate")

    return None


def extract_registrar(data: Dict[str, Any]) -> str:
    """
    Extract registrar name from RDAP entities.
    """

    for entity in data.get("entities", []):

        if "registrar" not in entity.get("roles", []):
            continue

        vcard_array = entity.get("vcardArray", [])

        if len(vcard_array) < 2:
            continue

        for item in vcard_array[1]:

            if len(item) >= 4 and item[0] == "fn":
                return str(item[3])

    return ""


def normalize_domain(domain: str) -> str:
    """
    Normalize a domain before RDAP lookup.
    """

    domain = (domain or "").strip().lower()

    domain = domain.replace("https://", "")
    domain = domain.replace("http://", "")

    domain = domain.split("/")[0]
    domain = domain.split("?")[0]
    domain = domain.split("#")[0]

    return domain.strip()


def lookup_domain(domain: str) -> Dict[str, Any]:
    """
    Perform a live RDAP lookup for a domain.

    The returned field names intentionally use camelCase
    because they match the API response structure used
    by the AI service.
    """

    domain = normalize_domain(domain)

    if not domain:
        return {
            "success": False,
            "domain": "",
            "error": "No domain provided."
        }

    url = f"https://rdap.org/domain/{domain}"

    try:

        response = requests.get(
            url,
            timeout=10,
            headers={
                "Accept": "application/rdap+json"
            }
        )

        if response.status_code != 200:

            return {
                "success": False,
                "domain": domain,
                "statusCode": response.status_code,
                "error": "RDAP lookup failed."
            }

        data = response.json()

        created_date = extract_event(
            data,
            "registration"
        )

        updated_date = extract_event(
            data,
            "last changed"
        )

        expiration_date = extract_event(
            data,
            "expiration"
        )

        registrar = extract_registrar(data)

        domain_age_days = calculate_domain_age(
            created_date
        )

        nameservers = [
            ns.get("ldhName")
            for ns in data.get("nameservers", [])
            if ns.get("ldhName")
        ]

        return {
            "success": True,
            "domain": domain,

            "registrar": registrar,

            "createdDate": created_date,
            "updatedDate": updated_date,
            "expirationDate": expiration_date,

            # IMPORTANT:
            # Keep this name consistent with main.py.
            "domainAgeDays": domain_age_days,

            "nameservers": nameservers,

            "status": data.get("status", []),

            "rdapUrl": url
        }

    except requests.RequestException as exc:

        return {
            "success": False,
            "domain": domain,
            "error": f"RDAP request failed: {str(exc)}"
        }

    except ValueError as exc:

        return {
            "success": False,
            "domain": domain,
            "error": f"Invalid RDAP response: {str(exc)}"
        }