import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import { useSyncStatus } from '../hooks/useSyncStatus'

export function SyncStatusBanner() {
  const { data: status } = useSyncStatus()
  const queryClient = useQueryClient()

  const syncMutation = useMutation({
    mutationFn: () => api.sync.trigger(3),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['sync-status'] })
      queryClient.invalidateQueries({ queryKey: ['games'] })
    },
  })

  const isActive = status?.state === 'SYNCING' || status?.state === 'ANALYZING'

  return (
    <div style={{
      background: isActive ? 'var(--accent-dim)' : 'var(--surface)',
      borderBottom: '1px solid var(--border)',
      padding: '6px 24px',
      display: 'flex',
      alignItems: 'center',
      gap: 16,
      fontSize: '0.78rem',
      color: 'var(--text-muted)',
    }}>
      <span>
        {status?.state === 'SYNCING' && '⟳ Fetching games from Chess.com...'}
        {status?.state === 'ANALYZING' && `⟳ Analyzing ${status.games_pending} games with AI...`}
        {status?.state === 'IDLE' && `${status?.games_analyzed ?? 0} games analyzed · Last sync: ${status?.last_synced_at === 'Never' ? 'Never' : new Date(status.last_synced_at).toLocaleString()}`}
        {!status && 'Loading...'}
      </span>
      <button
        onClick={() => syncMutation.mutate()}
        disabled={isActive || syncMutation.isPending}
        style={{ marginLeft: 'auto', padding: '4px 12px', fontSize: '0.75rem' }}
      >
        {isActive ? 'Syncing...' : 'Sync Now'}
      </button>
    </div>
  )
}
