import { Box, Typography } from '@mui/material';

interface ScoreBadgeProps {
  score: number | null | undefined;
  size?: number;
  showLabel?: boolean;
}

function scoreColor(score: number): string {
  if (score >= 80) return '#48BB78';
  if (score >= 60) return '#4FD1C5';
  if (score >= 40) return '#ECC94B';
  return '#F56565';
}

/** Compact circular score display used throughout company lists and cards. */
export function ScoreBadge({ score, size = 48, showLabel = false }: ScoreBadgeProps) {
  if (score === null || score === undefined) {
    return (
      <Box
        sx={{
          width: size,
          height: size,
          borderRadius: '50%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          border: '2px solid',
          borderColor: 'divider',
        }}
      >
        <Typography variant="caption" color="text.secondary">
          N/A
        </Typography>
      </Box>
    );
  }

  const color = scoreColor(score);

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 0.5 }}>
      <Box
        sx={{
          width: size,
          height: size,
          borderRadius: '50%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          border: '2.5px solid',
          borderColor: color,
          color,
        }}
      >
        <Typography variant={size > 40 ? 'subtitle1' : 'body2'} fontWeight={700}>
          {score}
        </Typography>
      </Box>
      {showLabel && (
        <Typography variant="caption" color="text.secondary">
          Score
        </Typography>
      )}
    </Box>
  );
}
