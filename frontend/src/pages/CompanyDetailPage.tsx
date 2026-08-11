import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  IconButton,
  List,
  Stack,
  Tab,
  Tabs,
  Typography,
} from '@mui/material';
import Grid from '@mui/material/Grid2';
import { useState } from 'react';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import BookmarkIcon from '@mui/icons-material/Bookmark';
import BookmarkBorderIcon from '@mui/icons-material/BookmarkBorder';
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome';
import { useCompanyDetail, useGenerateResearch, useScoreChange, useWhyInteresting } from '@/hooks/useCompany';
import { useAddToWatchlist, useRemoveFromWatchlist } from '@/hooks/useWatchlist';
import { QueryStateBoundary } from '@/components/QueryStateBoundary';
import { ScoreBadge } from '@/components/ScoreBadge';
import { ScoreChangeChip } from '@/components/ScoreChangeChip';
import { WhyInterestingCard } from '@/components/WhyInterestingCard';
import { EventListItem } from '@/components/EventListItem';

function FinancialStat({ label, value }: { label: string; value: string }) {
  return (
    <Box>
      <Typography variant="caption" color="text.secondary">
        {label}
      </Typography>
      <Typography variant="body1" fontWeight={600}>
        {value}
      </Typography>
    </Box>
  );
}

function pct(v: number | null): string {
  return v !== null ? `${v}%` : '—';
}

function num(v: number | null, prefix = ''): string {
  return v !== null ? `${prefix}${v}` : '—';
}

