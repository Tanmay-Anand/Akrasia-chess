import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import type { GameSummary } from '../api/types'

function resultBadge(result: string) {
  return <span className={`badge badge-${result}`}>{result.toUpperCase()}</span>
}

function statusBadge(status: string) {
  return <span className={`badge badge-${status.toLowerCase()}`}>{status}</span>
}

export function GameList() {
  const navigate = useNavigate()
  const { data: games, isLoading } = useQuery({
    queryKey: ['games'],
    queryFn: api.games.list,
  })

  if (isLoading) return <p style={{ color: 'var(--text-muted)' }}>Loading games...</p>
  if (!games?.length) return <p style={{ color: 'var(--text-muted)' }}>No games synced yet. Use "Sync Now" above.</p>

  return (
    <div>
      <h1 style={{ fontSize: '1.3rem', fontWeight: 700, marginBottom: 20 }}>
        Games <span style={{ color: 'var(--text-muted)', fontWeight: 400, fontSize: '1rem' }}>({games.length})</span>
      </h1>
      <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ borderBottom: '1px solid var(--border)', background: 'var(--surface-2)' }}>
              {['Date', 'Color', 'Result', 'Opening', 'Time', 'Rating', 'Status'].map((h) => (
                <th key={h} style={{ padding: '10px 16px', textAlign: 'left', fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {games.map((g: GameSummary) => (
              <tr
                key={g.id}
                onClick={() => g.analysis_status === 'ANALYZED' && navigate(`/games/${g.id}`)}
                style={{
                  borderBottom: '1px solid var(--border)',
                  cursor: g.analysis_status === 'ANALYZED' ? 'pointer' : 'default',
                  transition: 'background 0.1s',
                }}
                onMouseEnter={(e) => { if (g.analysis_status === 'ANALYZED') (e.currentTarget as HTMLElement).style.background = 'var(--surface-2)' }}
                onMouseLeave={(e) => { (e.currentTarget as HTMLElement).style.background = '' }}
              >
                <td style={{ padding: '10px 16px', fontSize: '0.82rem', color: 'var(--text-muted)' }}>
                  {g.played_at ? new Date(g.played_at).toLocaleDateString() : '—'}
                </td>
                <td style={{ padding: '10px 16px', fontSize: '0.82rem', textTransform: 'capitalize' }}>
                  {g.player_color}
                </td>
                <td style={{ padding: '10px 16px' }}>{resultBadge(g.result)}</td>
                <td style={{ padding: '10px 16px', fontSize: '0.82rem', color: 'var(--text-muted)', maxWidth: 200 }}>
                  {g.opening_eco ? `${g.opening_eco}${g.opening_name ? ' · ' + g.opening_name : ''}` : '—'}
                </td>
                <td style={{ padding: '10px 16px', fontSize: '0.82rem', color: 'var(--text-muted)', textTransform: 'capitalize' }}>
                  {g.time_class ?? '—'}
                </td>
                <td style={{ padding: '10px 16px', fontSize: '0.82rem' }}>{g.player_rating || '—'}</td>
                <td style={{ padding: '10px 16px' }}>{statusBadge(g.analysis_status)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
