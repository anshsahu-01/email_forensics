from services.rag_service import build_rag_context


# -----------------------------------
# SIMULATED EMAIL
# -----------------------------------

email = {
    "subject": "Urgent Zimbra Account Verification",
    
    "bodyText": """
    Your Zimbra account requires immediate verification.
    Please verify your account to prevent suspension.
    """,

    "urls": [
        "https://example.com/verify"
    ],

    "senderDomain": "example.com"
}


# -----------------------------------
# RAG
# -----------------------------------

result = build_rag_context(
    subject=email["subject"],
    body_text=email["bodyText"],
    urls=email["urls"],
    sender_domain=email["senderDomain"],
    k=3
)


# -----------------------------------
# DISPLAY
# -----------------------------------

print("\n==============================")
print("EMAIL-AWARE RAG TEST")
print("==============================")

print("\nINVESTIGATION QUERY:")
print(result["query"])

print("\n==============================")
print("RETRIEVED EVIDENCE")
print("==============================")

for i, evidence in enumerate(
    result["evidence"],
    start=1
):

    print(f"\n--- Evidence {i} ---")

    print("Source:", evidence["source"])
    print("File:", evidence["filename"])

    print("\nContent:")
    print(evidence["content"][:700])


print("\n==============================")
print("FINAL RAG CONTEXT")
print("==============================")

print(result["context"][:2500])