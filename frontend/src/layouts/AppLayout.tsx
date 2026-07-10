import { Outlet } from 'react-router-dom';
import { Sidebar } from '@/components/Sidebar';

export function AppLayout() {
  return (
    <div className="flex min-h-screen bg-paper">
      <Sidebar />
      <main className="flex-1 min-w-0">
        <div className="max-w-6xl mx-auto px-8 py-8">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
