import { Box, Chip, ListItem, Stack, Typography } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import type { EventItem } from '@/types';

interface EventListItemProps {
  event: EventItem;
  showCompany?: boolean;
}

function formatEventType(type: string): string {
  return type
    .split('_')
    .map((w) => w.charAt(0) + w.slice(1).toLowerCase())
    .join(' ');
}

function formatDate(iso: string): string {
  const d = new Date(iso);
  return d.toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
}

const POSITIVE_TYPES = new Set([
  'LARGE_ORDER', 'GOVERNMENT_CONTRACT', 'EXPORT_ORDER', 'CAPACITY_EXPANSION',
  'NEW_FACTORY', 'PLANT_COMMISSIONING', 'NEW_PRODUCT', 'JOINT_VENTURE',
  'STRATEGIC_PARTNERSHIP', 'PROMOTER_BUYING', 'INSTITUTIONAL_BUYING',
  'CREDIT_RATING_UPGRADE', 'PATENT', 'GOVERNMENT_APPROVAL', 'PLI_PARTICIPATION',
  'MANAGEMENT_GUIDANCE_UP', 'EARNINGS_SURPRISE', 'BONUS',
]);

export function EventListItem({ event, showCompany = true }: EventListItemProps) {
  const navigate = useNavigate();
  const isPositive = POSITIVE_TYPES.has(event.type);

  return (
    <ListItem
      onClick={showCompany ? () => navigate(`/company/${event.companyId}`) : undefined}
      sx={{
        borderRadius: 2,
        border: '1px solid',
        borderColor: 'divider',
        mb: 1,
        cursor: showCompany ? 'pointer' : 'default',
        flexDirection: 'column',
        alignItems: 'flex-start',
        py: 1.5,
      }}
    >
      <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 0.5, flexWrap: 'wrap' }}>
        <Chip
          size="small"
          label={formatEventType(event.type)}
          color={isPositive ? 'success' : 'default'}
          variant="outlined"
        />
        {showCompany && (
          <Typography variant="caption" fontWeight={600}>
            {event.companySymbol}
          </Typography>
        )}
        <Typography variant="caption" color="text.secondary">
          {formatDate(event.eventDate)}
        </Typography>
        {event.valueCr !== null && (
          <Typography variant="caption" color="text.secondary">
            · ₹{event.valueCr.toLocaleString()} Cr
          </Typography>
        )}
      </Stack>
      <Typography variant="body2">{event.title}</Typography>
      {event.description && (
        <Box sx={{ mt: 0.5 }}>
          <Typography variant="caption" color="text.secondary">
            {event.description}
          </Typography>
        </Box>
      )}
    </ListItem>
  );
}
