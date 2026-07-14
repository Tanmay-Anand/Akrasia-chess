import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'

export function useSyncStatus() {
  return useQuery({
    queryKey: ['sync-status'],
    queryFn: api.sync.status,
    refetchInterval: (query) => {
      const state = query.state.data?.state
      return state === 'SYNCING' || state === 'ANALYZING' ? 3000 : false
    },
  })
}
