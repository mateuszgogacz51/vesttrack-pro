export type Role = 'USER' | 'EMPLOYEE' | 'ADMIN';
export type AccountType = 'REGULAR' | 'IKE' | 'IKZE';
export type AssetType = 'STOCK' | 'ETF' | 'BOND' | 'OTHER';
export type StrategyRole = 'CORE' | 'SATELLITE';
export type TransactionType = 'BUY' | 'SELL' | 'DIVIDEND';
export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  email: string;
  role: Role;
}

export interface AccountResponse {
  id: number;
  name: string;
  brokerageFirmId: number | null;
  brokerageFirmName: string | null;
  accountType: AccountType;
  currency: string;
  annualContributionLimit: number | null;
  contributedThisYear: number;
  active: boolean;
}

export interface BrokerageFirm {
  id: number;
  name: string;
  category: 'BANK' | 'BROKER' | 'TFI';
  website: string | null;
}

export interface TransactionResponse {
  id: number;
  accountId: number;
  instrumentTicker: string;
  transactionType: TransactionType;
  quantity: number;
  price: number;
  fee: number;
  instrumentCurrency: string;
  exchangeRate: number;
  transactionDate: string;
  realizedGain: number | null;
  realizedGainCurrency: string | null;
}

export interface AssetAllocation {
  ticker: string;
  strategyRole: StrategyRole;
  currentValue: number;
  actualWeight: number;
  targetWeight: number;
  deviation: number;
  rebalanceSuggestion: string;
}

export interface PortfolioAllocationResponse {
  accountId: number;
  totalValue: number;
  coreActualWeight: number;
  satelliteActualWeight: number;
  assets: AssetAllocation[];
}

export interface PerformanceResponse {
  accountId: number;
  twrPercentage: number | null;
  mwrPercentage: number | null;
  currentMarketValue: number;
  methodologyNote: string;
}

export interface TicketResponse {
  id: number;
  subject: string;
  description: string;
  status: TicketStatus;
  assignedEmployeeEmail: string | null;
  createdAt: string;
}

export interface FinancialInstrument {
  id: number;
  ticker: string;
  name: string;
  assetType: AssetType;
  isin: string | null;
  exchange: string | null;
  quoteCurrency: string;
  accumulating: boolean | null;
  blocked: boolean;
  lastPrice: number | null;
  lastPriceAt: string | null;
}

export interface EmployeeStatsResponse {
  employeeId: number;
  email: string;
  resolvedTickets: number;
  inProgressTickets: number;
}

export interface AuditLogResponse {
  id: number;
  actorEmail: string;
  action: string;
  details: string | null;
  ipAddress: string | null;
  createdAt: string;
}

export interface ApiUsageResponse {
  provider: string;
  usageDate: string;
  callCount: number;
  errorCount: number;
  dailyLimit: number | null;
  usagePercentage: number | null;
}

export interface ProviderConfigResponse {
  provider: string;
  hasApiKey: boolean;
  dailyLimit: number | null;
  active: boolean;
}

export interface AnonymizedPortfolioView {
  accountId: number;
  accountTypeMasked: string;
  weightsByAssetType: { assetType: string; percentage: number }[];
}

export interface ApiErrorBody {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  fieldErrors?: Record<string, string>;
}
