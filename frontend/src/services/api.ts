const TXN_SERVICE_URL = 'http://localhost:8081/api/v1/transactions';
const RECON_SERVICE_URL = 'http://localhost:8083/api/v1/reconciliations';
const STALE_SERVICE_URL = 'http://localhost:8084/api/v1/stale/runs';
const CACHE_SERVICE_URL = 'http://localhost:8085/api/v1/cache';

export interface Transaction {
  transactionId: string;
  productId: string;
  accountId: string;
  quantity: number;
  price: number;
  currency: string;
  status: string;
  tradeDate: string;
  createdAt: string;
}

export interface CreateTransactionPayload {
  transactionId: string;
  productId: string;
  accountId: string;
  quantity: number;
  price: number;
  currency: string;
  tradeDate: string;
}

export interface ReconBalanceResult {
  productId: string;
  txnTotalQty: number;
  posTotalQty: number;
  discrepancyQty: number;
  status: string;
}

export interface ReconRun {
  runId: string;
  reconDate: string;
  status: string;
  stale: boolean;
  createdAt: string;
  results: ReconBalanceResult[];
}

export interface CachedPosition {
  positionId: string;
  productId: string;
  totalQuantity: number;
  totalAmount: number;
  version: number;
  cachedAt?: string;
}

export interface RecoveryItem {
  id: number;
  transactionId: string;
  productId: string;
  payload: string;
  failureReason: string;
  status: string;
  createdAt: string;
}

export const api = {
  async createTransaction(payload: CreateTransactionPayload): Promise<Transaction> {
    const formattedPayload = {
      ...payload,
      tradeDate: payload.tradeDate.includes('T')
        ? payload.tradeDate
        : new Date(payload.tradeDate + 'T00:00:00Z').toISOString()
    };

    const res = await fetch(TXN_SERVICE_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(formattedPayload),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      let errMsg = err.message;
      if (err.errors) {
        errMsg = Object.entries(err.errors)
          .map(([k, v]) => `${k}: ${v}`)
          .join(', ');
      }
      throw new Error(errMsg || err.error || 'Failed to create transaction');
    }
    return res.json();
  },

  async getTransactions(): Promise<Transaction[]> {
    const res = await fetch(TXN_SERVICE_URL);
    if (!res.ok) throw new Error('Failed to fetch transactions');
    return res.json();
  },

  async triggerReconciliation(date?: string): Promise<ReconRun> {
    const queryDate = date || new Date().toISOString().split('T')[0];
    const res = await fetch(`${RECON_SERVICE_URL}/run?date=${queryDate}`, { method: 'POST' });
    if (!res.ok) throw new Error('Failed to trigger reconciliation');
    return res.json();
  },

  async getReconciliations(): Promise<ReconRun[]> {
    const res = await fetch(`${RECON_SERVICE_URL}/runs`);
    if (!res.ok) throw new Error('Failed to fetch reconciliations');
    return res.json();
  },

  async getStaleRuns(): Promise<ReconRun[]> {
    const res = await fetch(STALE_SERVICE_URL);
    if (!res.ok) throw new Error('Failed to fetch stale runs');
    return res.json();
  },

  async getCachedPosition(productId: string): Promise<CachedPosition> {
    const res = await fetch(`${CACHE_SERVICE_URL}/positions/${productId}`);
    if (!res.ok) throw new Error(`Product ${productId} not found in Redis cache`);
    return res.json();
  },

  async getRecoveryQueue(): Promise<RecoveryItem[]> {
    const res = await fetch(`${TXN_SERVICE_URL}/recovery-queue`);
    if (!res.ok) throw new Error('Failed to fetch recovery queue');
    return res.json();
  }
};
