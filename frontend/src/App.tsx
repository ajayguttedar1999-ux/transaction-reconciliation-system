import React, { useState, useEffect } from 'react';
import { api, Transaction, ReconRun, CreateTransactionPayload } from './services/api';

export default function App() {
  const [activeTab, setActiveTab] = useState<'create' | 'transactions' | 'recon' | 'stale'>('create');
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [reconRuns, setReconRuns] = useState<ReconRun[]>([]);
  const [staleRuns, setStaleRuns] = useState<ReconRun[]>([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<{ text: string; type: 'success' | 'error' } | null>(null);

  const [form, setForm] = useState<CreateTransactionPayload>({
    transactionId: `TXN-${Date.now().toString().slice(-5)}`,
    productId: 'EQUITY-AAPL',
    accountId: 'ACC-1001',
    quantity: 100,
    price: 180.5,
    currency: 'USD',
    tradeDate: new Date().toISOString(),
  });

  const loadData = async () => {
    try {
      setLoading(true);
      if (activeTab === 'transactions') {
        const data = await api.getTransactions();
        setTransactions(data);
      } else if (activeTab === 'recon') {
        const data = await api.getReconciliations();
        setReconRuns(data);
      } else if (activeTab === 'stale') {
        const data = await api.getStaleRuns();
        setStaleRuns(data);
      }
    } catch (err: any) {
      setMessage({ text: err.message, type: 'error' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [activeTab]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setLoading(true);
      await api.createTransaction(form);
      setMessage({ text: `Transaction ${form.transactionId} booked successfully!`, type: 'success' });
      setForm(prev => ({ ...prev, transactionId: `TXN-${Date.now().toString().slice(-5)}` }));
    } catch (err: any) {
      setMessage({ text: err.message, type: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleTriggerRecon = async () => {
    try {
      setLoading(true);
      const newRun = await api.triggerReconciliation();
      setMessage({ text: `Reconciliation ${newRun.runId} completed with status: ${newRun.status}`, type: 'success' });
      loadData();
    } catch (err: any) {
      setMessage({ text: err.message, type: 'error' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: '1100px', margin: '40px auto', padding: '0 20px' }}>
      <header style={{ borderBottom: '1px solid #334155', paddingBottom: '20px', marginBottom: '30px' }}>
        <h1 style={{ margin: 0, fontSize: '26px', color: '#38bdf8' }}>
          Transaction Reconciliation Demo System
        </h1>
        <p style={{ margin: '6px 0 0 0', color: '#94a3b8' }}>
          Enterprise Financial Flow: React &rarr; Spring Boot &rarr; PostgreSQL &rarr; Kafka &rarr; Reconciliation
        </p>
      </header>

      {message && (
        <div style={{
          padding: '12px 16px',
          borderRadius: '8px',
          marginBottom: '20px',
          backgroundColor: message.type === 'success' ? '#065f46' : '#991b1b',
          color: '#ffffff',
          display: 'flex',
          justifyContent: 'space-between',
        }}>
          <span>{message.text}</span>
          <button onClick={() => setMessage(null)} style={{ background: 'none', border: 'none', color: '#fff', cursor: 'pointer' }}>✕</button>
        </div>
      )}

      <div style={{ display: 'flex', gap: '10px', marginBottom: '24px' }}>
        {[
          { id: 'create', label: '1. Create Transaction' },
          { id: 'transactions', label: '2. Transaction List' },
          { id: 'recon', label: '3. Reconciliation Runs' },
          { id: 'stale', label: '4. Stale Runs Alert' },
        ].map(tab => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id as any)}
            style={{
              padding: '10px 18px',
              borderRadius: '6px',
              border: 'none',
              cursor: 'pointer',
              fontWeight: 600,
              backgroundColor: activeTab === tab.id ? '#0284c7' : '#1e293b',
              color: activeTab === tab.id ? '#ffffff' : '#94a3b8',
              transition: 'all 0.2s',
            }}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === 'create' && (
        <div style={{ backgroundColor: '#1e293b', padding: '24px', borderRadius: '8px', border: '1px solid #334155' }}>
          <h2 style={{ marginTop: 0, color: '#f8fafc', fontSize: '20px' }}>Book New Financial Transaction</h2>
          <form onSubmit={handleSubmit} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
            <div>
              <label style={{ display: 'block', marginBottom: '6px', color: '#94a3b8' }}>Transaction ID</label>
              <input
                type="text"
                value={form.transactionId}
                onChange={e => setForm({ ...form, transactionId: e.target.value })}
                required
                style={{ width: '100%', padding: '10px', borderRadius: '6px', background: '#0f172a', border: '1px solid #475569', color: '#fff' }}
              />
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: '6px', color: '#94a3b8' }}>Product ID</label>
              <select
                value={form.productId}
                onChange={e => setForm({ ...form, productId: e.target.value })}
                style={{ width: '100%', padding: '10px', borderRadius: '6px', background: '#0f172a', border: '1px solid #475569', color: '#fff' }}
              >
                <option value="EQUITY-AAPL">EQUITY-AAPL (Apple Inc.)</option>
                <option value="EQUITY-IBM">EQUITY-IBM (IBM Corp.)</option>
                <option value="FX-EURUSD">FX-EURUSD (Euro/USD)</option>
              </select>
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: '6px', color: '#94a3b8' }}>Account ID</label>
              <input
                type="text"
                value={form.accountId}
                onChange={e => setForm({ ...form, accountId: e.target.value })}
                required
                style={{ width: '100%', padding: '10px', borderRadius: '6px', background: '#0f172a', border: '1px solid #475569', color: '#fff' }}
              />
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: '6px', color: '#94a3b8' }}>Quantity</label>
              <input
                type="number"
                step="0.01"
                value={form.quantity}
                onChange={e => setForm({ ...form, quantity: parseFloat(e.target.value) })}
                required
                style={{ width: '100%', padding: '10px', borderRadius: '6px', background: '#0f172a', border: '1px solid #475569', color: '#fff' }}
              />
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: '6px', color: '#94a3b8' }}>Price</label>
              <input
                type="number"
                step="0.01"
                value={form.price}
                onChange={e => setForm({ ...form, price: parseFloat(e.target.value) })}
                required
                style={{ width: '100%', padding: '10px', borderRadius: '6px', background: '#0f172a', border: '1px solid #475569', color: '#fff' }}
              />
            </div>
            <div>
              <label style={{ display: 'block', marginBottom: '6px', color: '#94a3b8' }}>Currency</label>
              <input
                type="text"
                value={form.currency}
                onChange={e => setForm({ ...form, currency: e.target.value })}
                required
                style={{ width: '100%', padding: '10px', borderRadius: '6px', background: '#0f172a', border: '1px solid #475569', color: '#fff' }}
              />
            </div>
            <div style={{ gridColumn: 'span 2', marginTop: '10px' }}>
              <button
                type="submit"
                disabled={loading}
                style={{
                  padding: '12px 24px',
                  backgroundColor: '#0284c7',
                  color: '#fff',
                  border: 'none',
                  borderRadius: '6px',
                  fontWeight: 600,
                  cursor: 'pointer',
                  fontSize: '15px'
                }}
              >
                {loading ? 'Submitting & Emitting Kafka Event...' : 'Book Transaction & Publish to Kafka'}
              </button>
            </div>
          </form>
        </div>
      )}

      {activeTab === 'transactions' && (
        <div style={{ backgroundColor: '#1e293b', padding: '24px', borderRadius: '8px', border: '1px solid #334155' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h2 style={{ margin: 0, fontSize: '20px' }}>Captured Transactions (PostgreSQL & Kafka)</h2>
            <button onClick={loadData} style={{ background: '#334155', color: '#fff', border: 'none', padding: '8px 14px', borderRadius: '6px', cursor: 'pointer' }}>Refresh</button>
          </div>
          <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
            <thead>
              <tr style={{ borderBottom: '1px solid #475569', color: '#94a3b8' }}>
                <th style={{ padding: '10px' }}>Txn ID</th>
                <th style={{ padding: '10px' }}>Product</th>
                <th style={{ padding: '10px' }}>Account</th>
                <th style={{ padding: '10px' }}>Quantity</th>
                <th style={{ padding: '10px' }}>Price</th>
                <th style={{ padding: '10px' }}>Status</th>
              </tr>
            </thead>
            <tbody>
              {transactions.map(t => (
                <tr key={t.transactionId} style={{ borderBottom: '1px solid #334155' }}>
                  <td style={{ padding: '10px', fontWeight: 600 }}>{t.transactionId}</td>
                  <td style={{ padding: '10px' }}>{t.productId}</td>
                  <td style={{ padding: '10px' }}>{t.accountId}</td>
                  <td style={{ padding: '10px' }}>{t.quantity}</td>
                  <td style={{ padding: '10px' }}>${t.price} {t.currency}</td>
                  <td style={{ padding: '10px' }}>
                    <span style={{ backgroundColor: '#065f46', color: '#34d399', padding: '4px 8px', borderRadius: '4px', fontSize: '12px' }}>
                      {t.status}
                    </span>
                  </td>
                </tr>
              ))}
              {transactions.length === 0 && (
                <tr><td colSpan={6} style={{ padding: '20px', textAlign: 'center', color: '#64748b' }}>No transactions recorded yet.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {activeTab === 'recon' && (
        <div style={{ backgroundColor: '#1e293b', padding: '24px', borderRadius: '8px', border: '1px solid #334155' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
            <h2 style={{ margin: 0, fontSize: '20px' }}>Reconciliation Runs (Sum vs Position Totals)</h2>
            <button
              onClick={handleTriggerRecon}
              disabled={loading}
              style={{ background: '#10b981', color: '#fff', border: 'none', padding: '10px 18px', borderRadius: '6px', fontWeight: 600, cursor: 'pointer' }}
            >
              Trigger New Reconciliation
            </button>
          </div>
          {reconRuns.map(run => (
            <div key={run.runId} style={{ backgroundColor: '#0f172a', padding: '16px', borderRadius: '8px', marginBottom: '16px', border: '1px solid #334155' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
                <div>
                  <span style={{ fontSize: '17px', fontWeight: 700, color: '#f8fafc', marginRight: '10px' }}>{run.runId}</span>
                  <span style={{ color: '#94a3b8', fontSize: '13px' }}>Date: {run.reconDate}</span>
                </div>
                <div>
                  <span style={{
                    backgroundColor: run.status === 'BALANCED' ? '#065f46' : '#991b1b',
                    color: run.status === 'BALANCED' ? '#34d399' : '#f87171',
                    padding: '4px 10px',
                    borderRadius: '4px',
                    fontSize: '13px',
                    fontWeight: 700,
                    marginRight: '8px'
                  }}>
                    {run.status}
                  </span>
                  {run.stale && (
                    <span style={{ backgroundColor: '#854d0e', color: '#fde047', padding: '4px 10px', borderRadius: '4px', fontSize: '13px', fontWeight: 700 }}>
                      STALE
                    </span>
                  )}
                </div>
              </div>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px', color: '#cbd5e1' }}>
                <thead>
                  <tr style={{ borderBottom: '1px solid #334155', color: '#64748b' }}>
                    <th style={{ padding: '6px' }}>Product</th>
                    <th style={{ padding: '6px' }}>Txn Total Qty</th>
                    <th style={{ padding: '6px' }}>Position Total Qty</th>
                    <th style={{ padding: '6px' }}>Discrepancy</th>
                    <th style={{ padding: '6px' }}>Line Status</th>
                  </tr>
                </thead>
                <tbody>
                  {run.results?.map(res => (
                    <tr key={res.productId} style={{ borderBottom: '1px solid #1e293b' }}>
                      <td style={{ padding: '6px' }}>{res.productId}</td>
                      <td style={{ padding: '6px' }}>{res.txnTotalQty}</td>
                      <td style={{ padding: '6px' }}>{res.posTotalQty}</td>
                      <td style={{ padding: '6px', color: res.discrepancyQty !== 0 ? '#f87171' : '#34d399' }}>{res.discrepancyQty}</td>
                      <td style={{ padding: '6px' }}>{res.status}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ))}
          {reconRuns.length === 0 && (
            <p style={{ textAlign: 'center', color: '#64748b' }}>No reconciliation runs executed yet. Click "Trigger New Reconciliation" above.</p>
          )}
        </div>
      )}

      {activeTab === 'stale' && (
        <div style={{ backgroundColor: '#1e293b', padding: '24px', borderRadius: '8px', border: '1px solid #334155' }}>
          <h2 style={{ marginTop: 0, color: '#fde047', fontSize: '20px' }}>Stale Runs Monitored by Stale Detection Service</h2>
          <p style={{ color: '#94a3b8' }}>
            These reconciliation runs were automatically flagged because late-arriving transactions were booked after reconciliation completed.
          </p>
          {staleRuns.map(run => (
            <div key={run.runId} style={{ backgroundColor: '#451a03', border: '1px solid #b45309', padding: '16px', borderRadius: '8px', marginBottom: '12px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ fontWeight: 700, color: '#fed7aa', fontSize: '16px' }}>{run.runId}</span>
                <span style={{ backgroundColor: '#b45309', color: '#fff', padding: '3px 8px', borderRadius: '4px', fontSize: '12px' }}>ACTION REQUIRED: RE-RUN RECON</span>
              </div>
              <p style={{ margin: '8px 0 0 0', color: '#fdba74', fontSize: '13px' }}>
                Recon Date: {run.reconDate} | Original Status: {run.status} | Invalidated At: {run.createdAt}
              </p>
            </div>
          ))}
          {staleRuns.length === 0 && (
            <p style={{ textAlign: 'center', color: '#34d399' }}>No stale runs detected! All completed reconciliations are current.</p>
          )}
        </div>
      )}
    </div>
  );
}
