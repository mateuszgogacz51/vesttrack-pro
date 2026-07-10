import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from '@/auth/AuthContext';
import { ProtectedRoute } from '@/auth/ProtectedRoute';
import { AppLayout } from '@/layouts/AppLayout';

import { LoginPage } from '@/pages/auth/LoginPage';
import { RegisterPage } from '@/pages/auth/RegisterPage';
import { ForgotPasswordPage } from '@/pages/auth/ForgotPasswordPage';
import { ResetPasswordPage } from '@/pages/auth/ResetPasswordPage';

import { DashboardPage } from '@/pages/dashboard/DashboardPage';
import { AccountDetailPage } from '@/pages/accounts/AccountDetailPage';
import { TicketsPage } from '@/pages/tickets/TicketsPage';

import { EmployeeTicketsPage } from '@/pages/employee/EmployeeTicketsPage';
import { EmployeeInstrumentsPage } from '@/pages/employee/EmployeeInstrumentsPage';

import { AdminTeamPage } from '@/pages/admin/AdminTeamPage';
import { AdminAuditPage } from '@/pages/admin/AdminAuditPage';
import { AdminApiUsagePage } from '@/pages/admin/AdminApiUsagePage';

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          <Route path="/reset-password" element={<ResetPasswordPage />} />

          <Route element={<ProtectedRoute allowedRoles={['USER', 'EMPLOYEE', 'ADMIN']} />}>
            <Route element={<AppLayout />}>
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/accounts/:accountId" element={<AccountDetailPage />} />
              <Route path="/tickets" element={<TicketsPage />} />

              <Route element={<ProtectedRoute allowedRoles={['EMPLOYEE', 'ADMIN']} />}>
                <Route path="/employee/tickets" element={<EmployeeTicketsPage />} />
                <Route path="/employee/instruments" element={<EmployeeInstrumentsPage />} />
              </Route>

              <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
                <Route path="/admin/team" element={<AdminTeamPage />} />
                <Route path="/admin/audit" element={<AdminAuditPage />} />
                <Route path="/admin/api-usage" element={<AdminApiUsagePage />} />
              </Route>
            </Route>
          </Route>

          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
