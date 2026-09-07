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
  receivedHeaders: ReceivedHeader[] | null;
  rawBody: string | null;
  createdAt: string;
  header: EmailHeader | null;
  indicators: EmailIndicator[] | null;
}
