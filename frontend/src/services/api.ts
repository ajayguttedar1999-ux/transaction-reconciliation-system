const TXN_SERVICE_URL = 'http://localhost:8081/api/v1/transactions';
const RECON_SERVICE_URL = 'http://localhost:8083/api/v1/reconciliations';
const STALE_SERVICE_URL = 'http://localhost:8084/api/v1/stale-runs';

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

export const api = {
  async createTransaction(payload: CreateTransactionPayload): Promise<Transaction> {
    const res = await fetch(TXN_SERVICE_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    if (!res.ok) {
      const err = await res.json();
      throw new Error(err.message || 'Failed to create transaction');
    }
    return res.json();
  },

  async getTransactions(): Promise<Transaction[]> {
    const res = await fetch(TXN_SERVICE_URL);
    if (!res.ok) throw new Error('Failed to fetch transactions');
    return res.json();
  },

  async triggerReconciliation(): Promise<ReconRun> {
    const res = await fetch(`${RECON_SERVICE_URL}/trigger`, { method: 'POST' });
    if (!res.ok) throw new Error('Failed to trigger reconciliation');
    return res.json();
  },

  async getReconciliations(): Promise<ReconRun[]> {
    const res = await fetch(RECON_SERVICE_URL);
    if (!res.ok) throw new Error('Failed to fetch reconciliations');
    return res.json();
  },

  async getStaleRuns(): Promise<ReconRun[]> {
    const res = await fetch(STALE_SERVICE_URL);
    if (!res.ok) throw new Error('Failed to fetch stale runs');
    return res.json();
  },
};
