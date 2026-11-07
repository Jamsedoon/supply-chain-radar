import { useEffect, useState } from 'react'
import { listAlerts, type AlertSummary } from './api'
import EvidencePanel from './EvidencePanel'
import './App.css'

// Downloads range from 0 to hundreds of millions; show them compactly.
function formatDownloads(n: number): string {
  return new Intl.NumberFormat('en', { notation: 'compact' }).format(n)
}

// Colour bands for the evidence score, in bits. The alert threshold is 9.
function riskClass(bits: number): string {
  if (bits >= 20) return 'risk risk-high'
  if (bits >= 12) return 'risk risk-medium'
  return 'risk risk-low'
}

export default function App() {
  const [alerts, setAlerts] = useState<AlertSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selectedId, setSelectedId] = useState<number | null>(null)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      setAlerts(await listAlerts())
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [])

  return (
    <main className={selectedId === null ? 'page' : 'page page-with-panel'}>
      <div className="main-column">
        <header className="page-header">
          <div>
            <h1>Supply-Chain Radar</h1>
            <p className="subtitle">
              npm packages impersonating popular ones, highest risk first
            </p>
          </div>
          <button onClick={load} disabled={loading}>
            {loading ? 'Loading…' : 'Refresh'}
          </button>
        </header>

        {error && <p className="error">Could not load alerts: {error}</p>}

        {!loading && !error && alerts.length === 0 && (
          <p className="empty">No alerts yet. The radar is watching.</p>
        )}

        {alerts.length > 0 && (
          <div className="table-wrap">
            <table className="alerts">
              <thead>
                <tr>
                  <th>Suspicious package</th>
                  <th>Looks like</th>
                  <th className="num">Evidence</th>
                  <th className="num">Its downloads</th>
                  <th className="num">Real one's downloads</th>
                  <th>Verdict</th>
                </tr>
              </thead>
              <tbody>
                {alerts.map((a) => (
                  <tr
                    key={a.id}
                    className={a.id === selectedId ? 'selected' : undefined}
                    onClick={() => setSelectedId(a.id)}
                  >
                    <td className="mono">{a.candidateName}</td>
                    <td className="mono">{a.referenceName}</td>
                    <td className="num">
                      <span className={riskClass(a.totalWeight)}>
                        {a.totalWeight.toFixed(1)} bits
                      </span>
                    </td>
                    <td className="num">{formatDownloads(a.candidateDownloads)}</td>
                    <td className="num">{formatDownloads(a.referenceDownloads)}</td>
                    <td>{a.verdict ?? <span className="muted">not reviewed</span>}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {selectedId !== null && (
        <EvidencePanel
          alertId={selectedId}
          onClose={() => setSelectedId(null)}
          onVerdictSaved={load}
        />
      )}
    </main>
  )
}