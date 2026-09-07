import { EmailCase } from '@/types';

const API_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/api/v1';

export async function fetchCases(): Promise<EmailCase[]> {
  const response = await fetch(`${API_URL}/cases`);
  if (!response.ok) {
    throw new Error(`Failed to load cases (${response.status})`);
  }
  return response.json();
}

export async function fetchCaseById(id: string | number): Promise<EmailCase> {
  const response = await fetch(`${API_URL}/cases/${id}`);
  if (!response.ok) {
    throw new Error(`Failed to fetch case details (${response.status})`);
  }
  return response.json();
}

export async function analyzeEmail(file: File): Promise<EmailCase> {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch(`${API_URL}/emails/analyze`, {
    method: 'POST',
    body: formData,
  });

  if (!response.ok) {
    throw new Error('Email analysis failed.');
  }

  return response.json();
}
