import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { ProtectedRoute } from './components/ProtectedRoute'
import { AppShell } from './components/AppShell'
import { LoginPage } from './pages/LoginPage'
import { DashboardPage } from './pages/DashboardPage'
import { PacientesPage } from './pages/PacientesPage'
import { AgendaPage } from './pages/AgendaPage'
import { CajaPage } from './pages/CajaPage'
import { FinanzasPage } from './pages/FinanzasPage'
import { ConfiguracionPage } from './pages/ConfiguracionPage'
import { FacturacionPage } from './pages/FacturacionPage'
import { AyudaPage } from './pages/AyudaPage'

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />

          <Route element={<ProtectedRoute />}>
            <Route element={<AppShell />}>
              <Route path="/" element={<ProtectedRoute roles={['ADMIN', 'COORDINADOR']} />}>
                <Route index element={<DashboardPage />} />
              </Route>
              <Route path="/pacientes" element={<PacientesPage />} />
              <Route path="/agenda" element={<AgendaPage />} />
              <Route path="/caja" element={<CajaPage />} />
              <Route path="/finanzas" element={<FinanzasPage />} />
              <Route path="/configuracion" element={<ConfiguracionPage />} />
              <Route path="/facturacion" element={<FacturacionPage />} />
              <Route path="/ayuda" element={<AyudaPage />} />
            </Route>
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  )
}
