// Shapes of the data the Java API returns. These mirror the Java records
// AlertSummary and AlertDetail, so TypeScript catches a typo in a field name
// before the page ever runs.

export type Verdict = 'REAL' | 'NOISE' | null

export interface AlertSummary {
  id: number
  candidateName: string
  referenceName: string
  totalWeight: number
  candidateDownloads: number
  referenceDownloads: number
  verdict: Verdict
  createdAt: string
}

export interface ClueEvidence {
  comparator: string
  level: 'STRONG' | 'WEAK' | 'NONE'
  rawScore: number
  weight: number
}

export interface AlertDetail extends AlertSummary {
  threshold: number
  publisher: string | null
  repositoryUrl: string | null
  evidence: ClueEvidence[]
}

// Every call goes to /api/..., which Vite forwards to the Java app in
// development, and nginx forwards in Docker.
async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, init)
  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText} for ${path}`)
  }
  return response.json() as Promise<T>
}

export function listAlerts(): Promise<AlertSummary[]> {
  return request<AlertSummary[]>('/api/alerts?limit=100')
}

export function getAlert(id: number): Promise<AlertDetail> {
  return request<AlertDetail>(`/api/alerts/${id}`)
}

export function setVerdict(id: number, verdict: 'REAL' | 'NOISE') {
  return request<{ id: number; verdict: string }>(`/api/alerts/${id}/verdict`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ verdict }),
  })
}