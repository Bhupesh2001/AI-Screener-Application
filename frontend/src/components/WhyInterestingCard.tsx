import { Box, Card, CardContent, Rating, Stack, Typography } from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import type { WhyInteresting } from '@/types';

interface WhyInterestingCardProps {
  data: WhyInteresting;
}

/**
 * The "one feature I'd add" card from the spec:
 *   ★★★★☆ Interesting
 *   ✔ Revenue growth accelerated for 3 quarters
 *   ✔ New ₹600 Cr order
 *   ⚠ Valuation slightly above historical average
 *
 * Saves the user from opening every company's details to get the gist.
 */
export function WhyInterestingCard({ data }: WhyInterestingCardProps) {
  return (
    <Card variant="outlined">
      <CardContent>
        <Stack direction="row" alignItems="center" spacing={1.5} sx={{ mb: 2 }}>
          <Rating value={data.starRating} max={5} readOnly precision={1} />
          <Typography variant="subtitle1" fontWeight={600}>
            {data.label}
          </Typography>
        </Stack>

        <Stack spacing={1}>
          {data.positives.map((reason, idx) => (
            <Box key={`pos-${idx}`} sx={{ display: 'flex', alignItems: 'flex-start', gap: 1 }}>
              <CheckCircleIcon fontSize="small" color="success" sx={{ mt: '2px' }} />
              <Typography variant="body2">{reason.text}</Typography>
            </Box>
          ))}
          {data.cautions.map((reason, idx) => (
            <Box key={`caution-${idx}`} sx={{ display: 'flex', alignItems: 'flex-start', gap: 1 }}>
              <WarningAmberIcon fontSize="small" color="warning" sx={{ mt: '2px' }} />
              <Typography variant="body2" color="text.secondary">
                {reason.text}
              </Typography>
            </Box>
          ))}
        </Stack>

        {data.positives.length === 0 && data.cautions.length === 0 && (
          <Typography variant="body2" color="text.secondary">
            No specific signals detected yet.
          </Typography>
        )}
      </CardContent>
    </Card>
  );
}
