import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'

export function useGameAnalysis(gameId: string | null) {
  return useQuery({
    queryKey: ['analysis', gameId],
    queryFn: () => api.analysis.moveErrors(gameId!),
    enabled: !!gameId,
  })
}
