import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import type { TrainingPlanJson, TrainingPriority } from '../api/types'

function PriorityCard({ num, priority }: { num: number; priority: TrainingPriority }) {
  const colors = ['var(--red)', 'var(--yellow)', 'var(--accent)']
  const color = colors[num - 1] ?? 'var(--accent)'

  return (
    <div className="card">
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
        <span style={{
          background: color, color: '#0f1117', borderRadius: '50%',
          width: 26, height: 26, display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontWeight: 700, fontSize: '0.8rem', flexShrink: 0,
        }}>{num}</span>
        <h3 style={{ fontSize: '0.95rem', fontWeight: 600 }}>{priority.focus}</h3>
      </div>
      <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)', marginBottom: 8, lineHeight: 1.5 }}>
        <strong style={{ color: 'var(--text)' }}>Action:</strong> {priority.action}
      </p>
      <p style={{ fontSize: '0.82rem', color: 'var(--text-muted)', lineHeight: 1.5 }}>
        <strong style={{ color: 'var(--text)' }}>Why:</strong> {priority.reason}
      </p>
    </div>
  )
}

export function TrainingPlan() {
  const queryClient = useQueryClient()

  const { data: plan, isLoading } = useQuery({
    queryKey: ['training-plan'],
    queryFn: api.trainingPlan.latest,
  })

  const generateMutation = useMutation({
    mutationFn: api.trainingPlan.generate,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['training-plan'] }),
  })

  const parsed: TrainingPlanJson | null = (() => {
    if (!plan?.plan_json) return null
    try { return JSON.parse(plan.plan_json) } catch { return null }
  })()

  const openings: string[] = (() => {
    if (!plan?.openings_to_drill) return []
    try { return JSON.parse(plan.openings_to_drill) } catch { return [] }
  })()

  const tactics: string[] = (() => {
    if (!plan?.tactical_patterns) return []
    try { return JSON.parse(plan.tactical_patterns) } catch { return [] }
  })()

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
        <h1 style={{ fontSize: '1.3rem', fontWeight: 700 }}>Training Plan</h1>
        <button
          onClick={() => generateMutation.mutate()}
          disabled={generateMutation.isPending}
          style={{ marginLeft: 'auto' }}
        >
          {generateMutation.isPending ? 'Generating...' : 'Regenerate Plan'}
        </button>
      </div>

      {isLoading && <p style={{ color: 'var(--text-muted)' }}>Loading...</p>}

      {!isLoading && !plan && (
        <div className="card">
          <p style={{ color: 'var(--text-muted)', marginBottom: 12 }}>
            No training plan yet. Analyze your games first, then generate a plan.
          </p>
          <button onClick={() => generateMutation.mutate()} disabled={generateMutation.isPending}>
            {generateMutation.isPending ? 'Generating...' : 'Generate First Plan'}
          </button>
          {generateMutation.isError && (
            <p style={{ color: 'var(--red)', marginTop: 10, fontSize: '0.82rem' }}>
              {(generateMutation.error as Error).message}
            </p>
          )}
        </div>
      )}

      {plan && (
        <>
          <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
            Generated {new Date(plan.generated_at).toLocaleString()} · Based on {plan.based_on_games} games
          </p>

          {parsed && (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))', gap: 16 }}>
              {[1, 2, 3].map((n) => {
                const priority = parsed[`priority_${n}` as keyof TrainingPlanJson] as TrainingPriority
                return priority ? <PriorityCard key={n} num={n} priority={priority} /> : null
              })}
            </div>
          )}

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
            {openings.length > 0 && (
              <div className="card">
                <h2 style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: 12 }}>
                  Openings to Drill
                </h2>
                <ul style={{ listStyle: 'none' }}>
                  {openings.map((o, i) => (
                    <li key={i} style={{ fontSize: '0.85rem', padding: '6px 0', borderBottom: '1px solid var(--border)', color: 'var(--text)' }}>
                      {o}
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {tactics.length > 0 && (
              <div className="card">
                <h2 style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: 12 }}>
                  Tactical Patterns to Study
                </h2>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
                  {tactics.map((t, i) => (
                    <span key={i} className="badge" style={{ background: 'var(--surface-2)', color: 'var(--text)', border: '1px solid var(--border)', fontSize: '0.78rem', padding: '4px 10px' }}>
                      {t}
                    </span>
                  ))}
                </div>
              </div>
            )}
          </div>
        </>
      )}
    </div>
  )
}
