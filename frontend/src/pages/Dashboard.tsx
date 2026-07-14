import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import { RatingChart } from '../components/RatingChart'
import { OpeningDistribution } from '../components/OpeningDistribution'

export function Dashboard() {
  const { data: stats, isLoading } = useQuery({
    queryKey: ['dashboard-stats'],
    queryFn: api.dashboard.stats,
  })

  if (isLoading) return <p style={{ color: 'var(--text-muted)' }}>Loading...</p>
  if (!stats) return <p style={{ color: 'var(--text-muted)' }}>No data yet. Sync your games first.</p>

  const total = stats.total_games || 1
  const winPct  = Math.round((stats.wins  / total) * 100)
  const lossPct = Math.round((stats.losses / total) * 100)
  const drawPct = Math.round((stats.draws  / total) * 100)

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      <h1 style={{ fontSize: '1.3rem', fontWeight: 700 }}>Dashboard</h1>

      {/* Stat tiles */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))', gap: 12 }}>
        {[
          { label: 'Total Games',    value: stats.total_games,    color: 'var(--text)' },
          { label: 'Wins',           value: `${stats.wins} (${winPct}%)`,  color: 'var(--green)' },
          { label: 'Losses',         value: `${stats.losses} (${lossPct}%)`, color: 'var(--red)' },
          { label: 'Draws',          value: `${stats.draws} (${drawPct}%)`,  color: 'var(--text-muted)' },
          { label: 'Analyzed',       value: stats.games_analyzed, color: 'var(--accent)' },
        ].map(({ label, value, color }) => (
          <div key={label} className="card" style={{ textAlign: 'center' }}>
            <div style={{ fontSize: '1.6rem', fontWeight: 700, color }}>{value}</div>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginTop: 4 }}>{label}</div>
          </div>
        ))}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
        <div className="card">
          <h2 style={{ fontSize: '0.9rem', fontWeight: 600, marginBottom: 16, color: 'var(--text-muted)' }}>
            Rating History
          </h2>
          {stats.rating_history.length > 0
            ? <RatingChart data={stats.rating_history} />
            : <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>No rating data yet.</p>}
        </div>

        <div className="card">
          <h2 style={{ fontSize: '0.9rem', fontWeight: 600, marginBottom: 16, color: 'var(--text-muted)' }}>
            Opening Distribution
          </h2>
          <OpeningDistribution data={stats.opening_distribution} />
        </div>
      </div>
    </div>
  )
}
