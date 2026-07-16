import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import { RatingChart } from '../components/RatingChart'
import { OpeningDistribution } from '../components/OpeningDistribution'
import { BestWorstOpenings } from '../components/BestWorstOpenings'
import { RecentGames } from '../components/RecentGames'
import { usePatternReport } from '../hooks/usePatternReport'
import type { TimeControlStat } from '../api/types'

const tile = (icon: string, label: string, value: React.ReactNode, sub?: React.ReactNode, color = 'var(--text)') => (
  <div className="card" style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
    <span style={{ fontSize: '1.4rem', lineHeight: 1 }}>{icon}</span>
    <div style={{ flex: 1 }}>
      <div style={{ fontSize: '1.4rem', fontWeight: 700, color, lineHeight: 1.1 }}>{value}</div>
      <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: 3 }}>{label}</div>
      {sub && <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: 2 }}>{sub}</div>}
    </div>
  </div>
)

const statRow = (label: string, value: React.ReactNode, valueColor = 'var(--text)') => (
  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                padding: '8px 0', borderBottom: '1px solid var(--border)' }}>
    <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>{label}</span>
    <span style={{ fontSize: '0.9rem', fontWeight: 700, color: valueColor }}>{value}</span>
  </div>
)

function FormStreakBadge({ streak }: { streak: number }) {
  if (streak === 0) return <span style={{ color: 'var(--text-muted)' }}>—</span>
  const isWin = streak > 0
  const abs = Math.abs(streak)
  return (
    <span style={{ color: isWin ? 'var(--green)' : 'var(--red)', fontWeight: 700, fontSize: '1.1rem' }}>
      {isWin ? `W${abs}` : `L${abs}`}
    </span>
  )
}

function TimeControlBar({ tc }: { tc: TimeControlStat }) {
  return (
    <div style={{ marginBottom: 10 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4, fontSize: '0.8rem' }}>
        <span style={{ textTransform: 'capitalize', fontWeight: 500 }}>{tc.time_class}</span>
        <span style={{ color: 'var(--text-muted)' }}>{tc.games}g · {tc.win_pct}% WR</span>
      </div>
      <div style={{ height: 6, background: 'var(--surface-2)', borderRadius: 4, overflow: 'hidden' }}>
        <div style={{
          height: '100%', width: `${tc.win_pct}%`,
          background: tc.win_pct >= 55 ? 'var(--green)' : tc.win_pct >= 45 ? 'var(--accent)' : 'var(--red)',
          borderRadius: 4,
        }} />
      </div>
    </div>
  )
}

function WinSideCard({ label, games, winPct }: { label: string; games: number; winPct: number }) {
  const color = winPct >= 55 ? 'var(--green)' : winPct >= 45 ? 'var(--accent)' : 'var(--red)'
  return (
    <div style={{ textAlign: 'center', flex: 1 }}>
      <div style={{ fontSize: '1.5rem', fontWeight: 700, color }}>{winPct}%</div>
      <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: 2 }}>{label} WR</div>
      <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>{games} games</div>
    </div>
  )
}

