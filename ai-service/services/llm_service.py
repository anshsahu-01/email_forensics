import json
import re

from langchain_ollama import ChatOllama
from langchain_core.prompts import ChatPromptTemplate

from prompts.forensic_prompt import FORENSIC_STRUCTURED_PROMPT


class LLMService:

    def __init__(self):
        self.llm = ChatOllama(
            model="llama3.2:3b",
            temperature=0.1,
            format="json"
        )

    def _extract_json_object(self, text: str) -> dict:
        text = text.strip()

        # Remove markdown code fences if the model adds them
        text = re.sub(
            r"^```json\s*",
            "",
            text,
            flags=re.IGNORECASE
        )

        text = re.sub(r"^```\s*", "", text)
        text = re.sub(r"\s*```$", "", text)

        # First try normal JSON parsing
        try:
            return json.loads(text)
        except json.JSONDecodeError:
            pass

        # Find the first balanced JSON object
        start = text.find("{")

        if start == -1:
            raise ValueError("No JSON object found in LLM response")

        depth = 0
        in_string = False
        escape = False

        for i in range(start, len(text)):
            char = text[i]

            if escape:
                escape = False
                continue

            if char == "\\" and in_string:
                escape = True
                continue

            if char == '"':
                in_string = not in_string
                continue

            if in_string:
                continue

            if char == "{":
                depth += 1

            elif char == "}":
                depth -= 1

                if depth == 0:
                    candidate = text[start:i + 1]
                    return json.loads(candidate)

        raise ValueError("Incomplete JSON object in LLM response")

    def _build_fallback_reasoning(
        self,
        detected_indicators,
        verdict,
        threat_score
    ):
        """
        Build short evidence-based reasoning when Llama does not
        return the requested reasoning field.

        This does NOT invent new evidence.
        It only uses indicators already detected by Python.
        """

        reasoning = []

        if isinstance(detected_indicators, list):

            for indicator in detected_indicators:

                if isinstance(indicator, str):
                    statement = indicator.strip()

                    if statement and statement not in reasoning:
                        reasoning.append(statement)

                elif isinstance(indicator, dict):
                    # Try common fields if an indicator is represented
                    # as an object rather than a string.
                    statement = (
                        indicator.get("description")
                        or indicator.get("indicator")
                        or indicator.get("reason")
                        or indicator.get("name")
                    )

                    if statement:
                        statement = str(statement).strip()

                        if statement and statement not in reasoning:
                            reasoning.append(statement)

                if len(reasoning) >= 2:
                    break

        # If no usable indicators exist, use only the authoritative
        # deterministic result.
        if not reasoning:
            reasoning.append(
                f"Deterministic forensic analysis classified the email as "
                f"{verdict} with a threat score of {threat_score}."
            )

        return reasoning[:2]

    def analyze(
        self,
        threat_score,
        risk_level,
        verdict,
        confidence,
        subject,
        sender_domain,
        body_text,
        detected_indicators,
        url_analysis,
        whois_analysis,
        rag_context
    ):

        prompt = ChatPromptTemplate.from_messages([
            (
                "system",
                FORENSIC_STRUCTURED_PROMPT
            ),
            (
                "human",
                """
The Python forensic engine has already calculated the authoritative
threat assessment.

Your ONLY task is to provide concise human-readable reasoning explaining
the supplied evidence.

Do not recalculate the score.
Do not change the verdict.
Do not create new indicators.
Do not create new IOCs.
Do not add unsupported claims.

AUTHORITATIVE RESULT
Threat score: {threat_score}
Risk level: {risk_level}
Verdict: {verdict}
Confidence: {confidence}

EMAIL
Subject:
{subject}

Sender domain:
{sender_domain}

Email body:
{body_text}

DETECTED INDICATORS
{detected_indicators}

URL ANALYSIS
{url_analysis}

WHOIS/RDAP ANALYSIS
{whois_analysis}

CISA THREAT INTELLIGENCE
{rag_context}

Return EXACTLY this JSON structure:

{{
  "reasoning": [
    "evidence-based reason 1",
    "evidence-based reason 2"
  ],
  "summary": "short evidence-based forensic summary"
}}

The reasoning array MUST contain at least one item.
The reasoning array MUST contain no more than two items.
The summary MUST be present.
"""
            )
        ])

        try:

            prompt_value = prompt.invoke({
                "threat_score": threat_score,
                "risk_level": risk_level,
                "verdict": verdict,
                "confidence": confidence,
                "subject": subject,
                "sender_domain": sender_domain,
                "body_text": body_text,

                "detected_indicators": json.dumps(
                    detected_indicators,
                    indent=2,
                    default=str
                ),

                "url_analysis": json.dumps(
                    url_analysis,
                    indent=2,
                    default=str
                ),

                "whois_analysis": json.dumps(
                    whois_analysis,
                    indent=2,
                    default=str
                ),

                "rag_context": json.dumps(
                    rag_context,
                    indent=2,
                    default=str
                )
            })

            print("\n")
            print("=" * 70)
            print("SENDING EVIDENCE TO LLAMA")
            print("=" * 70)
            print("LLM model: llama3.2:3b")
            print("Indicators:", len(detected_indicators))
            print("RAG context:", len(str(rag_context)))
            print("=" * 70)
            print("\n")

            response = self.llm.invoke(prompt_value)

            raw_content = response.content

            # ============================================================
            # DEBUG: SHOW EXACT RAW RESPONSE FROM LLAMA
            # ============================================================

            print("=" * 70)
            print("RAW LLAMA RESPONSE")
            print("=" * 70)
            print(raw_content)
            print("=" * 70)
            print("END RAW LLAMA RESPONSE")
            print("=" * 70)

            # Parse Llama JSON response
            parsed = self._extract_json_object(raw_content)

            if not isinstance(parsed, dict):
                raise ValueError(
                    "LLM response JSON is not an object"
                )

            print("=" * 70)
            print("PARSED LLAMA OUTPUT")
            print("=" * 70)
            print("Summary:", parsed.get("summary", ""))
            print("Reasoning:", parsed.get("reasoning", []))
            print(
                "Reasoning type:",
                type(parsed.get("reasoning", [])).__name__
            )
            print("=" * 70)

            reasoning = parsed.get("reasoning", [])
            summary = parsed.get("summary", "")

            # Handle a single reasoning string
            if isinstance(reasoning, str):
                reasoning = [reasoning]

            # Reject invalid reasoning structures
            if not isinstance(reasoning, list):
                reasoning = []

            # Clean reasoning
            reasoning = [
                str(item).strip()
                for item in reasoning[:2]
                if item and str(item).strip()
            ]

            # ============================================================
            # IMPORTANT:
            # If Llama ignored the requested schema, use deterministic
            # evidence already detected by Python.
            # ============================================================

            if not reasoning:
                print("=" * 70)
                print("LLAMA RETURNED NO REASONING")
                print("USING DETERMINISTIC EVIDENCE FALLBACK")
                print("=" * 70)

                reasoning = self._build_fallback_reasoning(
                    detected_indicators,
                    verdict,
                    threat_score
                )

            # Ensure summary exists
            if not summary:
                summary = (
                    f"{verdict} email with threat score "
                    f"{threat_score}."
                )

            result = {
                "reasoning": reasoning[:2],
                "summary": str(summary)
            }

            print("=" * 70)
            print("FINAL LLM OUTPUT")
            print("=" * 70)
            print("Summary:", result["summary"])
            print("Reasoning:", result["reasoning"])
            print("=" * 70)
            print("\n")

            return result

        except Exception as e:

            print("\n")
            print("=" * 70)
            print("LLAMA ANALYSIS ERROR")
            print("=" * 70)
            print(str(e))
            print("=" * 70)
            print("\n")

            return {
                "reasoning": self._build_fallback_reasoning(
                    detected_indicators,
                    verdict,
                    threat_score
                ),
                "summary": (
                    f"{verdict} email with threat score "
                    f"{threat_score}."
                ),
                "error": str(e)
            }