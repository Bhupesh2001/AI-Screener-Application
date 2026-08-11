import { useMemo, useState } from 'react';
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Box,
  InputAdornment,
  List,
  MenuItem,
  Slider,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import Grid from '@mui/material/Grid2';
import SearchIcon from '@mui/icons-material/Search';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import { useCompanyFilter, useCompanySearch } from '@/hooks/useCompany';
import { CompanyListItem } from '@/components/CompanyListItem';
import { QueryStateBoundary } from '@/components/QueryStateBoundary';
import { SectorService_TRACKED_SECTORS } from '@/constants';
import type { CompanyFilters } from '@/types';

export function StockExplorerPage() {
  const [query, setQuery] = useState('');
  const [sector, setSector] = useState('');
  const [minScore, setMinScore] = useState<number>(0);
  const [maxDebtToEquity, setMaxDebtToEquity] = useState<number>(3);
  const [minRoce, setMinRoce] = useState<number>(0);

  const filters: CompanyFilters = useMemo(
    () => ({
      sector: sector || undefined,
      minScore: minScore > 0 ? minScore : undefined,
      maxDebtToEquity: maxDebtToEquity < 3 ? maxDebtToEquity : undefined,
      minRoce: minRoce > 0 ? minRoce : undefined,
    }),
    [sector, minScore, maxDebtToEquity, minRoce]
  );

  const hasActiveFilters = !!sector || minScore > 0 || maxDebtToEquity < 3 || minRoce > 0;
  const isSearching = query.trim().length > 0;

  const searchResult = useCompanySearch(query.trim());
  const filterResult = useCompanyFilter(filters, !isSearching && hasActiveFilters);

  const results = isSearching ? searchResult : filterResult;
  const showingFiltered = !isSearching && hasActiveFilters;

  return (
    <Box>
      <Typography variant="h5" fontWeight={700} sx={{ mb: 3 }}>
        Stock Explorer
      </Typography>

      <TextField
        fullWidth
        placeholder="Search by name or symbol (e.g. RVNL, Bharat Electronics)"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        slotProps={{
          input: {
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon fontSize="small" />
              </InputAdornment>
            ),
          },
        }}
        sx={{ mb: 2 }}
      />

      <Accordion variant="outlined" sx={{ mb: 3 }}>
        <AccordionSummary expandIcon={<ExpandMoreIcon />}>
          <Typography variant="subtitle2">Filters</Typography>
        </AccordionSummary>
        <AccordionDetails>
          <Grid container spacing={3}>
            <Grid size={{ xs: 12, sm: 4 }}>
              <TextField
                select
                fullWidth
                label="Sector"
                size="small"
                value={sector}
                onChange={(e) => setSector(e.target.value)}
              >
                <MenuItem value="">All Sectors</MenuItem>
                {SectorService_TRACKED_SECTORS.map((s) => (
                  <MenuItem key={s} value={s}>
                    {s}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <Typography variant="caption" color="text.secondary">
                Minimum Score: {minScore}
              </Typography>
              <Slider
                value={minScore}
                onChange={(_, v) => setMinScore(v as number)}
                min={0}
                max={100}
                step={5}
                size="small"
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <Typography variant="caption" color="text.secondary">
                Min ROCE: {minRoce}%
              </Typography>
              <Slider
                value={minRoce}
                onChange={(_, v) => setMinRoce(v as number)}
                min={0}
                max={40}
                step={1}
                size="small"
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <Typography variant="caption" color="text.secondary">
                Max Debt/Equity: {maxDebtToEquity >= 3 ? 'Any' : maxDebtToEquity}
              </Typography>
              <Slider
                value={maxDebtToEquity}
                onChange={(_, v) => setMaxDebtToEquity(v as number)}
                min={0}
                max={3}
                step={0.1}
                size="small"
              />
            </Grid>
          </Grid>
        </AccordionDetails>
      </Accordion>

      <Stack spacing={1}>
        {!isSearching && !hasActiveFilters && (
          <Typography variant="body2" color="text.secondary">
            Search by name/symbol above, or expand Filters to browse by criteria.
          </Typography>
        )}

        {(isSearching || showingFiltered) && (
          <QueryStateBoundary
            isLoading={results.isLoading}
            isError={results.isError}
            error={results.error}
          >
            <List disablePadding>
              {results.data && results.data.length === 0 && (
                <Typography variant="body2" color="text.secondary">
                  No companies match your criteria.
                </Typography>
              )}
              {results.data?.map((c) => (
                <CompanyListItem key={c.id} company={c} />
              ))}
            </List>
          </QueryStateBoundary>
        )}
      </Stack>
    </Box>
  );
}
