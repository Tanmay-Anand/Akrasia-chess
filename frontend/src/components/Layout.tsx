import { NavLink, Outlet } from 'react-router-dom'
import { SyncStatusBanner } from './SyncStatusBanner'

export function Layout() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <nav style={{
        background: 'var(--surface)',
        borderBottom: '1px solid var(--border)',
        padding: '0 24px',
        display: 'flex',
        alignItems: 'center',
        gap: 32,
        height: 52,
        position: 'sticky',
        top: 0,
        zIndex: 100,
      }}>
        <span style={{ fontWeight: 700, fontSize: '1rem', color: 'var(--accent)', marginRight: 8 }}>
          ♟ Praxis
        </span>
        {[
          { to: '/', label: 'Dashboard' },
          { to: '/games', label: 'Games' },
          { to: '/patterns', label: 'Patterns' },
          { to: '/training', label: 'Training Plan' },
        ].map(({ to, label }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            style={({ isActive }) => ({
              color: isActive ? 'var(--text)' : 'var(--text-muted)',
              fontWeight: isActive ? 600 : 400,
              fontSize: '0.875rem',
              borderBottom: isActive ? '2px solid var(--accent)' : '2px solid transparent',
              paddingBottom: 2,
            })}
          >
            {label}
          </NavLink>
        ))}
      </nav>
      <SyncStatusBanner />
      <main style={{ flex: 1, padding: '24px', maxWidth: 1280, width: '100%', margin: '0 auto' }}>
        <Outlet />
      </main>
    </div>
  )
}
