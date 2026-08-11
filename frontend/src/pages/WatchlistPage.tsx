import { Box, List, Typography } from '@mui/material';
import { useWatchlist } from '@/hooks/useWatchlist';
import { QueryStateBoundary } from '@/components/QueryStateBoundary';
import { CompanyListItem } from '@/components/CompanyListItem';

export function WatchlistPage() {
  const { data, isLoading, isError, error } = useWatchlist();

  return (
    <Box>
      <Typography variant="h5" fontWeight={700} sx={{ mb: 3 }}>
        Watchlist
      </Typography>

      <QueryStateBoundary isLoading={isLoading} isError={isError} error={error}>
        {data && data.length === 0 && (
          <Typography variant="body2" color="text.secondary">
            Your watchlist is empty. Add companies from the Stock Explorer or a company's page.
          </Typography>
        )}
        <List disablePadding>
          {data?.map((c) => (
            <CompanyListItem key={c.id} company={c} />
          ))}
        </List>
      </QueryStateBoundary>
    </Box>
  );
}
