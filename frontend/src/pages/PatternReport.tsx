import { usePatternReport } from '../hooks/usePatternReport'
import { PatternHeatmap } from '../components/PatternHeatmap'

export function PatternReport() {
  const { data: pattern, isLoading } = usePatternReport()

  if (isLoading) return <p style={{ color: 'var(--text-muted)' }}>Loading patterns...</p>
  if (!pattern) {
    return (
      <div>
        <h1 style={{ fontSize: '1.3rem', fontWeight: 700, marginBottom: 12 }}>Pattern Report</h1>
        <p style={{ color: 'var(--text-muted)' }}>No pattern data yet. Sync and analyze your games first.</p>
      </div>
    )
  }

  const motifData: Record<string, number> = pattern.motif_frequency ? JSON.parse(pattern.motif_frequency) : {}
  const topMotifs = Object.entries(motifData)
    .sort(([, a], [, b]) => b - a)
    .slice(0, 5)

  const phaseData = [
    { phase: 'Opening',    count: pattern.mistakes_opening,    color: 'var(--green)' },
    { phase: 'Middlegame', count: pattern.mistakes_middlegame, color: 'var(--yellow)' },
    { phase: 'Endgame',    count: pattern.mistakes_endgame,    color: 'var(--red)' },
  ]

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      <h1 style={{ fontSize: '1.3rem', fontWeight: 700 }}>
        Pattern Report
        <span style={{ fontSize: '0.85rem', fontWeight: 400, color: 'var(--text-muted)', marginLeft: 10 }}>
          {pattern.games_analyzed} games analyzed
        </span>
      </h1>

      {/* AI-generated weaknesses */}
      {pattern.primary_weakness && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 12 }}>
          {[
            { label: '#1 Primary Weakness',   text: pattern.primary_weakness,   color: 'var(--red)' },
            { label: '#2 Secondary Weakness', text: pattern.secondary_weakness, color: 'var(--yellow)' },
            { label: '#3 Tertiary Weakness',  text: pattern.tertiary_weakness,  color: 'var(--accent)' },
          ].filter(w => w.text).map(({ label, text, color }) => (
            <div key={label} className="card">
              <div style={{ fontSize: '0.7rem', color, fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: 8 }}>{label}</div>
              <p style={{ fontSize: '0.85rem', lineHeight: 1.6 }}>{text}</p>
            </div>
          ))}
        </div>
      )}

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
        {/* Mistake heatmap by move range */}
        <div className="card">
          <h2 style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: 16 }}>
            Mistakes by Move Range
          </h2>
          <PatternHeatmap pattern={pattern} />
          {pattern.critical_move_range && (
            <p style={{ marginTop: 10, fontSize: '0.8rem', color: 'var(--orange)' }}>
              Critical zone: <strong>{pattern.critical_move_range}</strong>
            </p>
          )}
        </div>

        {/* Phase breakdown */}
        <div className="card">
          <h2 style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: 16 }}>
            Mistakes by Game Phase
          </h2>
          {phaseData.map(({ phase, count, color }) => {
            const total = pattern.mistakes_opening + pattern.mistakes_middlegame + pattern.mistakes_endgame || 1
            const pct = Math.round((count / total) * 100)
            return (
              <div key={phase} style={{ marginBottom: 12 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4, fontSize: '0.82rem' }}>
                  <span>{phase}</span>
                  <span style={{ color: 'var(--text-muted)' }}>{count} ({pct}%)</span>
                </div>
                <div style={{ height: 8, background: 'var(--surface-2)', borderRadius: 4, overflow: 'hidden' }}>
                  <div style={{ height: '100%', width: `${pct}%`, background: color, borderRadius: 4 }} />
                </div>
              </div>
            )
          })}
        </div>
      </div>

      {/* Tactical motif breakdown */}
      {topMotifs.length > 0 && (
        <div className="card">
          <h2 style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: 16 }}>
            Tactical Motifs
            {pattern.dominant_motif && (
              <span style={{ marginLeft: 10, color: 'var(--orange)', fontWeight: 400 }}>
                — dominant: {pattern.dominant_motif.replace('_', ' ')}
              </span>
            )}
          </h2>
          <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
            {topMotifs.map(([motif, count]) => (
              <div key={motif} className="card" style={{ padding: '10px 16px', textAlign: 'center', minWidth: 100 }}>
                <div style={{ fontSize: '1.3rem', fontWeight: 700, color: 'var(--yellow)' }}>{count}</div>
                <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: 2 }}>
                  {motif.replace(/_/g, ' ')}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Opening assessment */}
      {pattern.opening_assessment && (
        <div className="card">
          <h2 style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: 8 }}>
            Opening Assessment
          </h2>
          <p style={{ fontSize: '0.85rem', lineHeight: 1.6 }}>{pattern.opening_assessment}</p>
        </div>
      )}
    </div>
  )
}
