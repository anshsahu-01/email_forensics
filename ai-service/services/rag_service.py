from typing import List, Dict, Any

from langchain_chroma import Chroma
from langchain_ollama import OllamaEmbeddings


# -----------------------------------
# CONFIGURATION
# -----------------------------------

CHROMA_DB_DIR = "./chroma_db"
COLLECTION_NAME = "cisa_threat_intelligence"


# -----------------------------------
# EMBEDDING MODEL
# -----------------------------------

embeddings = OllamaEmbeddings(
    model="nomic-embed-text"
)


# -----------------------------------
# CHROMA VECTOR DATABASE
# -----------------------------------

vector_db = Chroma(
    collection_name=COLLECTION_NAME,
    persist_directory=CHROMA_DB_DIR,
    embedding_function=embeddings
)


# -----------------------------------
# BUILD EMAIL INVESTIGATION QUERY
# -----------------------------------

def build_email_query(
    subject: str = "",
    body_text: str = "",
    urls: List[str] | None = None,
    sender_domain: str = ""
) -> str:

    urls = urls or []

    query_parts = [
        f"Email subject: {subject}",
        f"Sender domain: {sender_domain}",
        f"Email body: {body_text}",
        f"Extracted URLs: {' '.join(urls)}"
    ]

    return "\n".join(query_parts)


# -----------------------------------
# RETRIEVE THREAT INTELLIGENCE
# -----------------------------------

def retrieve_threat_intelligence(
    query: str,
    k: int = 3
) -> List[Dict[str, Any]]:

    if not query or not query.strip():
        return []

    results = vector_db.similarity_search(
        query,
        k=k
    )

    evidence = []

    for document in results:

        evidence.append({
            "content": document.page_content,
            "source": document.metadata.get(
                "source",
                "Unknown"
            ),
            "filename": document.metadata.get(
                "filename",
                "Unknown"
            )
        })

    return evidence


# -----------------------------------
# BUILD RAG CONTEXT
# -----------------------------------

def build_rag_context(
    subject: str = "",
    body_text: str = "",
    urls: List[str] | None = None,
    sender_domain: str = "",
    k: int = 3
) -> Dict[str, Any]:

    query = build_email_query(
        subject=subject,
        body_text=body_text,
        urls=urls,
        sender_domain=sender_domain
    )

    evidence = retrieve_threat_intelligence(
        query=query,
        k=k
    )

    context_parts = []

    for index, item in enumerate(evidence, start=1):

        context_parts.append(
            f"""
[EVIDENCE {index}]
Source: {item["source"]}
File: {item["filename"]}

{item["content"]}
""".strip()
        )

    context = "\n\n---\n\n".join(context_parts)

    if not context:
        context = (
            "No relevant threat-intelligence evidence "
            "was found in the knowledge base."
        )

    return {
        "query": query,
        "evidence": evidence,
        "context": context
    }