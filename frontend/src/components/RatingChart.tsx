import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts'
import type { RatingPoint } from '../api/types'

interface Props {
  data: RatingPoint[]
  currentRating?: number
  ratingDelta?: number
}

export function RatingChart({ data, currentRating, ratingDelta }: Props) {
  const formatted = data.map((p) => ({
    date: new Date(p.date).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
    rating: p.rating,
  }))

  const deltaColor = (ratingDelta ?? 0) >= 0 ? 'var(--green)' : 'var(--red)'
  const deltaLabel = ratingDelta != null ? `${ratingDelta >= 0 ? '+' : ''}${ratingDelta}` : null

  // Derived stats from the rating series
  const deltas = data.length > 1
    ? data.slice(1).map((p, i) => p.rating - data[i].rating)
    : []
  const positiveDeltas = deltas.filter(d => d > 0)
  const negativeDeltas = deltas.filter(d => d < 0)
  const highestClimb = positiveDeltas.length ? Math.max(...positiveDeltas) : 0
  const worstSlump   = negativeDeltas.length ? Math.min(...negativeDeltas) : 0

  const firstDate = data.length > 1 ? new Date(data[0].date).getTime() : null
  const lastDate  = data.length > 1 ? new Date(data[data.length - 1].date).getTime() : null
  const weeks = firstDate && lastDate ? (lastDate - firstDate) / (7 * 24 * 60 * 60 * 1000) : 0
  const avgPerWeek = weeks > 0 && data.length > 1
    ? Math.round((data[data.length - 1].rating - data[0].rating) / weeks)
    : null

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, marginBottom: 10 }}>
        {currentRating != null && currentRating > 0 && (
          <>
            <span style={{ fontSize: '1.6rem', fontWeight: 700 }}>{currentRating}</span>
            {deltaLabel && (
              <span style={{ fontSize: '0.85rem', fontWeight: 600, color: deltaColor }}>{deltaLabel}</span>
            )}
          </>
        )}
        <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', marginLeft: 'auto' }}>Current Rating</span>
      </div>

      <ResponsiveContainer width="100%" height={155}>
        <LineChart data={formatted} margin={{ top: 4, right: 8, left: -20, bottom: 0 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
          <XAxis dataKey="date" tick={{ fill: 'var(--text-muted)', fontSize: 10 }} tickLine={false} interval="preserveStartEnd" />
          <YAxis tick={{ fill: 'var(--text-muted)', fontSize: 10 }} tickLine={false} domain={['auto', 'auto']} />
          <Tooltip
            contentStyle={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 6 }}
            labelStyle={{ color: 'var(--text-muted)', fontSize: 11 }}
            itemStyle={{ color: 'var(--accent)', fontWeight: 600 }}
          />
          <Line type="monotone" dataKey="rating" stroke="var(--accent)" strokeWidth={2} dot={false} />
        </LineChart>
      </ResponsiveContainer>

      {/* Derived stat chips */}
      {deltas.length > 0 && (
        <div style={{ display: 'flex', gap: 10, marginTop: 10 }}>
          {highestClimb > 0 && (
            <div style={{ flex: 1, background: 'var(--surface-2)', borderRadius: 6, padding: '6px 10px', textAlign: 'center' }}>
              <div style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--green)' }}>+{highestClimb}</div>
              <div style={{ fontSize: '0.62rem', color: 'var(--text-muted)', marginTop: 2 }}>Best climb</div>
            </div>
          )}
          {worstSlump < 0 && (
            <div style={{ flex: 1, background: 'var(--surface-2)', borderRadius: 6, padding: '6px 10px', textAlign: 'center' }}>
              <div style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--red)' }}>{worstSlump}</div>
              <div style={{ fontSize: '0.62rem', color: 'var(--text-muted)', marginTop: 2 }}>Worst slump</div>
            </div>
          )}
          {avgPerWeek !== null && (
            <div style={{ flex: 1, background: 'var(--surface-2)', borderRadius: 6, padding: '6px 10px', textAlign: 'center' }}>
              <div style={{ fontSize: '0.95rem', fontWeight: 700, color: avgPerWeek >= 0 ? 'var(--accent)' : 'var(--text-muted)' }}>
                {avgPerWeek >= 0 ? '+' : ''}{avgPerWeek}
              </div>
              <div style={{ fontSize: '0.62rem', color: 'var(--text-muted)', marginTop: 2 }}>Avg / week</div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
