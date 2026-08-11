// Mirrors the backend DTO layer (com.stockresearch.dto.*) field-for-field.
// Keep these in sync manually if the backend DTOs change.

export interface CompanySummary {
  id: number;
  symbol: string;
  name: string;
  sector: string;
  marketCapCr: number | null;
  currentPrice: number | null;
  currentScore: number | null;
  previousScore: number | null;
  scoreChange: number | null;
}

export interface ScoreReason {
  text: string;
  type: 'POSITIVE' | 'NEGATIVE' | 'NEUTRAL';
  category: string;
}

export interface Score {
  snapshotId: number;
  totalScore: number;
  computedAt: string;
  categoryScores: Record<string, number>;
  reasons: ScoreReason[];
}

export interface ScoreChange {
  previousScore: number | null;
  currentScore: number;
  delta: number | null;
  reasons: ScoreReason[];
}

export interface EventItem {
  id: number;
  companyId: number;
  companySymbol: string;
  companyName: string;
  type: string;
  title: string;
  description: string | null;
  valueCr: number | null;
  eventDate: string;
  sourceUrl: string | null;
  source: string | null;
  scoreImpact: number | null;
}

export interface NewsItem {
  id: number;
  companyId: number;
  companySymbol: string;
  headline: string;
  summary: string | null;
  url: string | null;
  sourceName: string | null;
  publishedAt: string;
  matchesGovernmentTheme: boolean | null;
}

export interface ResearchSummary {
  id: number;
  businessOverview: string | null;
  strengths: string | null;
  weaknesses: string | null;
  growthDrivers: string | null;
  governmentTailwinds: string | null;
  risks: string | null;
  recentDevelopments: string | null;
  improvingAssessment: string | null;
  futureMonitoringPoints: string | null;
  confidenceLevel: string | null;
  generatedAt: string;
}

export interface CompanyDetail {
  id: number;
  symbol: string;
  name: string;
  exchange: string;
  sector: string;
  industry: string | null;

  marketCapCr: number | null;
  revenueGrowthPct: number | null;
  profitGrowthPct: number | null;
  operatingMarginPct: number | null;
  promoterHoldingPct: number | null;
  institutionalHoldingPct: number | null;
  debtToEquity: number | null;
  roce: number | null;
  roe: number | null;
  peRatio: number | null;
  operatingCashFlowCr: number | null;

  currentPrice: number | null;
  week52High: number | null;
  week52Low: number | null;

  lastRefreshedAt: string | null;

  currentScore: Score | null;
  previousScore: Score | null;

  recentEvents: EventItem[];
  recentNews: NewsItem[];
  latestResearch: ResearchSummary | null;

  onWatchlist: boolean;
}

export interface WhyInteresting {
  companyId: number;
  symbol: string;
  totalScore: number;
  starRating: number;
  label: string;
  positives: ScoreReason[];
  cautions: ScoreReason[];
}

export interface SectorHeatmapEntry {
  sector: string;
  averageScore: number;
  companyCount: number;
}

export interface MarketOverview {
  totalCompaniesTracked: number;
  totalEventsLast7Days: number;
  companiesAboveThreshold: number;
  topSector: string | null;
}

export interface Dashboard {
  topScoring: CompanySummary[];
  latestEvents: EventItem[];
  sectorHeatmap: SectorHeatmapEntry[];
  recentlyImproved: CompanySummary[];
  watchlist: CompanySummary[];
  marketOverview: MarketOverview;
}

export interface SectorSummary {
  sector: string;
  averageRevenueGrowthPct: number | null;
  averageProfitGrowthPct: number | null;
  averageScore: number;
  recentNewsCount: number;
  highestScore: number;
  bestPerforming: CompanySummary[];
}

export type AiProvider = 'CLAUDE' | 'OPENAI' | 'GEMINI' | 'OPENROUTER' | 'LOCAL';
export type Theme = 'DARK' | 'LIGHT';

export interface Settings {
  aiProvider: AiProvider;
  apiKey: string | null; // masked when read from server
  baseUrl: string | null;
  modelName: string | null;
  temperature: number | null;
  maxTokens: number | null;

  theme: Theme;

  refreshIntervalHours: number | null;

  marketCapMinCr: number | null;
  marketCapMaxCr: number | null;
  minPromoterHoldingPct: number | null;
  maxDebtToEquity: number | null;
  minRocePct: number | null;
  topNResults: number | null;
  minScoreThreshold: number | null;
}

export interface CompanyFilters {
  sector?: string;
  minMarketCapCr?: number;
  maxMarketCapCr?: number;
  maxPe?: number;
  minRoce?: number;
  maxDebtToEquity?: number;
  minRevenueGrowthPct?: number;
  minProfitGrowthPct?: number;
  minScore?: number;
}
