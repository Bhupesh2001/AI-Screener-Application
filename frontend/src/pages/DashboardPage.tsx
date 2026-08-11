import {
  Box,
  Card,
  CardContent,
  Chip,
  List,
  Stack,
  Typography,
} from '@mui/material';
import Grid from '@mui/material/Grid2';
import { useDashboard } from '@/hooks/useDashboard';
import { QueryStateBoundary } from '@/components/QueryStateBoundary';
import { CompanyListItem } from '@/components/CompanyListItem';
import { EventListItem } from '@/components/EventListItem';
import { useNavigate } from 'react-router-dom';

function StatCard({ label, value }: { label: string; value: string | number }) {
  return (
    <Card variant="outlined" sx={{ height: '100%' }}>
      <CardContent>
        <Typography variant="caption" color="text.secondary">
          {label}
        </Typography>
        <Typography variant="h5" fontWeight={700} sx={{ mt: 0.5 }}>
          {value}
        </Typography>
      </CardContent>
    </Card>
  );
}

function sectorHeatColor(score: number): string {
  if (score >= 70) return '#48BB78';
  if (score >= 55) return '#4FD1C5';
  if (score >= 40) return '#ECC94B';
  return '#8B98A5';
}

export function DashboardPage() {
  const { data, isLoading, isError, error } = useDashboard();
  const navigate = useNavigate();

  return (
    <Box>
      <Typography variant="h5" fontWeight={700} sx={{ mb: 3 }}>
        Dashboard
      </Typography>

      <QueryStateBoundary isLoading={isLoading} isError={isError} error={error}>
        {data && (
          <Stack spacing={3}>
            {/* Market Overview */}
            <Grid container spacing={2}>
              <Grid size={{ xs: 6, sm: 3 }}>
                <StatCard label="Companies Tracked" value={data.marketOverview.totalCompaniesTracked} />
              </Grid>
              <Grid size={{ xs: 6, sm: 3 }}>
                <StatCard label="Events (7 days)" value={data.marketOverview.totalEventsLast7Days} />
              </Grid>
              <Grid size={{ xs: 6, sm: 3 }}>
                <StatCard label="Above Threshold" value={data.marketOverview.companiesAboveThreshold} />
              </Grid>
              <Grid size={{ xs: 6, sm: 3 }}>
                <StatCard label="Top Sector" value={data.marketOverview.topSector ?? '—'} />
              </Grid>
            </Grid>

            <Grid container spacing={3}>
              {/* Top Scoring Stocks */}
              <Grid size={{ xs: 12, md: 6 }}>
                <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 1.5 }}>
                  Top Scoring Stocks
                </Typography>
                <List disablePadding>
                  {data.topScoring.length === 0 && (
                    <Typography variant="body2" color="text.secondary">
                      No scored companies yet.
                    </Typography>
                  )}
                  {data.topScoring.map((c) => (
                    <CompanyListItem key={c.id} company={c} />
                  ))}
                </List>
              </Grid>

              {/* Recently Improved */}
              <Grid size={{ xs: 12, md: 6 }}>
                <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 1.5 }}>
                  Recently Improved Scores
                </Typography>
                <List disablePadding>
                  {data.recentlyImproved.length === 0 && (
                    <Typography variant="body2" color="text.secondary">
                      No score improvements detected yet.
                    </Typography>
                  )}
                  {data.recentlyImproved.map((c) => (
                    <CompanyListItem key={c.id} company={c} />
                  ))}
                </List>
              </Grid>

              {/* Sector Heatmap */}
              <Grid size={{ xs: 12, md: 6 }}>
                <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 1.5 }}>
                  Sector Heatmap
                </Typography>
                <Stack direction="row" flexWrap="wrap" gap={1}>
                  {data.sectorHeatmap.length === 0 && (
                    <Typography variant="body2" color="text.secondary">
                      No sector data yet.
                    </Typography>
                  )}
                  {data.sectorHeatmap.map((s) => (
                    <Chip
                      key={s.sector}
                      label={`${s.sector} · ${s.averageScore} (${s.companyCount})`}
                      onClick={() => navigate(`/sectors/${encodeURIComponent(s.sector)}`)}
                      sx={{
                        bgcolor: sectorHeatColor(s.averageScore),
                        color: '#0B0F14',
                        fontWeight: 600,
                      }}
                    />
                  ))}
                </Stack>
              </Grid>

              {/* Watchlist */}
              <Grid size={{ xs: 12, md: 6 }}>
                <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 1.5 }}>
                  Watchlist
                </Typography>
                <List disablePadding>
                  {data.watchlist.length === 0 && (
                    <Typography variant="body2" color="text.secondary">
                      Your watchlist is empty. Add companies from the Stock Explorer.
                    </Typography>
                  )}
                  {data.watchlist.map((c) => (
                    <CompanyListItem key={c.id} company={c} />
                  ))}
                </List>
              </Grid>

              {/* Latest Events */}
              <Grid size={{ xs: 12 }}>
                <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 1.5 }}>
                  Latest Events
                </Typography>
                <List disablePadding>
                  {data.latestEvents.length === 0 && (
                    <Typography variant="body2" color="text.secondary">
                      No events detected yet.
                    </Typography>
                  )}
                  {data.latestEvents.map((e) => (
                    <EventListItem key={e.id} event={e} />
                  ))}
                </List>
              </Grid>
            </Grid>
          </Stack>
        )}
      </QueryStateBoundary>
    </Box>
  );
}
