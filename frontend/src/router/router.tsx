import { createBrowserRouter } from 'react-router-dom';
import { AppLayout } from '@/components/AppLayout';
import { DashboardPage } from '@/pages/DashboardPage';
import { StockExplorerPage } from '@/pages/StockExplorerPage';
import { WatchlistPage } from '@/pages/WatchlistPage';
import { EventCenterPage } from '@/pages/EventCenterPage';
import { SectorDashboardPage } from '@/pages/SectorDashboardPage';
import { SectorDetailPage } from '@/pages/SectorDetailPage';
import { SettingsPage } from '@/pages/SettingsPage';
import { CompanyDetailPage } from '@/pages/CompanyDetailPage';

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppLayout />,
    children: [
      { index: true, element: <DashboardPage /> },
      { path: 'explorer', element: <StockExplorerPage /> },
      { path: 'watchlist', element: <WatchlistPage /> },
      { path: 'events', element: <EventCenterPage /> },
      { path: 'sectors', element: <SectorDashboardPage /> },
      { path: 'sectors/:sector', element: <SectorDetailPage /> },
      { path: 'settings', element: <SettingsPage /> },
      { path: 'company/:id', element: <CompanyDetailPage /> },
    ],
  },
]);
