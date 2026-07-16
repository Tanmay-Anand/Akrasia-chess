import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef } from 'react'
import { api } from '../api/client'

export function useAnalysisProgress() {
  const queryClient = useQueryClient()
  const wasPatternGenerating = useRef(false)
  const forcePollUntil = useRef(0)

  const startWarmup = () => { forcePollUntil.current = Date.now() + 20_000 }

  const query = useQuery({
    queryKey: ['analysis-progress'],
    queryFn: api.analysis.progress,
    refetchInterval: (q) => {
      const d = q.state.data
      if (d?.running || d?.pattern_generating || d?.queued) return 2000
      if (Date.now() < forcePollUntil.current) return 2000
      return false
    },
  })

  useEffect(() => {
    const d = query.data
    if (!d) return

    // When pattern generation finishes, refresh data across all tabs
    if (wasPatternGenerating.current && !d.pattern_generating && !d.running) {
      queryClient.invalidateQueries({ queryKey: ['dashboard-stats'] })
      queryClient.invalidateQueries({ queryKey: ['games'] })
      queryClient.invalidateQueries({ queryKey: ['patterns'] })
      queryClient.invalidateQueries({ queryKey: ['sync-status'] })
    }

    wasPatternGenerating.current = d.pattern_generating
  }, [query.data, queryClient])

  return { ...query, startWarmup }
}
