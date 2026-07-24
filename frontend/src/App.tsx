import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Layout } from './components/Layout'
import { Dashboard } from './pages/Dashboard'
import { GameList } from './pages/GameList'
import { GameAnalysis } from './pages/GameAnalysis'
import { PatternReport } from './pages/PatternReport'
import { TrainingPlan } from './pages/TrainingPlan'
import { Insights } from './pages/Insights'
import { Drills } from './pages/Drills'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      retry: 1,
    },
  },
})

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route index element={<Dashboard />} />
            <Route path="games" element={<GameList />} />
            <Route path="games/:id" element={<GameAnalysis />} />
            <Route path="insights" element={<Insights />} />
            <Route path="drills" element={<Drills />} />
            <Route path="patterns" element={<PatternReport />} />
            <Route path="training" element={<TrainingPlan />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}
