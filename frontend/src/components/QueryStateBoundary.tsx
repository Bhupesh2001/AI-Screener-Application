import { Alert, Box, CircularProgress } from '@mui/material';
import type { ReactNode } from 'react';

interface QueryStateBoundaryProps {
  isLoading: boolean;
  isError: boolean;
  error?: unknown;
  children: ReactNode;
}

export function QueryStateBoundary({ isLoading, isError, error, children }: QueryStateBoundaryProps) {
  if (isLoading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
        <CircularProgress size={28} />
      </Box>
    );
  }

  if (isError) {
    const message = error instanceof Error ? error.message : 'Something went wrong loading this data.';
    return (
      <Alert severity="error" sx={{ my: 2 }}>
        {message}
      </Alert>
    );
  }

  return <>{children}</>;
}
