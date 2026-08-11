import { useMemo } from 'react';
import { CssBaseline, ThemeProvider } from '@mui/material';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { RouterProvider } from 'react-router-dom';
import { router } from '@/router/router';
import { darkTheme, lightTheme } from '@/theme/theme';
import { useSettings } from '@/hooks/useMisc';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 30 * 1000,
    },
  },
});

function ThemedApp() {
  const { data: settings } = useSettings();
  const theme = useMemo(() => (settings?.theme === 'LIGHT' ? lightTheme : darkTheme), [settings?.theme]);

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <RouterProvider router={router} />
    </ThemeProvider>
  );
}

export function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ThemedApp />
    </QueryClientProvider>
  );
}
