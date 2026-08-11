import { useState } from 'react';
import { Box, List, MenuItem, TextField, Typography } from '@mui/material';
import { useRecentEvents } from '@/hooks/useMisc';
import { QueryStateBoundary } from '@/components/QueryStateBoundary';
import { EventListItem } from '@/components/EventListItem';
import { EVENT_TYPES } from '@/constants';

function formatEventType(type: string): string {
  return type
    .split('_')
    .map((w) => w.charAt(0) + w.slice(1).toLowerCase())
    .join(' ');
}

export function EventCenterPage() {
  const [type, setType] = useState('');
  const { data, isLoading, isError, error } = useRecentEvents(type || undefined, 100);

  return (
    <Box>
      <Typography variant="h5" fontWeight={700} sx={{ mb: 3 }}>
        Event Center
      </Typography>

      <TextField
        select
        label="Filter by type"
        size="small"
        value={type}
        onChange={(e) => setType(e.target.value)}
        sx={{ mb: 3, minWidth: 260 }}
      >
        <MenuItem value="">All Event Types</MenuItem>
        {EVENT_TYPES.map((t) => (
          <MenuItem key={t} value={t}>
            {formatEventType(t)}
          </MenuItem>
        ))}
      </TextField>

      <QueryStateBoundary isLoading={isLoading} isError={isError} error={error}>
        {data && data.length === 0 && (
          <Typography variant="body2" color="text.secondary">
            No events detected yet. Try refreshing discovery from Settings.
          </Typography>
        )}
        <List disablePadding>
          {data?.map((e) => (
            <EventListItem key={e.id} event={e} />
          ))}
        </List>
      </QueryStateBoundary>
    </Box>
  );
}
