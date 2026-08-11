import { Box, List, Stack, Typography } from '@mui/material';
import Grid from '@mui/material/Grid2';
import { useParams, useNavigate } from 'react-router-dom';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { IconButton } from '@mui/material';
import { useSector } from '@/hooks/useMisc';
import { QueryStateBoundary } from '@/components/QueryStateBoundary';
import { CompanyListItem } from '@/components/CompanyListItem';
import { ScoreBadge } from '@/components/ScoreBadge';

export function SectorDetailPage() {
  const { sector } = useParams<{ sector: string }>();
  const navigate = useNavigate();
  const { data, isLoading, isError, error } = useSector(sector);

  return (
    <Box>
      <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 3 }}>
        <IconButton onClick={() => navigate('/sectors')} size="small">
          <ArrowBackIcon fontSize="small" />
        </IconButton>
        <Typography variant="h5" fontWeight={700}>
          {sector} Sector
        </Typography>
      </Stack>

      <QueryStateBoundary isLoading={isLoading} isError={isError} error={error}>
        {data && (
          <Stack spacing={3}>
            <Grid container spacing={2}>
              <Grid size={{ xs: 6, sm: 3 }}>
                <Stack alignItems="center" spacing={1}>
                  <ScoreBadge score={data.averageScore} size={56} />
                  <Typography variant="caption" color="text.secondary">
                    Average Score
                  </Typography>
                </Stack>
              </Grid>
              <Grid size={{ xs: 6, sm: 3 }}>
                <Typography variant="caption" color="text.secondary">
                  Avg Revenue Growth
                </Typography>
                <Typography variant="h6" fontWeight={700}>
                  {data.averageRevenueGrowthPct !== null ? `${data.averageRevenueGrowthPct}%` : '—'}
                </Typography>
              </Grid>
              <Grid size={{ xs: 6, sm: 3 }}>
                <Typography variant="caption" color="text.secondary">
                  Avg Profit Growth
                </Typography>
                <Typography variant="h6" fontWeight={700}>
                  {data.averageProfitGrowthPct !== null ? `${data.averageProfitGrowthPct}%` : '—'}
                </Typography>
              </Grid>
              <Grid size={{ xs: 6, sm: 3 }}>
                <Typography variant="caption" color="text.secondary">
                  Recent News
                </Typography>
                <Typography variant="h6" fontWeight={700}>
                  {data.recentNewsCount}
                </Typography>
              </Grid>
            </Grid>

            <Box>
              <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 1.5 }}>
                Best Performing Stocks
              </Typography>
              <List disablePadding>
                {data.bestPerforming.length === 0 && (
                  <Typography variant="body2" color="text.secondary">
                    No scored companies in this sector yet.
                  </Typography>
                )}
                {data.bestPerforming.map((c) => (
                  <CompanyListItem key={c.id} company={c} />
                ))}
              </List>
            </Box>
          </Stack>
        )}
      </QueryStateBoundary>
    </Box>
  );
}