export function CompanyDetailPage() {
  const { id } = useParams<{ id: string }>();
  const companyId = id ? Number(id) : undefined;
  const navigate = useNavigate();
  const [tab, setTab] = useState(0);

  const { data: company, isLoading, isError, error } = useCompanyDetail(companyId);
  const { data: scoreChange } = useScoreChange(companyId);
  const { data: whyInteresting } = useWhyInteresting(companyId);
  const addToWatchlist = useAddToWatchlist();
  const removeFromWatchlist = useRemoveFromWatchlist();
  const generateResearch = useGenerateResearch(companyId ?? -1);

  const toggleWatchlist = () => {
    if (!companyId || !company) return;
    if (company.onWatchlist) {
      removeFromWatchlist.mutate(companyId);
    } else {
      addToWatchlist.mutate({ companyId });
    }
  };

  return (
    <Box>
      <Stack direction="row" alignItems="center" spacing={1} sx={{ mb: 2 }}>
        <IconButton onClick={() => navigate(-1)} size="small">
          <ArrowBackIcon fontSize="small" />
        </IconButton>
      </Stack>

      <QueryStateBoundary isLoading={isLoading} isError={isError} error={error}>
        {company && (
          <Stack spacing={3}>
            {/* Header */}
            <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" spacing={2}>
              <Box>
                <Typography variant="h5" fontWeight={700}>
                  {company.name}
                </Typography>
                <Stack direction="row" spacing={1} alignItems="center" sx={{ mt: 0.5 }}>
                  <Typography variant="body2" color="text.secondary">
                    {company.symbol} · {company.exchange} · {company.sector}
                  </Typography>
                  {company.industry && (
                    <Chip label={company.industry} size="small" variant="outlined" />
                  )}
                </Stack>
              </Box>
              <Stack direction="row" spacing={1} alignItems="center">
                <Button
                  variant={company.onWatchlist ? 'outlined' : 'contained'}
                  startIcon={company.onWatchlist ? <BookmarkIcon /> : <BookmarkBorderIcon />}
                  onClick={toggleWatchlist}
                >
                  {company.onWatchlist ? 'On Watchlist' : 'Add to Watchlist'}
                </Button>
              </Stack>
            </Stack>

            {/* Score + Price summary */}
            <Grid container spacing={2}>
              <Grid size={{ xs: 6, sm: 3 }}>
                <Card variant="outlined">
                  <CardContent sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                    <ScoreBadge score={company.currentScore?.totalScore} size={48} />
                    <Box>
                      <Typography variant="caption" color="text.secondary">
                        Research Score
                      </Typography>
                      <Box>
                        <ScoreChangeChip change={scoreChange?.delta} />
                      </Box>
                    </Box>
                  </CardContent>
                </Card>
              </Grid>
              <Grid size={{ xs: 6, sm: 3 }}>
                <Card variant="outlined">
                  <CardContent>
                    <Typography variant="caption" color="text.secondary">
                      Current Price
                    </Typography>
                    <Typography variant="h6" fontWeight={700}>
                      {num(company.currentPrice, '₹')}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
              <Grid size={{ xs: 6, sm: 3 }}>
                <Card variant="outlined">
                  <CardContent>
                    <Typography variant="caption" color="text.secondary">
                      52W High / Low
                    </Typography>
                    <Typography variant="body2" fontWeight={600}>
                      {num(company.week52High, '₹')} / {num(company.week52Low, '₹')}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
              <Grid size={{ xs: 6, sm: 3 }}>
                <Card variant="outlined">
                  <CardContent>
                    <Typography variant="caption" color="text.secondary">
                      Market Cap
                    </Typography>
                    <Typography variant="body2" fontWeight={600}>
                      {company.marketCapCr !== null ? `₹${company.marketCapCr.toLocaleString()} Cr` : '—'}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
            </Grid>

            {/* Why Interesting card */}
            {whyInteresting && <WhyInterestingCard data={whyInteresting} />}

            {/* Score change reasoning */}
            {scoreChange && scoreChange.previousScore !== null && (
              <Card variant="outlined">
                <CardContent>
                  <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 1 }}>
                    Why did the score change?
                  </Typography>
                  <Stack direction="row" spacing={3} sx={{ mb: 1.5 }}>
                    <FinancialStat label="Previous Score" value={String(scoreChange.previousScore)} />
                    <FinancialStat label="Current Score" value={String(scoreChange.currentScore)} />
                  </Stack>
                  <Stack spacing={0.5}>
                    {scoreChange.reasons.slice(0, 6).map((r, idx) => (
                      <Typography key={idx} variant="body2" color={r.type === 'POSITIVE' ? 'success.main' : r.type === 'NEGATIVE' ? 'error.main' : 'text.secondary'}>
                        {r.type === 'POSITIVE' ? '+ ' : r.type === 'NEGATIVE' ? '− ' : '• '}
                        {r.text}
                      </Typography>
                    ))}
                  </Stack>
                </CardContent>
              </Card>
            )}

            {/* Tabs: Financials / Score Breakdown / Events / News / AI Research */}
            <Box>
              <Tabs value={tab} onChange={(_, v) => setTab(v)} variant="scrollable" scrollButtons="auto">
                <Tab label="Financials" />
                <Tab label="Score Breakdown" />
                <Tab label="Events" />
                <Tab label="News" />
                <Tab label="AI Research" />
              </Tabs>

              <Box sx={{ pt: 2 }}>
                {tab === 0 && (
                  <Grid container spacing={3}>
                    <Grid size={{ xs: 6, sm: 4, md: 3 }}>
                      <FinancialStat label="Revenue Growth" value={pct(company.revenueGrowthPct)} />
                    </Grid>
                    <Grid size={{ xs: 6, sm: 4, md: 3 }}>
                      <FinancialStat label="Profit Growth" value={pct(company.profitGrowthPct)} />
                    </Grid>
                    <Grid size={{ xs: 6, sm: 4, md: 3 }}>
                      <FinancialStat label="Operating Margin" value={pct(company.operatingMarginPct)} />
                    </Grid>
                    <Grid size={{ xs: 6, sm: 4, md: 3 }}>
                      <FinancialStat label="Debt to Equity" value={num(company.debtToEquity)} />
                    </Grid>
                    <Grid size={{ xs: 6, sm: 4, md: 3 }}>
                      <FinancialStat label="ROCE" value={pct(company.roce)} />
                    </Grid>
                    <Grid size={{ xs: 6, sm: 4, md: 3 }}>
                      <FinancialStat label="ROE" value={pct(company.roe)} />
                    </Grid>
                    <Grid size={{ xs: 6, sm: 4, md: 3 }}>
                      <FinancialStat label="PE Ratio" value={num(company.peRatio)} />
                    </Grid>
                    <Grid size={{ xs: 6, sm: 4, md: 3 }}>
                      <FinancialStat label="Operating Cash Flow" value={company.operatingCashFlowCr !== null ? `₹${company.operatingCashFlowCr} Cr` : '—'} />
                    </Grid>
                    <Grid size={{ xs: 6, sm: 4, md: 3 }}>
                      <FinancialStat label="Promoter Holding" value={pct(company.promoterHoldingPct)} />
                    </Grid>
                    <Grid size={{ xs: 6, sm: 4, md: 3 }}>
                      <FinancialStat label="Institutional Holding" value={pct(company.institutionalHoldingPct)} />
                    </Grid>
                  </Grid>
                )}

                {tab === 1 && company.currentScore && (
                  <Stack spacing={1.5}>
                    {Object.entries(company.currentScore.categoryScores).map(([category, score]) => (
                      <Box key={category}>
                        <Stack direction="row" justifyContent="space-between">
                          <Typography variant="body2">{category}</Typography>
                          <Typography variant="body2" fontWeight={600}>
                            {score}/100
                          </Typography>
                        </Stack>
                        <Box sx={{ height: 6, bgcolor: 'action.hover', borderRadius: 1, mt: 0.5, overflow: 'hidden' }}>
                          <Box
                            sx={{
                              height: '100%',
                              width: `${score}%`,
                              bgcolor: score >= 70 ? 'success.main' : score >= 40 ? 'warning.main' : 'error.main',
                            }}
                          />
                        </Box>
                      </Box>
                    ))}
                    <Typography variant="subtitle2" fontWeight={600} sx={{ mt: 2 }}>
                      All Reasons
                    </Typography>
                    {company.currentScore.reasons.map((r, idx) => (
                      <Typography
                        key={idx}
                        variant="body2"
                        color={r.type === 'POSITIVE' ? 'success.main' : r.type === 'NEGATIVE' ? 'error.main' : 'text.secondary'}
                      >
                        [{r.category}] {r.text}
                      </Typography>
                    ))}
                  </Stack>
                )}

                {tab === 2 && (
                  <List disablePadding>
                    {company.recentEvents.length === 0 && (
                      <Typography variant="body2" color="text.secondary">
                        No events detected yet.
                      </Typography>
                    )}
                    {company.recentEvents.map((e) => (
                      <EventListItem key={e.id} event={e} showCompany={false} />
                    ))}
                  </List>
                )}

                {tab === 3 && (
                  <Stack spacing={1.5}>
                    {company.recentNews.length === 0 && (
                      <Typography variant="body2" color="text.secondary">
                        No news available yet.
                      </Typography>
                    )}
                    {company.recentNews.map((n) => (
                      <Card key={n.id} variant="outlined">
                        <CardContent>
                          <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 0.5 }}>
                            <Typography variant="caption" color="text.secondary">
                              {n.sourceName} · {new Date(n.publishedAt).toLocaleDateString()}
                            </Typography>
                            {n.matchesGovernmentTheme && (
                              <Chip label="Govt Tailwind" size="small" color="success" variant="outlined" />
                            )}
                          </Stack>
                          <Typography variant="body2" fontWeight={600}>
                            {n.headline}
                          </Typography>
                          {n.summary && (
                            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                              {n.summary}
                            </Typography>
                          )}
                        </CardContent>
                      </Card>
                    ))}
                  </Stack>
                )}

                {tab === 4 && (
                  <Stack spacing={2}>
                    <Box>
                      <Button
                        variant="contained"
                        startIcon={<AutoAwesomeIcon />}
                        onClick={() => generateResearch.mutate()}
                        disabled={generateResearch.isPending}
                      >
                        {generateResearch.isPending ? 'Generating…' : 'Generate AI Research'}
                      </Button>
                      {generateResearch.isError && (
                        <Typography variant="body2" color="error.main" sx={{ mt: 1 }}>
                          {(generateResearch.error as any)?.response?.data?.error ??
                            'Failed to generate research. Check your AI settings.'}
                        </Typography>
                      )}
                    </Box>

                    {company.latestResearch ? (
                      <Stack spacing={2}>
                        <Typography variant="caption" color="text.secondary">
                          Generated {new Date(company.latestResearch.generatedAt).toLocaleString()} ·
                          Confidence: {company.latestResearch.confidenceLevel}
                        </Typography>
                        {([
                          ['Business Overview', company.latestResearch.businessOverview],
                          ['Strengths', company.latestResearch.strengths],
                          ['Weaknesses', company.latestResearch.weaknesses],
                          ['Growth Drivers', company.latestResearch.growthDrivers],
                          ['Government Tailwinds', company.latestResearch.governmentTailwinds],
                          ['Risks', company.latestResearch.risks],
                          ['Recent Developments', company.latestResearch.recentDevelopments],
                          ['Is It Improving?', company.latestResearch.improvingAssessment],
                          ['Points to Monitor', company.latestResearch.futureMonitoringPoints],
                        ] as [string, string | null][]).map(([label, value]) =>
                          value ? (
                            <Box key={label}>
                              <Typography variant="subtitle2" fontWeight={600}>
                                {label}
                              </Typography>
                              <Typography variant="body2" color="text.secondary">
                                {value}
                              </Typography>
                            </Box>
                          ) : null
                        )}
                      </Stack>
                    ) : (
                      <Typography variant="body2" color="text.secondary">
                        No AI research generated yet for this company.
                      </Typography>
                    )}
                  </Stack>
                )}
              </Box>
            </Box>
          </Stack>
        )}
      </QueryStateBoundary>
    </Box>
  );
}
