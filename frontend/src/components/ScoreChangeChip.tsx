import { Chip } from '@mui/material';
import ArrowUpwardIcon from '@mui/icons-material/ArrowUpward';
import ArrowDownwardIcon from '@mui/icons-material/ArrowDownward';
import RemoveIcon from '@mui/icons-material/Remove';

interface ScoreChangeChipProps {
  change: number | null | undefined;
  size?: 'small' | 'medium';
}

export function ScoreChangeChip({ change, size = 'small' }: ScoreChangeChipProps) {
  if (change === null || change === undefined) {
    return <Chip size={size} label="New" variant="outlined" />;
  }
  if (change === 0) {
    return <Chip size={size} icon={<RemoveIcon />} label="0" variant="outlined" />;
  }
  if (change > 0) {
    return (
      <Chip
        size={size}
        icon={<ArrowUpwardIcon />}
        label={`+${change}`}
        color="success"
        variant="outlined"
      />
    );
  }
  return (
    <Chip
      size={size}
      icon={<ArrowDownwardIcon />}
      label={`${change}`}
      color="error"
      variant="outlined"
    />
  );
}
