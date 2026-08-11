import { useEffect, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Divider,
  MenuItem,
  Slider,
  Snackbar,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';
import Grid from '@mui/material/Grid2';
import { useRefreshDiscovery, useSettings, useUpdateSettings } from '@/hooks/useMisc';
import { QueryStateBoundary } from '@/components/QueryStateBoundary';
import type { AiProvider, Settings, Theme } from '@/types';

const AI_PROVIDERS: { value: AiProvider; label: string }[] = [
  { value: 'CLAUDE', label: 'Claude (Anthropic)' },
  { value: 'OPENAI', label: 'OpenAI' },
  { value: 'GEMINI', label: 'Gemini (Google)' },
  { value: 'OPENROUTER', label: 'OpenRouter' },
  { value: 'LOCAL', label: 'Local LLM (coming soon)' },
];

export function SettingsPage() {
  const { data, isLoading, isError, error } = useSettings();
  const updateSettings = useUpdateSettings();
  const refreshDiscovery = useRefreshDiscovery();

  const [form, setForm] = useState<Partial<Settings>>({});
  const [snackbar, setSnackbar] = useState<string | null>(null);

  useEffect(() => {
    if (data) setForm(data);
  }, [data]);

  const handleChange = <K extends keyof Settings>(key: K, value: Settings[K]) => {
    setForm((prev) => ({ ...prev, [key]: value }));
  };

  const handleSave = () => {
    // Only send apiKey if the user typed a new one (not the masked placeholder)
    const payload = { ...form };
    updateSettings.mutate(payload, {
      onSuccess: () => setSnackbar('Settings saved.'),
      onError: () => setSnackbar('Failed to save settings.'),
    });
  };

  const handleRefreshNow = () => {
    refreshDiscovery.mutate(undefined, {
      onSuccess: (res) => setSnackbar(`Refresh complete: ${res.included} included, ${res.excluded} excluded.`),
      onError: () => setSnackbar('Refresh failed.'),
    });
  };

  return (
    <Box>
      <Typography variant="h5" fontWeight={700} sx={{ mb: 3 }}>
        Settings
      </Typography>

      <QueryStateBoundary isLoading={isLoading} isError={isError} error={error}>
        <Stack spacing={3}>
          {/* AI Configuration */}
          <Card variant="outlined">
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 2 }}>
                AI Provider
              </Typography>
              <Grid container spacing={2}>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    select
                    fullWidth
                    label="Provider"
                    size="small"
                    value={form.aiProvider ?? 'OPENAI'}
                    onChange={(e) => handleChange('aiProvider', e.target.value as AiProvider)}
                  >
                    {AI_PROVIDERS.map((p) => (
                      <MenuItem key={p.value} value={p.value}>
                        {p.label}
                      </MenuItem>
                    ))}
                  </TextField>
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    label="Model Name"
                    size="small"
                    placeholder="e.g. claude-sonnet-4-6, gpt-4o-mini"
                    value={form.modelName ?? ''}
                    onChange={(e) => handleChange('modelName', e.target.value)}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    type="password"
                    label="API Key"
                    size="small"
                    placeholder={form.apiKey ?? 'Not set'}
                    helperText="Leave blank to keep the currently saved key"
                    onChange={(e) => handleChange('apiKey', e.target.value)}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    label="Base URL (optional)"
                    size="small"
                    placeholder="Uses provider default if blank"
                    value={form.baseUrl ?? ''}
                    onChange={(e) => handleChange('baseUrl', e.target.value)}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <Typography variant="caption" color="text.secondary">
                    Temperature: {form.temperature ?? 0.3}
                  </Typography>
                  <Slider
                    value={form.temperature ?? 0.3}
                    onChange={(_, v) => handleChange('temperature', v as number)}
                    min={0}
                    max={1}
                    step={0.05}
                    size="small"
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    type="number"
                    label="Maximum Tokens"
                    size="small"
                    value={form.maxTokens ?? 1500}
                    onChange={(e) => handleChange('maxTokens', Number(e.target.value))}
                  />
                </Grid>
              </Grid>
            </CardContent>
          </Card>

          {/* Appearance */}
          <Card variant="outlined">
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 2 }}>
                Appearance
              </Typography>
              <TextField
                select
                label="Theme"
                size="small"
                value={form.theme ?? 'DARK'}
                onChange={(e) => handleChange('theme', e.target.value as Theme)}
                sx={{ minWidth: 200 }}
              >
                <MenuItem value="DARK">Dark</MenuItem>
                <MenuItem value="LIGHT">Light</MenuItem>
              </TextField>
            </CardContent>
          </Card>

          {/* Refresh & Discovery Thresholds */}
          <Card variant="outlined">
            <CardContent>
              <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 2 }}>
                Refresh & Discovery Thresholds
              </Typography>
              <Grid container spacing={2}>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    type="number"
                    label="Refresh Interval (hours)"
                    size="small"
                    value={form.refreshIntervalHours ?? 6}
                    onChange={(e) => handleChange('refreshIntervalHours', Number(e.target.value))}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    type="number"
                    label="Min Score Threshold"
                    size="small"
                    value={form.minScoreThreshold ?? 60}
                    onChange={(e) => handleChange('minScoreThreshold', Number(e.target.value))}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    type="number"
                    label="Market Cap Min (Cr)"
                    size="small"
                    value={form.marketCapMinCr ?? 200}
                    onChange={(e) => handleChange('marketCapMinCr', Number(e.target.value))}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    type="number"
                    label="Market Cap Max (Cr)"
                    size="small"
                    value={form.marketCapMaxCr ?? 10000}
                    onChange={(e) => handleChange('marketCapMaxCr', Number(e.target.value))}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    type="number"
                    label="Min Promoter Holding (%)"
                    size="small"
                    value={form.minPromoterHoldingPct ?? 35}
                    onChange={(e) => handleChange('minPromoterHoldingPct', Number(e.target.value))}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    type="number"
                    label="Max Debt/Equity"
                    size="small"
                    value={form.maxDebtToEquity ?? 1.0}
                    onChange={(e) => handleChange('maxDebtToEquity', Number(e.target.value))}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    type="number"
                    label="Min ROCE (%)"
                    size="small"
                    value={form.minRocePct ?? 12}
                    onChange={(e) => handleChange('minRocePct', Number(e.target.value))}
                  />
                </Grid>
                <Grid size={{ xs: 12, sm: 6 }}>
                  <TextField
                    fullWidth
                    type="number"
                    label="Top N Results"
                    size="small"
                    value={form.topNResults ?? 50}
                    onChange={(e) => handleChange('topNResults', Number(e.target.value))}
                  />
                </Grid>
              </Grid>

              <Divider sx={{ my: 2 }} />

              <Button
                variant="outlined"
                startIcon={<RefreshIcon />}
                onClick={handleRefreshNow}
                disabled={refreshDiscovery.isPending}
              >
                {refreshDiscovery.isPending ? 'Refreshing…' : 'Refresh Now'}
              </Button>
            </CardContent>
          </Card>

          <Alert severity="info">
            This app never recommends buying or selling. AI research and scores are
            for organizing information only — always do your own research.
          </Alert>

          <Box>
            <Button variant="contained" onClick={handleSave} disabled={updateSettings.isPending}>
              {updateSettings.isPending ? 'Saving…' : 'Save Settings'}
            </Button>
          </Box>
        </Stack>
      </QueryStateBoundary>

      <Snackbar
        open={!!snackbar}
        autoHideDuration={4000}
        onClose={() => setSnackbar(null)}
        message={snackbar}
      />
    </Box>
  );
}
