import { Box, Card, CardActionArea, CardContent, Stack, Typography } from '@mui/material';
import Grid from '@mui/material/Grid2';
import { useNavigate } from 'react-router-dom';
import { useSectors } from '@/hooks/useMisc';
import { QueryStateBoundary } from '@/components/QueryStateBoundary';
import { ScoreBadge } from '@/components/ScoreBadge';

export function SectorDashboardPage() {
  const { data, isLoading, isError, error } = useSectors();
  const navigate = useNavigate();

  return (
    <Box>
      <Typography variant="h5" fontWeight={700} sx={{ mb: 3 }}>
        Sector Dashboard
      </Typography>

      <QueryStateBoundary isLoading={isLoading} isError={isError} error={error}>
        <Grid container spacing={2}>
          {data?.map((s) => (
            <Grid key={s.sector} size={{ xs: 12, sm: 6, md: 4 }}>
              <Card variant="outlined">
                <CardActionArea onClick={() => navigate(`/sectors/${encodeURIComponent(s.sector)}`)}>
                  <CardContent>
                    <Stack direction="row" justifyContent="space-between" alignItems="flex-start">
                      <Box>
                        <Typography variant="subtitle1" fontWeight={600}>
                          {s.sector}
                        </Typography>
                        <Typography variant="caption" color="text.secondary">
                          {s.bestPerforming.length} companies tracked
                        </Typography>
                      </Box>
                      <ScoreBadge score={s.averageScore} size={40} />
                    </Stack>

                    <Stack direction="row" spacing={3} sx={{ mt: 2 }}>
                      <Box>
                        <Typography variant="caption" color="text.secondary">
                          Avg Revenue Growth
                        </Typography>
                        <Typography variant="body2" fontWeight={600}>
                          {s.averageRevenueGrowthPct !== null ? `${s.averageRevenueGrowthPct}%` : '—'}
                        </Typography>
                      </Box>
                      <Box>
                        <Typography variant="caption" color="text.secondary">
                          Highest Score
                        </Typography>
                        <Typography variant="body2" fontWeight={600}>
                          {s.highestScore || '—'}
                        </Typography>
                      </Box>
                      <Box>
                        <Typography variant="caption" color="text.secondary">
                          Recent News
                        </Typography>
                        <Typography variant="body2" fontWeight={600}>
                          {s.recentNewsCount}
                        </Typography>
                      </Box>
                    </Stack>
                  </CardContent>
                </CardActionArea>
              </Card>
            </Grid>
          ))}
        </Grid>
      </QueryStateBoundary>
    </Box>
  );
}
