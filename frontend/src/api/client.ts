import type {
  AnalysisProgress,
  DashboardStats,
  GameSummary,
  MoveError,
  Pattern,
  RatingPoint,
  SyncStatus,
  TrainingPlan,
} from './types'


const BASE = '/api'

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  if (!res.ok) {
    const text = await res.text()
    throw new Error(`API error ${res.status}: ${text}`)
  }
  if (res.status === 204) return null as T
  return res.json() as Promise<T>
}

export const api = {
  sync: {
    trigger: (months = 1) =>
      request<{ message: string; new_games: number }>('/sync', {
        method: 'POST',
        body: JSON.stringify({ months }),
      }),
    forceResync: (months = 3) =>
      request<{ message: string; gamesUpdated: number }>('/sync/force-resync', {
        method: 'POST',
        body: JSON.stringify({ months }),
      }),
    status: () => request<SyncStatus>('/sync/status'),
  },

  games: {
    list: () => request<GameSummary[]>('/games'),
    get: (id: string) => request<GameSummary>(`/games/${id}`),
    reanalyze: (id: string) =>
      request<{ message: string }>(`/games/${id}/analyze`, { method: 'POST' }),
  },

  dashboard: {
    stats: () => request<DashboardStats>('/dashboard/stats'),
    ratingHistory: () => request<RatingPoint[]>('/dashboard/rating-history'),
  },

  analysis: {
    moveErrors: (gameId: string) => request<MoveError[]>(`/analysis/${gameId}`),
    reanalyzeAll: () =>
      request<{ message: string; games_queued: number }>('/analysis/reanalyze', { method: 'POST' }),
    progress: () => request<AnalysisProgress>('/analysis/progress'),
  },

  patterns: {
    latest: () => request<Pattern | null>('/patterns'),
  },

  trainingPlan: {
    latest: () => request<TrainingPlan | null>('/training-plan'),
    generate: () =>
      request<TrainingPlan>('/training-plan/generate', { method: 'POST' }),
  },
}
