import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'

export function usePatternReport() {
  return useQuery({
    queryKey: ['patterns'],
    queryFn: api.patterns.latest,
  })
}
