import { createTheme, type ThemeOptions } from '@mui/material/styles';

// Dark mode first, per spec: "Dark Mode first. No unnecessary animations."
// Palette favors a calm, professional slate/blue-gray rather than pure
// black, with a restrained accent color used sparingly for scores/positives.

const baseOptions: ThemeOptions = {
  typography: {
    fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
    h1: { fontWeight: 700 },
    h2: { fontWeight: 700 },
    h3: { fontWeight: 600 },
    h4: { fontWeight: 600 },
    h5: { fontWeight: 600 },
    h6: { fontWeight: 600 },
    button: { textTransform: 'none', fontWeight: 600 },
  },
  shape: {
    borderRadius: 10,
  },
  components: {
    MuiButton: {
      defaultProps: { disableElevation: true },
    },
    MuiPaper: {
      defaultProps: { elevation: 0 },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          backgroundImage: 'none',
        },
      },
    },
    // No unnecessary animations, per spec.
    MuiButtonBase: {
      defaultProps: {
        disableRipple: true,
      },
    },
  },
};

export const darkTheme = createTheme({
  ...baseOptions,
  palette: {
    mode: 'dark',
    background: {
      default: '#0B0F14',
      paper: '#12181F',
    },
    primary: {
      main: '#4FD1C5', // teal accent - used for score highlights, active states
    },
    secondary: {
      main: '#9F7AEA',
    },
    success: {
      main: '#48BB78',
    },
    error: {
      main: '#F56565',
    },
    warning: {
      main: '#ECC94B',
    },
    divider: 'rgba(255,255,255,0.08)',
    text: {
      primary: '#E6EDF3',
      secondary: '#8B98A5',
    },
  },
});

export const lightTheme = createTheme({
  ...baseOptions,
  palette: {
    mode: 'light',
    background: {
      default: '#F7F9FB',
      paper: '#FFFFFF',
    },
    primary: {
      main: '#0F9488',
    },
    secondary: {
      main: '#7C3AED',
    },
    success: {
      main: '#2F9E58',
    },
    error: {
      main: '#DC2626',
    },
    warning: {
      main: '#B7791F',
    },
    divider: 'rgba(15,23,42,0.08)',
    text: {
      primary: '#0F172A',
      secondary: '#64748B',
    },
  },
});
