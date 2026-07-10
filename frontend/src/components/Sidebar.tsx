import { NavLink } from 'react-router-dom';
import { useAuth } from '@/auth/AuthContext';

interface NavItem {
  to: string;
  label: string;
  icon: JSX.Element;
}

const icon = (d: string) => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7">
    <path d={d} strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const userNav: NavItem[] = [
  { to: '/dashboard', label: 'Pulpit', icon: icon('M4 13h6V4H4v9zm0 7h6v-5H4v5zm10 0h6V11h-6v9zm0-16v5h6V4h-6z') },
  { to: '/accounts', label: 'Rachunki', icon: icon('M3 7h18M3 7v11a2 2 0 002 2h14a2 2 0 002-2V7M3 7l3-4h12l3 4') },
  { to: '/tickets', label: 'Zgłoszenia', icon: icon('M8 10h8M8 14h5M21 12a9 9 0 11-4.5-7.79L21 3l-1 5.5') }
];

const employeeNav: NavItem[] = [
  { to: '/employee/tickets', label: 'Kolejka zgłoszeń', icon: icon('M8 10h8M8 14h5M21 12a9 9 0 11-4.5-7.79L21 3l-1 5.5') },
  { to: '/employee/instruments', label: 'Instrumenty', icon: icon('M4 4h16v4H4zM4 12h10M4 16h6') }
];

const adminNav: NavItem[] = [
  { to: '/admin/team', label: 'Zespół', icon: icon('M17 20h5v-2a4 4 0 00-3-3.87M9 20H4v-2a4 4 0 013-3.87m6-5.13a4 4 0 11-8 0 4 4 0 018 0zm6 3a4 4 0 10-8 0') },
  { to: '/admin/audit', label: 'Dziennik audytu', icon: icon('M9 5H5a2 2 0 00-2 2v12a2 2 0 002 2h12a2 2 0 002-2v-4M9 5v4h4M9 5l7-3 3 3-7 7') },
  { to: '/admin/api-usage', label: 'Limity API', icon: icon('M13 2L3 14h8l-1 8 10-12h-8l1-8z') }
];

export function Sidebar() {
  const { user, logout } = useAuth();

  let sections: { title: string; items: NavItem[] }[] = [{ title: 'Inwestor', items: userNav }];
  if (user?.role === 'EMPLOYEE') {
    sections = [...sections, { title: 'Pracownik', items: employeeNav }];
  }
  if (user?.role === 'ADMIN') {
    sections = [...sections, { title: 'Pracownik', items: employeeNav }, { title: 'Administrator', items: adminNav }];
  }

  return (
    <aside className="w-64 shrink-0 bg-ink text-white flex flex-col h-screen sticky top-0">
      <div className="px-6 py-6 flex items-center gap-2.5 border-b border-white/10">
        <div className="w-8 h-8 rounded-md bg-clay flex items-center justify-center font-display font-semibold text-ink">
          V
        </div>
        <span className="font-display text-lg tracking-tight">VestTrack Pro</span>
      </div>

      <nav className="flex-1 overflow-y-auto py-5 px-3 scrollbar-thin">
        {sections.map((section) => (
          <div key={section.title} className="mb-6">
            <p className="px-3 mb-2 text-[11px] uppercase tracking-wider text-white/40 font-medium">
              {section.title}
            </p>
            <div className="flex flex-col gap-0.5">
              {section.items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) =>
                    `flex items-center gap-3 px-3 py-2 rounded-md text-sm transition-colors ${
                      isActive ? 'bg-white/10 text-white' : 'text-white/65 hover:bg-white/5 hover:text-white'
                    }`
                  }
                >
                  {item.icon}
                  {item.label}
                </NavLink>
              ))}
            </div>
          </div>
        ))}
      </nav>

      <div className="p-4 border-t border-white/10">
        <div className="flex items-center gap-2.5 px-2 mb-2">
          <div className="w-8 h-8 rounded-full bg-brand-light flex items-center justify-center text-xs font-medium">
            {user?.email.slice(0, 2).toUpperCase()}
          </div>
          <div className="min-w-0">
            <p className="text-sm truncate">{user?.email}</p>
            <p className="text-xs text-white/40">{user?.role}</p>
          </div>
        </div>
        <button
          onClick={() => logout()}
          className="w-full text-left px-2 py-1.5 text-sm text-white/60 hover:text-white rounded-md hover:bg-white/5"
        >
          Wyloguj się
        </button>
      </div>
    </aside>
  );
}
