import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell } from 'recharts'
import type { Pattern } from '../api/types'

interface Props { pattern: Pattern }

export function PatternHeatmap({ pattern }: Props) {
  const data = [
    { range: 'Moves 1–10',  count: pattern.mistakes_moves1to10,  color: '#4ade80' },
    { range: 'Moves 11–20', count: pattern.mistakes_moves11to20, color: '#fbbf24' },
    { range: 'Moves 21–30', count: pattern.mistakes_moves21to30, color: '#fb923c' },
    { range: 'Moves 31+',   count: pattern.mistakes_moves31_plus, color: '#f87171' },
  ]

  return (
    <ResponsiveContainer width="100%" height={180}>
      <BarChart data={data} margin={{ top: 8, right: 8, left: -20, bottom: 0 }}>
        <XAxis dataKey="range" tick={{ fill: 'var(--text-muted)', fontSize: 11 }} tickLine={false} />
        <YAxis tick={{ fill: 'var(--text-muted)', fontSize: 11 }} tickLine={false} allowDecimals={false} />
        <Tooltip
          contentStyle={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 6 }}
          labelStyle={{ color: 'var(--text)', fontSize: 12 }}
          itemStyle={{ color: 'var(--text)' }}
          formatter={(v) => [`${v} mistakes`, '']}
        />
        <Bar dataKey="count" radius={[4, 4, 0, 0]}>
          {data.map((d, i) => <Cell key={i} fill={d.color} />)}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  )
}
