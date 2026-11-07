import { useEffect, useState } from 'react'
import { getAlert, setVerdict, type AlertDetail, type ClueEvidence } from './api'

// Plain-English names and one-line meanings for each comparator.
const CLUES: Record<string, { label: string; meaning: string }> = {
  edit_distance: {
    label: 'Edit distance',
    meaning: 'How few letter changes turn one name into the other',
  },
  jaro_winkler: {
    label: 'Jaro-Winkler',
    meaning: 'Overall similarity, with extra weight on a matching start',
  },
  keyboard_adjacency: {
    label: 'Keyboard slip',
    meaning: 'A changed letter sits right next to the real one on the keyboard',
  },
  homoglyph: {
    label: 'Look-alike letters',
    meaning: 'Characters swapped for ones that look the same, like 1 for l',
  },
  delimiter: {
    label: 'Punctuation trick',
    meaning: 'Same word, with a hyphen or dot added or removed',
  },
  family_prefix: {
    label: 'Family prefix',
    meaning: 'One name extends the other, like react-dom. Counts against a squat',
  },
}

// Bars are drawn on a fixed scale so they can be compared between alerts.
const BAR_SCALE_BITS = 12

/**
 * npm gives repository addresses in git's own formats
 * (git+https://..., git://..., git@github.com:...). Browsers can't open those,
 * so convert to a normal web address, or return null if it can't be done safely.
 */
function toWebUrl(raw: string | null): string | null {
  if (!raw) return null
  let url = raw.trim()
  url = url.replace(/^git\+/, '')
  url = url.replace(/^git:\/\//, 'https://')
  url = url.replace(/^git@([^:]+):/, 'https://$1/')
  url = url.replace(/\.git$/, '')
  return /^https?:\/\//.test(url) ? url : null
}

function formatDownloads(n: number): string {
  return new Intl.NumberFormat('en', { notation: 'compact' }).format(n)
}

function ClueRow({ clue }: { clue: ClueEvidence }) {
  const info = CLUES[clue.comparator] ?? { label: clue.comparator, meaning: '' }
  const width = Math.min(Math.abs(clue.weight) / BAR_SCALE_BITS, 1) * 100
  const direction = clue.weight < 0 ? 'against' : clue.weight > 0 ? 'for' : 'none'

  return (
    <li className="clue">
      <div className="clue-head">
        <span className="clue-label">{info.label}</span>
        <span className={`clue-bits clue-${direction}`}>
          {clue.weight === 0 ? 'no signal' : `${clue.weight > 0 ? '+' : ''}${clue.weight.toFixed(2)} bits`}
        </span>
      </div>
      <div className="clue-track">
        <div className={`clue-bar clue-bar-${direction}`} style={{ width: `${width}%` }} />
      </div>
      <p className="clue-meaning">{info.meaning}</p>
    </li>
  )
}

interface Props {
  alertId: number
  onClose: () => void
  onVerdictSaved: () => void
}

export default function EvidencePanel({ alertId, onClose, onVerdictSaved }: Props) {
  const [alert, setAlert] = useState<AlertDetail | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    setAlert(null)
    setError(null)
    getAlert(alertId)
      .then(setAlert)
      .catch((e) => setError(e instanceof Error ? e.message : String(e)))
  }, [alertId])

  async function judge(verdict: 'REAL' | 'NOISE') {
    if (!alert) return
    setSaving(true)
    try {
      await setVerdict(alert.id, verdict)
      setAlert({ ...alert, verdict })
      onVerdictSaved()
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e))
    } finally {
      setSaving(false)
    }
  }

  const repoUrl = toWebUrl(alert?.repositoryUrl ?? null)

  return (
    <aside className="panel" aria-label="Alert evidence">
      <div className="panel-top">
        <span className="panel-kicker">Why this was flagged</span>
        <button className="panel-close" onClick={onClose} aria-label="Close">
          ×
        </button>
      </div>

      {error && <p className="error">{error}</p>}
      {!alert && !error && <p className="muted">Loading…</p>}

      {alert && (
        <>
          <div className="pair">
            <div>
              <div className="pair-tag">Suspicious</div>
              <div className="pair-name mono">{alert.candidateName}</div>
            </div>
            <div className="pair-vs">vs</div>
            <div>
              <div className="pair-tag">Real package</div>
              <div className="pair-name mono">{alert.referenceName}</div>
            </div>
          </div>

          <section className="stats">
            <div className="stat">
              <div className="stat-value">{alert.totalWeight.toFixed(1)}</div>
              <div className="stat-label">bits of evidence (alert at {alert.threshold})</div>
            </div>
            <div className="stat">
              <div className="stat-value">
                {formatDownloads(alert.candidateDownloads)}
                <span className="stat-sep"> vs </span>
                {formatDownloads(alert.referenceDownloads)}
              </div>
              <div className="stat-label">weekly downloads</div>
            </div>
          </section>

          <h3 className="panel-heading">The six clues</h3>
          <ul className="clues">
            {alert.evidence.map((clue) => (
              <ClueRow key={clue.comparator} clue={clue} />
            ))}
          </ul>

          <dl className="meta">
            <dt>Publisher</dt>
            <dd>{alert.publisher ?? <span className="muted">unknown</span>}</dd>
            <dt>Source code</dt>
            <dd>
              {repoUrl ? (
                <a href={repoUrl} target="_blank" rel="noreferrer noopener">
                  {repoUrl.replace(/^https?:\/\//, '')}
                </a>
              ) : (
                <span className="muted">none listed</span>
              )}
            </dd>
          </dl>

          <div className="verdict">
            <span className="verdict-label">
              Your verdict: <strong>{alert.verdict ?? 'not reviewed'}</strong>
            </span>
            <div className="verdict-buttons">
              <button
                className="btn btn-real"
                disabled={saving || alert.verdict === 'REAL'}
                onClick={() => judge('REAL')}
              >
                Real threat
              </button>
              <button
                className="btn btn-noise"
                disabled={saving || alert.verdict === 'NOISE'}
                onClick={() => judge('NOISE')}
              >
                Noise
              </button>
            </div>
          </div>
        </>
      )}
    </aside>
  )
}