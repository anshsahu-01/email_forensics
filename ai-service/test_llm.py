from services.llm_service import analyze_with_llama


result = analyze_with_llama(
    subject="Urgent Zimbra Account Verification",
    body_text="""
    Your Zimbra account requires immediate verification.
    Please verify your account to prevent suspension.
    """,
    urls=[
        "https://example.com/verify"
    ],
    sender_domain="example.com",
    rag_context="""
    CISA reports phishing campaigns targeting Zimbra users.
    Similar campaigns may use credential theft and suspicious
    verification requests.
    """
)

print("\n==============================")
print("LLAMA FORENSIC ANALYSIS")
print("==============================")

print(result)