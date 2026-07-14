import type { MoveError } from '../api/types'

interface Props {
  error: MoveError
  isSelected: boolean
  onClick: () => void
}

const SEVERITY_SYMBOL: Record<string, string> = {
  BLUNDER: '??',
  MISTAKE: '?',
  INACCURACY: '?!',
}

export function MoveErrorCard({ error, isSelected, onClick }: Props) {
  const symbol = SEVERITY_SYMBOL[error.severity] ?? '?'
  const severityClass = `badge badge-${error.severity.toLowerCase()}`

  return (
    <div
      onClick={onClick}
      style={{
        padding: '12px 14px',
        borderRadius: 8,
        cursor: 'pointer',
        border: `1px solid ${isSelected ? 'var(--accent)' : 'var(--border)'}`,
        background: isSelected ? 'var(--accent-dim)' : 'var(--surface-2)',
        marginBottom: 8,
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
        <span style={{ fontWeight: 700, fontFamily: 'monospace', color: 'var(--text)' }}>
          {Math.ceil(error.move_number / 2)}.{error.move_played}{symbol}
        </span>
        <span className={severityClass}>{error.severity}</span>
        {error.tactical_motif && error.tactical_motif !== 'OTHER' && (
          <span className="badge" style={{ background: 'var(--surface)', color: 'var(--text-muted)', border: '1px solid var(--border)' }}>
            {error.tactical_motif.replace('_', ' ')}
          </span>
        )}
        {error.game_phase && (
          <span style={{ marginLeft: 'auto', fontSize: '0.7rem', color: 'var(--text-muted)' }}>
            {error.game_phase}
          </span>
        )}
      </div>
      {error.better_move && (
        <div style={{ fontSize: '0.8rem', color: 'var(--green)', marginBottom: 4 }}>
          Better: <strong>{error.better_move}</strong>
        </div>
      )}
      {error.explanation && !error.analysis_failed && (
        <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)', lineHeight: 1.5 }}>
          {error.explanation}
        </p>
      )}
      {error.analysis_failed && (
        <p style={{ fontSize: '0.78rem', color: 'var(--red)', fontStyle: 'italic' }}>
          AI analysis unavailable for this move.
        </p>
      )}
      {error.clock_remaining !== null && error.clock_remaining !== undefined && error.clock_remaining <= 30 && (
        <div style={{ marginTop: 4, fontSize: '0.72rem', color: 'var(--orange)' }}>
          ⏱ Time pressure ({error.clock_remaining}s remaining)
        </div>
      )}
    </div>
  )
}
