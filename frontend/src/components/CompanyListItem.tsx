import { Box, ListItemButton, Stack, Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import { ScoreBadge } from './ScoreBadge';
import { ScoreChangeChip } from './ScoreChangeChip';
import type { CompanySummary } from '@/types';

interface CompanyListItemProps {
  company: CompanySummary;
}

function formatMarketCap(cr: number | null): string {
  if (cr === null) return '—';
  if (cr >= 100000) return `₹${(cr / 100000).toFixed(2)}L Cr`;
  if (cr >= 1000) return `₹${(cr / 1000).toFixed(1)}K Cr`;
  return `₹${cr.toFixed(0)} Cr`;
}

export function CompanyListItem({ company }: CompanyListItemProps) {
  const navigate = useNavigate();

  return (
    <ListItemButton
      onClick={() => navigate(`/company/${company.id}`)}
      sx={{
        borderRadius: 2,
        border: '1px solid',
        borderColor: 'divider',
        mb: 1,
        py: 1.5,
      }}
    >
      <Stack direction="row" alignItems="center" spacing={2} sx={{ width: '100%' }}>
        <ScoreBadge score={company.currentScore} size={44} />

        <Box sx={{ flexGrow: 1, minWidth: 0 }}>
          <Typography variant="subtitle2" fontWeight={600} noWrap>
            {company.name}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {company.symbol} · {company.sector} · {formatMarketCap(company.marketCapCr)}
          </Typography>
        </Box>

        <Stack alignItems="flex-end" spacing={0.5}>
          <Typography variant="body2" fontWeight={600}>
            {company.currentPrice !== null ? `₹${company.currentPrice.toFixed(2)}` : '—'}
          </Typography>
          <ScoreChangeChip change={company.scoreChange} />
        </Stack>
      </Stack>
    </ListItemButton>
  );
}
