export type AnalysisStatus = 'PENDING' | 'ANALYZING' | 'ANALYZED' | 'FAILED'
export type Severity = 'BLUNDER' | 'MISTAKE' | 'INACCURACY'
export type TacticalMotif = 'FORK' | 'PIN' | 'SKEWER' | 'BACK_RANK' | 'DISCOVERED_ATTACK' | 'HANGING_PIECE' | 'POSITIONAL' | 'OTHER'
export type GamePhase = 'OPENING' | 'MIDDLEGAME' | 'ENDGAME'

export interface GameSummary {
  id: string
  chess_com_id: string
  played_at: string
  time_class: string
  time_control: string
  player_color: string
  result: string
  opening_eco: string | null
  opening_name: string | null
  analysis_status: AnalysisStatus
  player_rating: number
}

export interface MoveError {
  id: string
  move_number: number
  move_played: string
  better_move: string | null
  fen_position: string
  severity: Severity
  tactical_motif: TacticalMotif | null
  explanation: string | null
  game_phase: GamePhase | null
  clock_remaining: number | null
  analysis_failed: boolean
}

export interface SyncStatus {
  state: 'IDLE' | 'SYNCING' | 'ANALYZING'
  games_fetched: number
  games_analyzed: number
  games_pending: number
  last_synced_at: string
}

export interface RatingPoint {
  date: string
  rating: number
}

export interface DashboardStats {
  total_games: number
  wins: number
  losses: number
  draws: number
  games_analyzed: number
  opening_distribution: Record<string, number>
  rating_history: RatingPoint[]
}

export interface Pattern {
  id: string
  games_analyzed: number
  computed_at: string
  mistakes_moves1to10: number
  mistakes_moves11to20: number
  mistakes_moves21to30: number
  mistakes_moves31_plus: number
  mistakes_opening: number
  mistakes_middlegame: number
  mistakes_endgame: number
  motif_frequency: string | null
  opening_accuracy: string | null
  primary_weakness: string | null
  secondary_weakness: string | null
  tertiary_weakness: string | null
  critical_move_range: string | null
  dominant_motif: string | null
  opening_assessment: string | null
}

export interface TrainingPriority {
  focus: string
  action: string
  reason: string
}

export interface TrainingPlanJson {
  priority_1: TrainingPriority
  priority_2: TrainingPriority
  priority_3: TrainingPriority
  openings_to_drill: string[]
  tactical_patterns_to_study: string[]
}

export interface TrainingPlan {
  id: string
  generated_at: string
  based_on_games: number
  plan_json: string
  openings_to_drill: string | null
  tactical_patterns: string | null
}
