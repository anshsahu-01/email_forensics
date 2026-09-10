declare global {
  interface Window {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    google: any;
  }
}

export interface EmailHeader {
  id?: number;
  subject: string | null;
  senderFrom: string | null;
  to: string | null;
  cc: string | null;
  replyTo: string | null;
  date: string | null;
  messageId: string | null;
  returnPath: string | null;
  spfStatus: string | null;
  dkimStatus: string | null;
  dmarcStatus: string | null;
}

export interface EmailIndicator {
  id: number;
  type: string;
  value: string;
  details: string | null;

  vtMalicious: number | null;
  vtSuspicious: number | null;
  vtHarmless: number | null;
  vtUndetected: number | null;
  vtReputation: string | null;

  // AbuseIPDB
  abuseIpDbStatus: string | null;
  abuseConfidenceScore: number | null;
  totalReports: number | null;
  lastReportedAt: string | null;

  // ASN / Network Intelligence
  asnNumber: string | null;
  asnOrg: string | null;

  // RDAP / Registry Intelligence
  rdapServer: string | null;
  rdapRegistry: string | null;
  rdapHandle: string | null;
  rdapName: string | null;
  rdapOrganization: string | null;
  rdapCountry: string | null;
  rdapStartAddress: string | null;
  rdapEndAddress: string | null;
  rdapCidr: string | null;
}

export interface ReceivedHeader {
  from: string | null;
  by: string | null;
  with: string | null;
  id: string | null;
  for: string | null;
  date: string | null;
  ip: string | null;
  hopIndex: number;
}

export interface EmailCase {
  id: number;
  fileName: string | null;
  fileHash: string | null;
  analysisStatus: string;

  threatScore: number | null;

  originatingIp: string | null;
  senderIp: string | null;
  connectingIp: string | null;
  spoofingRisk: string | null;

  geoCountry: string | null;
  geoCity: string | null;
  geoLatitude: number | null;
  geoLongitude: number | null;
  geoTimezone: string | null;

  receivedHeaders: ReceivedHeader[] | null;
  rawBody: string | null;

  createdAt: string;

  header: EmailHeader | null;
  indicators: EmailIndicator[] | null;

  // =========================================================
  // AI FORENSIC ANALYSIS
  // These fields are stored by Spring Boot as JSON strings.
  // =========================================================

  aiRiskLevel: string | null;
  aiVerdict: string | null;
  aiConfidence: number | null;

  aiSummary: string | null;
  aiReasoning: string | null;

  aiIndicators: string | null;
  aiAttackTechniques: string | null;
  aiIocs: string | null;
  aiOriginAnalysis: string | null;

  aiRagInsights: string | null;
  aiWhoisAnalysis: string | null;
  aiUrlAnalysis: string | null;

  aiGraphs: string | null;
  aiError: string | null;
}