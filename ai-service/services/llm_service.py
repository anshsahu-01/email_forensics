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
Authoritative threat score: {threat_score}
Authoritative risk level: {risk_level}
Authoritative verdict: {verdict}
Authoritative confidence: {confidence}

Subject:
{subject}

Sender domain:
{sender_domain}

Email body:
{body_text}

Detected indicators:
{detected_indicators}

URL analysis:
{url_analysis}

WHOIS/RDAP analysis:
{whois_analysis}

CISA threat intelligence context:
{rag_context}
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

            response = self.llm.invoke(prompt_value)

            raw_content = response.content

            parsed = self._extract_json_object(raw_content)

            if not isinstance(parsed, dict):
                raise ValueError(
                    "LLM response JSON is not an object"
                )

            reasoning = parsed.get("reasoning", [])
            summary = parsed.get("summary", "")

            if isinstance(reasoning, str):
                reasoning = [reasoning]

            if not isinstance(reasoning, list):
                reasoning = []

            reasoning = [
                str(item)
                for item in reasoning[:2]
                if item
            ]

            if not summary:
                summary = (
                    f"{verdict} email with threat score "
                    f"{threat_score}."
                )

            return {
                "reasoning": reasoning,
                "summary": str(summary)
            }
        except Exception as e:

           

            return {
                "reasoning": [
                    "LLM reasoning unavailable; deterministic forensic analysis remains authoritative."
                ],
                "summary": (
                    f"{verdict} email with threat score "
                    f"{threat_score}."
                ),
                "error": str(e)
            }