export function Dashboard() {
  const queryClient = useQueryClient()
  const { data: stats, isLoading } = useQuery({
    queryKey: ['dashboard-stats'],
    queryFn: api.dashboard.stats,
  })
  const { data: pattern } = usePatternReport()

  const reanalyzeMut = useMutation({
    mutationFn: api.analysis.reanalyzeAll,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['dashboard-stats'] })
      queryClient.invalidateQueries({ queryKey: ['analysis-progress'] })
    },
  })

  if (isLoading) return <p style={{ color: 'var(--text-muted)' }}>Loading...</p>
  if (!stats)    return <p style={{ color: 'var(--text-muted)' }}>No data yet. Sync your games first.</p>

  const total   = stats.total_games || 1
  const winPct  = Math.round((stats.wins   / total) * 100)
  const lossPct = Math.round((stats.losses / total) * 100)
  const drawPct = Math.round((stats.draws  / total) * 100)

  // Today's Focus — derived from pattern data
  const focusPhase = pattern
    ? (['opening', 'middlegame', 'endgame'] as const).reduce((best, p) => {
        const counts = { opening: pattern.mistakes_opening, middlegame: pattern.mistakes_middlegame, endgame: pattern.mistakes_endgame }
        return counts[p] > counts[best] ? p : best
      }, 'opening' as 'opening' | 'middlegame' | 'endgame')
    : null

  const totalMistakes = pattern
    ? (pattern.mistakes_opening + pattern.mistakes_middlegame + pattern.mistakes_endgame) || 1
    : 1
  const focusCount = pattern && focusPhase
    ? ({ opening: pattern.mistakes_opening, middlegame: pattern.mistakes_middlegame, endgame: pattern.mistakes_endgame })[focusPhase]
    : 0
  const focusPct = Math.round((focusCount / totalMistakes) * 100)

  const phaseEmoji: Record<string, string> = { opening: '📖', middlegame: '⚔️', endgame: '♟' }
  const phaseLabel = focusPhase
    ? focusPhase.charAt(0).toUpperCase() + focusPhase.slice(1)
    : null

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>

      {/* Today's Focus Banner */}
      {pattern && focusPhase && (
        <div className="card" style={{
          borderLeft: '3px solid var(--accent)',
          display: 'flex', alignItems: 'center', gap: 18,
          padding: '14px 18px',
        }}>
          <span style={{ fontSize: '1.8rem', lineHeight: 1 }}>{phaseEmoji[focusPhase]}</span>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: '0.65rem', color: 'var(--accent)', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.06em', marginBottom: 4 }}>
              Today's Focus
            </div>
            <div style={{ fontSize: '1rem', fontWeight: 700, marginBottom: 4 }}>
              Fix {phaseLabel} Mistakes
            </div>
            <div style={{ fontSize: '0.82rem', color: 'var(--text-muted)', lineHeight: 1.5, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {focusPct}% of your mistakes happen here
              {pattern.primary_weakness ? ` · ${pattern.primary_weakness.slice(0, 80)}${pattern.primary_weakness.length > 80 ? '…' : ''}` : ''}
            </div>
          </div>
          <Link
            to="/training"
            style={{
              display: 'inline-block', padding: '8px 16px', borderRadius: 6,
              background: 'var(--accent)', color: '#fff', fontWeight: 600,
              fontSize: '0.82rem', textDecoration: 'none', flexShrink: 0,
              whiteSpace: 'nowrap',
            }}
          >
            Start Training →
          </Link>
        </div>
      )}

      {/* Row 1 — stat tiles */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: 12 }}>
        {tile('♟', 'Total Games',  stats.total_games)}
        {tile('↗', 'Wins',         `${stats.wins} (${winPct}%)`,  undefined, 'var(--green)')}
        {tile('↘', 'Losses',       `${stats.losses} (${lossPct}%)`, undefined, 'var(--red)')}
        {tile('↔', 'Draws',        `${stats.draws} (${drawPct}%)`,  undefined, 'var(--text-muted)')}
        {tile('🔍', 'Analyzed',    stats.games_analyzed, undefined, 'var(--accent)')}
      </div>

      {/* Row 2 — Coach cards */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12 }}>
        <div className="card" style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
          <span style={{ fontSize: '1.4rem' }}>🔥</span>
          <div>
            <div style={{ lineHeight: 1.1 }}><FormStreakBadge streak={stats.form_streak} /></div>
            <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: 3 }}>Current Streak</div>
          </div>
        </div>

        <div className="card">
          <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginBottom: 10, fontWeight: 600 }}>Win Rate by Side</div>
          <div style={{ display: 'flex', gap: 12 }}>
            <WinSideCard label="White" games={stats.white_games} winPct={stats.white_win_pct} />
            <div style={{ width: 1, background: 'var(--border)' }} />
            <WinSideCard label="Black" games={stats.black_games} winPct={stats.black_win_pct} />
          </div>
        </div>

        <div className="card" style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
          <span style={{ fontSize: '1.4rem' }}>🎯</span>
          <div>
            <div style={{ fontSize: '1.4rem', fontWeight: 700, color: 'var(--accent)', lineHeight: 1.1 }}>
              {stats.avg_accuracy != null ? `${stats.avg_accuracy}%` : '—'}
            </div>
            <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: 3 }}>Avg Accuracy</div>
            {stats.best_accuracy != null && (
              <div style={{ fontSize: '0.7rem', color: 'var(--green)', marginTop: 2 }}>Best: {stats.best_accuracy}%</div>
            )}
          </div>
        </div>

        <div className="card" style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
          <span style={{ fontSize: '1.4rem' }}>⚠</span>
          <div>
            <div style={{ fontSize: '1.4rem', fontWeight: 700, color: 'var(--red)', lineHeight: 1.1 }}>
              {stats.blunder_count}
            </div>
            <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', marginTop: 3 }}>Total Blunders</div>
          </div>
        </div>
      </div>

      {/* Row 3 — Rating History | Opening Distribution | Performance + Time Control */}
      <div style={{ display: 'grid', gridTemplateColumns: '1.8fr 1.4fr 1fr', gap: 16 }}>
        <div className="card">
          <div style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: 4 }}>Rating History</div>
          {stats.rating_history.length > 0
            ? <RatingChart data={stats.rating_history} currentRating={stats.current_rating} ratingDelta={stats.rating_delta} />
            : <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>No rating data yet.</p>}
        </div>

        <div className="card">
          <div style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: 14 }}>Opening Distribution</div>
          <OpeningDistribution data={stats.opening_stats} totalGames={stats.total_games} />
        </div>

        <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 0 }}>
          <div style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: 4 }}>Performance</div>
          {statRow('Analyzed', stats.games_analyzed, 'var(--accent)')}

          {stats.time_control_stats?.length > 0 && (
            <div style={{ marginTop: 14 }}>
              <div style={{ fontSize: '0.72rem', color: 'var(--text-muted)', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: 10 }}>
                By Time Control
              </div>
              {stats.time_control_stats.map(tc => (
                <TimeControlBar key={tc.time_class} tc={tc} />
              ))}
            </div>
          )}

          <div style={{ marginTop: 'auto', paddingTop: 14 }}>
            <button
              className="secondary"
              style={{ width: '100%', fontSize: '0.78rem', padding: '7px 12px' }}
              onClick={() => reanalyzeMut.mutate()}
              disabled={reanalyzeMut.isPending}>
              {reanalyzeMut.isPending ? 'Queuing…' : '↺ Re-Analyze All'}
            </button>
          </div>
        </div>
      </div>

      {/* Row 4 — Mistakes by Phase | Best/Worst Openings | Recent Games */}
      <div style={{ display: 'grid', gridTemplateColumns: '0.8fr 1.6fr 1.2fr', gap: 16 }}>
        <div className="card">
          <div style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: 14 }}>
            Mistakes by Phase
          </div>
          {pattern ? (
            <div>
              {[
                { label: 'Opening',    count: pattern.mistakes_opening,    color: 'var(--green)' },
                { label: 'Middlegame', count: pattern.mistakes_middlegame, color: 'var(--yellow)' },
                { label: 'Endgame',    count: pattern.mistakes_endgame,    color: 'var(--red)' },
              ].map(({ label, count, color }) => {
                const pct = Math.round((count / totalMistakes) * 100)
                return (
                  <div key={label} style={{ marginBottom: 14 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4, fontSize: '0.8rem' }}>
                      <span style={{ fontWeight: focusPhase && label.toLowerCase() === focusPhase ? 600 : 400 }}>{label}</span>
                      <span style={{ color: 'var(--text-muted)', fontWeight: 600 }}>{count} <span style={{ fontWeight: 400, opacity: 0.7 }}>({pct}%)</span></span>
                    </div>
                    <div style={{ height: 7, background: 'var(--surface-2)', borderRadius: 4, overflow: 'hidden' }}>
                      <div style={{ height: '100%', width: `${pct}%`, background: color, borderRadius: 4, transition: 'width 0.4s' }} />
                    </div>
                  </div>
                )
              })}
              <div style={{ marginTop: 6, fontSize: '0.7rem', color: 'var(--text-muted)' }}>
                {pattern.games_analyzed} games · {totalMistakes} flagged
              </div>
            </div>
          ) : (
            <p style={{ color: 'var(--text-muted)', fontSize: '0.82rem' }}>
              Analyze games to see breakdown.
            </p>
          )}
        </div>

        <div className="card">
          <div style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: 14 }}>Best / Worst Win Rate</div>
          <BestWorstOpenings data={stats.opening_stats} />
        </div>

        <div className="card">
          <div style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: 8 }}>Recent Games</div>
          <RecentGames games={stats.recent_games} />
        </div>
      </div>

    </div>
  )
}
