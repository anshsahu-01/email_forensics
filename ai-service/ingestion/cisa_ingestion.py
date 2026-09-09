import os
from langchain_chroma import Chroma
from langchain_ollama import OllamaEmbeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_core.documents import Document


# -----------------------------
# CONFIGURATION
# -----------------------------

CISA_DIR = "./data/cisa"
CHROMA_DB_DIR = "./chroma_db"

COLLECTION_NAME = "cisa_threat_intelligence"


# -----------------------------
# EMBEDDING MODEL
# -----------------------------

embeddings = OllamaEmbeddings(
    model="nomic-embed-text"
)


# -----------------------------
# LOAD CISA TEXT FILES
# -----------------------------

documents = []

for filename in os.listdir(CISA_DIR):

    if not filename.endswith(".txt"):
        continue

    filepath = os.path.join(CISA_DIR, filename)

    with open(filepath, "r", encoding="utf-8") as file:
        content = file.read().strip()

    if not content:
        print(f"Skipping empty file: {filename}")
        continue

    document = Document(
        page_content=content,
        metadata={
            "source": "CISA",
            "filename": filename
        }
    )

    documents.append(document)

    print(f"Loaded: {filename}")


# -----------------------------
# CHECK DOCUMENTS
# -----------------------------

if not documents:
    print("No CISA documents found.")
    exit(1)


print(f"\nTotal documents loaded: {len(documents)}")


# -----------------------------
# CHUNKING
# -----------------------------

text_splitter = RecursiveCharacterTextSplitter(
    chunk_size=800,
    chunk_overlap=150
)

chunks = text_splitter.split_documents(documents)

print(f"Created {len(chunks)} chunks")


# -----------------------------
# CREATE / LOAD CHROMA
# -----------------------------

vector_db = Chroma(
    collection_name=COLLECTION_NAME,
    persist_directory=CHROMA_DB_DIR,
    embedding_function=embeddings
)


# -----------------------------
# ADD DOCUMENTS
# -----------------------------

vector_db.add_documents(chunks)


print("\nCISA knowledge base successfully created!")
print(f"ChromaDB location: {CHROMA_DB_DIR}")
print(f"Collection: {COLLECTION_NAME}")
print(f"Chunks stored: {len(chunks)}")


# -----------------------------
# TEST RETRIEVAL
# -----------------------------

query = "phishing attack targeting Zimbra users"

results = vector_db.similarity_search(
    query,
    k=3
)

print("\n==============================")
print("RAG RETRIEVAL TEST")
print("==============================")

for i, result in enumerate(results, start=1):

    print(f"\n--- Result {i} ---")

    print("Source:",
          result.metadata.get("filename"))

    print("Content:")
    print(result.page_content[:500])