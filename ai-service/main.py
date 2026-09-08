import os
from typing import List, Optional
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

# Modern LangChain Partner Integrations
from langchain_chroma import Chroma
from langchain_ollama import OllamaEmbeddings, ChatOllama
from langchain_core.prompts import ChatPromptTemplate

# Initialize FastAPI App
app = FastAPI(
    title="Email Threat Forensic AI Engine",
    description="Microservice providing RAG-backed threat analysis, heuristic checks, and narrative summaries.",
    version="1.0.0"
)

# ------------------------------------------------------------------------------
# 1. Initialize Local AI & Vector Database Models
# ------------------------------------------------------------------------------
# Embedding model for vector operations
embeddings = OllamaEmbeddings(model="nomic-embed-text")

# Generative LLM for forensic report synthesis
llm = ChatOllama(model="llama3.1:8b", temperature=0.1)

# ChromaDB Vector Store instance
CHROMA_DB_DIR = os.getenv("CHROMA_DB_DIR", "./chroma_db")

# Initialize ChromaDB with fallback to prevent initialization startup failure
try:
    vector_db = Chroma(persist_directory=CHROMA_DB_DIR, embedding_function=embeddings)
except Exception as e:
    vector_db = None
    print(f"Warning: Failed to initialize ChromaDB at {CHROMA_DB_DIR}: {e}")

# ------------------------------------------------------------------------------
# 2. Input/Output DTO Schema Models (Matches Spring Boot Request Body)
# ------------------------------------------------------------------------------
class EmailAnalysisRequest(BaseModel):
    subject: Optional[str] = ""
    bodyText: Optional[str] = ""
    urls: Optional[List[str]] = []
    senderDomain: Optional[str] = ""

class EmailAnalysisResponse(BaseModel):
    threatScore: float
    riskLevel: str
    summary: str
    ragInsights: str

# ------------------------------------------------------------------------------
# 3. Helper Logic: Heuristic & Lookalike Analysis
# ------------------------------------------------------------------------------
def calculate_heuristic_score(subject: str, body: str, urls: List[str], sender_domain: str) -> float:
    """Calculates a baseline threat score using pattern recognition rules."""
    score = 10.0  # Base safe score
    
    # Check for urgency/coercion keywords
    suspicious_keywords = ["urgent", "wire transfer", "verify account", "password reset", "immediate action", "bank"]
    text_content = f"{subject} {body}".lower()
    
    for kw in suspicious_keywords:
        if kw in text_content:
            score += 15.0

    # Check suspicious/lookalike URL indicators
    if urls:
        score += len(urls) * 5.0
        for url in urls:
            if "@" in url or "login" in url or "verify" in url:
                score += 10.0

    # Check domain spoofing heuristics
    if sender_domain and any(brand in sender_domain.lower() for brand in ["paypal", "google", "microsoft", "apple"]):
        if not sender_domain.endswith((".com", ".org", ".net")):
            score += 30.0

    return min(score, 100.0)

# ------------------------------------------------------------------------------
# 4. Main Endpoint: REST API for Spring Boot Integration
# ------------------------------------------------------------------------------
@app.post("/api/v1/analyze", response_model=EmailAnalysisResponse)
async def analyze_email(payload: EmailAnalysisRequest):
    try:
        subject = payload.subject or ""
        body_text = payload.bodyText or ""
        urls = payload.urls or []
        sender_domain = payload.senderDomain or ""

        # Step 1: Calculate Heuristic Baseline Score
        base_score = calculate_heuristic_score(subject, body_text, urls, sender_domain)

        # Step 2: Vector Search via ChromaDB (RAG Retrieval)
        retrieved_context = "No prior matching threat campaign detected in historical logs."
        rag_insights = "No historical vector match found."
        
        if vector_db and body_text.strip():
            try:
                docs = vector_db.similarity_search(body_text, k=2)
                if docs:
                    retrieved_context = "\n".join([doc.page_content for doc in docs])
                    rag_insights = f"Matched similarity with historical cases: {docs[0].page_content[:150]}..."
            except Exception as search_err:
                rag_insights = f"Vector search error: {str(search_err)}"

        # Step 3: Construct LLM Prompt
        prompt = ChatPromptTemplate.from_template("""
        You are an expert Cyber Forensics AI Investigator. Analyze the following email details:

        Subject: {subject}
        Sender Domain: {sender_domain}
        Extracted URLs: {urls}
        Email Body:
        {body_text}

        Historical Threat Database Context:
        {retrieved_context}

        Provide a concise, 2-sentence forensic threat report explaining whether this email is legitimate or a security risk, highlighting any social engineering or phishing cues.
        """)

        # Step 4: LLM Generation via LLaMA
        chain = prompt | llm
        llm_response = chain.invoke({
            "subject": subject,
            "sender_domain": sender_domain,
            "urls": ", ".join(urls),
            "body_text": body_text[:2000],  # Truncate to stay safely within context window
            "retrieved_context": retrieved_context
        })

        summary = llm_response.content if hasattr(llm_response, 'content') else str(llm_response)

        # Determine Risk Level
        if base_score >= 70.0:
            risk_level = "HIGH_RISK"
        elif base_score >= 35.0:
            risk_level = "MEDIUM_RISK"
        else:
            risk_level = "LOW_RISK"

        return EmailAnalysisResponse(
            threatScore=base_score,
            riskLevel=risk_level,
            summary=summary.strip(),
            ragInsights=rag_insights
        )

    except Exception as e:
        # Fallback response in case of processing error
        return EmailAnalysisResponse(
            threatScore=0.0,
            riskLevel="UNKNOWN",
            summary=f"Analysis pipeline error: {str(e)}",
            ragInsights="Execution failed."
        )

# ------------------------------------------------------------------------------
# 5. Local Server Runner Execution
# ------------------------------------------------------------------------------
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)