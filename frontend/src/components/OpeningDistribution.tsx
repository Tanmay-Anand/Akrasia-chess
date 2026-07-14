import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts'

interface Props { data: Record<string, number> }

export function OpeningDistribution({ data }: Props) {
  const entries = Object.entries(data)
    .sort(([, a], [, b]) => b - a)
    .slice(0, 8)
    .map(([eco, count]) => ({ eco, count }))

  if (entries.length === 0) {
    return <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>No opening data yet.</p>
  }

  return (
    <ResponsiveContainer width="100%" height={200}>
      <BarChart data={entries} margin={{ top: 8, right: 8, left: -20, bottom: 0 }}>
        <XAxis dataKey="eco" tick={{ fill: 'var(--text-muted)', fontSize: 11 }} tickLine={false} />
        <YAxis tick={{ fill: 'var(--text-muted)', fontSize: 11 }} tickLine={false} allowDecimals={false} />
        <Tooltip
          contentStyle={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 6 }}
          labelStyle={{ color: 'var(--text)', fontSize: 12, fontWeight: 600 }}
          itemStyle={{ color: 'var(--accent)' }}
        />
        <Bar dataKey="count" radius={[4, 4, 0, 0]}>
          {entries.map((_, i) => (
            <Cell key={i} fill={`hsl(${210 + i * 12}, 55%, ${50 - i * 3}%)`} />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  )
}
