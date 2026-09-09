from services.whois_service import lookup_domain


result = lookup_domain("google.com")

print("\n==============================")
print("WHOIS / RDAP TEST")
print("==============================")

print(result)