import { useEffect, useRef, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import { useSyncStatus } from '../hooks/useSyncStatus'
import { useAnalysisProgress } from '../hooks/useAnalysisProgress'

function fmtEta(secs: number): string {
  if (secs < 0) return ''
  if (secs < 60) return `~${secs}s`
  const m = Math.round(secs / 60)
  return `~${m} min`
}

export function SyncStatusBanner() {
  const { data: status } = useSyncStatus()
  const { data: progress, startWarmup } = useAnalysisProgress()
  const queryClient = useQueryClient()
  const [toast, setToast] = useState<string | null>(null)
  const toastTimer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const wasRunning = useRef(false)

  // Show toast when analysis pipeline finishes
  useEffect(() => {
    if (!progress) return
    const justFinished = wasRunning.current && !progress.running && !progress.pattern_generating
    if (justFinished) {
      setToast('Analysis complete — Pattern Report updated')
      if (toastTimer.current) clearTimeout(toastTimer.current)
      toastTimer.current = setTimeout(() => setToast(null), 5000)
    }
    wasRunning.current = progress.running || progress.pattern_generating
  }, [progress])

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['sync-status'] })
    queryClient.invalidateQueries({ queryKey: ['games'] })
    queryClient.invalidateQueries({ queryKey: ['dashboard-stats'] })
    queryClient.invalidateQueries({ queryKey: ['analysis-progress'] })
  }

  const syncMutation = useMutation({
    mutationFn: () => api.sync.trigger(3),
    onSuccess: () => { startWarmup(); invalidate() },
  })

  const forceResyncMutation = useMutation({
    mutationFn: () => api.sync.forceResync(3),
    onSuccess: () => { startWarmup(); invalidate() },
  })

  const isAnalyzing = progress?.running || progress?.pattern_generating
  const isActive = status?.state === 'SYNCING' || isAnalyzing
  const isBusy = isActive || syncMutation.isPending || forceResyncMutation.isPending

  const pct = progress?.percent_complete ?? 0
  const eta = progress ? fmtEta(progress.eta_seconds) : ''

  return (
    <>
      <div style={{
        background: isActive ? 'var(--accent-dim)' : 'var(--surface)',
        borderBottom: '1px solid var(--border)',
        padding: '6px 24px',
        display: 'flex',
        flexDirection: 'column',
        gap: 6,
        fontSize: '0.78rem',
        color: 'var(--text-muted)',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <span style={{ flex: 1 }}>
            {status?.state === 'SYNCING' && '⟳ Fetching games from Chess.com...'}

            {progress?.running && (
              <>
                <span style={{ color: 'var(--accent)', fontWeight: 600 }}>
                  ⟳ Analyzing {progress.completed} / {progress.total} games
                </span>
                {eta && <span style={{ marginLeft: 8 }}>{eta} remaining</span>}
              </>
            )}

            {progress?.pattern_generating && !progress.running && (
              <span style={{ color: 'var(--yellow)' }}>
                ⟳ Generating Pattern Report...
              </span>
            )}

            {!isActive && status?.state === 'IDLE' && (
              `${status?.games_analyzed ?? 0} games analyzed · Last sync: ${
                status?.last_synced_at === 'Never'
                  ? 'Never'
                  : new Date(status.last_synced_at).toLocaleString()
              }`
            )}

            {!status && 'Loading...'}
          </span>

          <div style={{ display: 'flex', gap: 8 }}>
            <button
              onClick={() => forceResyncMutation.mutate()}
              disabled={isBusy}
              className="secondary"
              style={{ padding: '4px 10px', fontSize: '0.72rem' }}
              title="Re-fetch last 3 months from Chess.com and update accuracy data"
            >
              {forceResyncMutation.isPending ? 'Re-syncing...' : '↻ Re-Sync'}
            </button>
            <button
              onClick={() => syncMutation.mutate()}
              disabled={isBusy}
              style={{ padding: '4px 12px', fontSize: '0.75rem' }}
            >
              {syncMutation.isPending ? 'Syncing...' : 'Sync Now'}
            </button>
          </div>
        </div>

        {/* Progress bar */}
        {progress?.running && (
          <div style={{ height: 3, background: 'var(--surface-2)', borderRadius: 2, overflow: 'hidden' }}>
            <div style={{
              height: '100%',
              width: `${pct}%`,
              background: 'var(--accent)',
              borderRadius: 2,
              transition: 'width 0.4s ease',
            }} />
          </div>
        )}
      </div>

      {/* Bottom-right toast */}
      {toast && (
        <div
          onClick={() => setToast(null)}
          style={{
            position: 'fixed',
            bottom: 24,
            right: 24,
            zIndex: 9999,
            background: 'var(--surface)',
            border: '1px solid var(--accent)',
            borderRadius: 8,
            padding: '12px 18px',
            fontSize: '0.82rem',
            color: 'var(--text)',
            boxShadow: '0 4px 20px rgba(0,0,0,0.4)',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: 10,
            maxWidth: 320,
            animation: 'slideIn 0.2s ease',
          }}
        >
          <span style={{ fontSize: '1rem' }}>✓</span>
          <span>{toast}</span>
        </div>
      )}

      <style>{`
        @keyframes slideIn {
          from { transform: translateY(12px); opacity: 0; }
          to   { transform: translateY(0);   opacity: 1; }
        }
      `}</style>
    </>
  )
}
