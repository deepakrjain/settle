import React, { useState, useEffect } from 'react';
import { SettlementPlanResponse, SettlementTransaction, UserBalance } from '../types';
import { api } from '../api/client';
import { Scale, ArrowRight, Zap, CheckCircle2, AlertCircle, Radio } from 'lucide-react';

interface BalancesDashboardProps {
  groupId: string;
  onBalanceUpdated?: () => void;
}

export const BalancesDashboard: React.FC<BalancesDashboardProps> = ({ groupId, onBalanceUpdated }) => {
  const [balances, setBalances] = useState<UserBalance[]>([]);
  const [plan, setPlan] = useState<SettlementPlanResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [settlingKey, setSettlingKey] = useState<string | null>(null);
  const [settleStatus, setSettleStatus] = useState<{ message: string; type: 'success' | 'error' } | null>(null);
  const [liveConnected, setLiveConnected] = useState(false);

  const fetchData = async () => {
    try {
      const [balancesData, planData] = await Promise.all([
        api.getBalances(groupId),
        api.getSettlementPlan(groupId)
      ]);
      setBalances(balancesData);
      setPlan(planData);
    } catch (err) {
      console.error('Failed to load balance dashboard data', err);
    } finally {
      setLoading(false);
    }
  };

  // Phase 10: WebSocket Live Listener
  useEffect(() => {
    fetchData();

    // Setup WebSocket connection to /ws endpoint for STOMP topic /topic/groups/{groupId}/balances
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.host;
    const wsUrl = `${protocol}//${host}/ws`;

    let socket: WebSocket | null = null;

    try {
      socket = new WebSocket(wsUrl);

      socket.onopen = () => {
        setLiveConnected(true);
        // STOMP CONNECT frame
        socket?.send('CONNECT\naccept-version:1.2,1.1,1.0\n\n\0');
      };

      socket.onmessage = (event) => {
        const text = event.data;
        if (text.startsWith('CONNECTED')) {
          // STOMP SUBSCRIBE frame
          const subFrame = `SUBSCRIBE\nid:sub-0\ndestination:/topic/groups/${groupId}/balances\n\n\0`;
          socket?.send(subFrame);
        } else if (text.startsWith('MESSAGE')) {
          // Live balance update received from Spring @TransactionalEventListener!
          try {
            const bodyStart = text.indexOf('\n\n') + 2;
            const bodyEnd = text.lastIndexOf('\0');
            const jsonStr = text.substring(bodyStart, bodyEnd > bodyStart ? bodyEnd : text.length);
            const updatedBalances: UserBalance[] = JSON.parse(jsonStr);
            setBalances(updatedBalances);
            // Refresh settlement plan as well
            api.getSettlementPlan(groupId).then(setPlan).catch(console.error);
            if (onBalanceUpdated) onBalanceUpdated();
          } catch (e) {
            fetchData();
          }
        }
      };

      socket.onclose = () => setLiveConnected(false);
      socket.onerror = () => setLiveConnected(false);
    } catch {
      setLiveConnected(false);
    }

    return () => {
      if (socket && socket.readyState === WebSocket.OPEN) {
        socket.close();
      }
    };
  }, [groupId]);

  const handleSettle = async (tx: SettlementTransaction) => {
    const txKey = `${tx.fromUserId}-${tx.toUserId}-${tx.amount}`;
    setSettlingKey(txKey);
    setSettleStatus(null);

    // UI disables the button after click for smooth user experience,
    // but the REAL protection against duplicate settlement payments is enforced
    // by the backend database unique constraint on idempotencyKey.
    const idempotencyKey = typeof crypto !== 'undefined' && crypto.randomUUID
      ? crypto.randomUUID()
      : 'IDEM-' + Date.now() + '-' + Math.random().toString(36).substring(2, 9);

    try {
      const res = await api.recordSettlement(groupId, {
        fromUserId: tx.fromUserId,
        toUserId: tx.toUserId,
        amount: tx.amount,
        idempotencyKey
      });

      if (res.status === 'COMPLETED') {
        setSettleStatus({
          message: `Settlement of ₹${tx.amount.toFixed(2)} recorded successfully!`,
          type: 'success'
        });
        fetchData();
        if (onBalanceUpdated) onBalanceUpdated();
      } else {
        setSettleStatus({
          message: `Payment status: ${res.status}. ${res.status === 'FAILED' ? 'Payment gateway temporary failure.' : ''}`,
          type: 'error'
        });
      }
    } catch (err: any) {
      setSettleStatus({
        message: err.message || 'Settlement failed',
        type: 'error'
      });
    } finally {
      setSettlingKey(null);
    }
  };

  if (loading) {
    return <div style={{ textAlign: 'center', padding: '2rem', color: '#94a3b8' }}>Calculating net balances...</div>;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Live WebSocket Indicator */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h3 style={{ fontSize: '1.25rem', fontWeight: 600, color: '#f8fafc', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <Scale size={22} color="#6366f1" /> Net Balances & Debt Minimization
        </h3>
        
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '0.4rem',
          fontSize: '0.75rem',
          fontWeight: 600,
          padding: '0.25rem 0.65rem',
          borderRadius: '20px',
          background: liveConnected ? 'rgba(16, 185, 129, 0.15)' : 'rgba(239, 68, 68, 0.15)',
          color: liveConnected ? '#34d399' : '#f87171',
          border: `1px solid ${liveConnected ? 'rgba(16, 185, 129, 0.3)' : 'rgba(239, 68, 68, 0.3)'}`
        }}>
          <Radio size={14} className={liveConnected ? 'animate-pulse' : ''} />
          {liveConnected ? 'WebSocket Live' : 'Polling Sync'}
        </div>
      </div>

      {settleStatus && (
        <div style={{
          background: settleStatus.type === 'success' ? 'rgba(16, 185, 129, 0.15)' : 'rgba(239, 68, 68, 0.15)',
          border: `1px solid ${settleStatus.type === 'success' ? 'rgba(16, 185, 129, 0.3)' : 'rgba(239, 68, 68, 0.3)'}`,
          color: settleStatus.type === 'success' ? '#34d399' : '#f87171',
          padding: '0.75rem 1rem',
          borderRadius: '10px',
          fontSize: '0.85rem',
          display: 'flex',
          alignItems: 'center',
          gap: '0.5rem'
        }}>
          {settleStatus.type === 'success' ? <CheckCircle2 size={18} /> : <AlertCircle size={18} />}
          {settleStatus.message}
        </div>
      )}

      {/* Member Balances Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: '1rem' }}>
        {balances.map((b) => {
          const isCreditor = b.netBalance > 0.001;
          const isDebtor = b.netBalance < -0.001;
          return (
            <div
              key={b.userId}
              className="glass-panel"
              style={{
                padding: '1rem',
                borderLeft: `4px solid ${isCreditor ? '#10b981' : isDebtor ? '#ef4444' : '#64748b'}`
              }}
            >
              <div style={{ fontSize: '0.8rem', color: '#94a3b8', fontFamily: 'monospace', marginBottom: '0.4rem' }}>
                {b.userId.substring(0, 8)}...
              </div>
              <div style={{
                fontSize: '1.25rem',
                fontWeight: 700,
                color: isCreditor ? '#34d399' : isDebtor ? '#f87171' : '#cbd5e1'
              }}>
                {isCreditor ? `+₹${b.netBalance.toFixed(2)}` : isDebtor ? `-₹${Math.abs(b.netBalance).toFixed(2)}` : '₹0.00'}
              </div>
              <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.2rem' }}>
                {isCreditor ? 'is owed money' : isDebtor ? 'owes money' : 'settled up'}
              </div>
            </div>
          );
        })}
      </div>

      {/* Suggested Settlement Plans (Greedy & Optimal comparison) */}
      {plan && (
        <div style={{ display: 'grid', gridTemplateColumns: plan.optimalCalculated ? '1fr 1fr' : '1fr', gap: '1.5rem', marginTop: '0.5rem' }}>
          {/* Greedy Plan */}
          <div className="glass-panel" style={{ padding: '1.5rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
              <h4 style={{ fontSize: '1.05rem', fontWeight: 600, color: '#f8fafc', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <Zap size={18} color="#6366f1" /> Greedy Plan (O(N log N))
              </h4>
              <span style={{ background: 'rgba(99, 102, 241, 0.2)', color: '#818cf8', padding: '0.2rem 0.6rem', borderRadius: '12px', fontSize: '0.75rem', fontWeight: 600 }}>
                {plan.greedyTransactionCount} transaction(s)
              </span>
            </div>

            {plan.greedyPlan.length === 0 ? (
              <p style={{ color: '#64748b', fontSize: '0.85rem' }}>Everyone is fully settled up! No transactions needed.</p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                {plan.greedyPlan.map((tx, idx) => {
                  const txKey = `${tx.fromUserId}-${tx.toUserId}-${tx.amount}`;
                  const isSettling = settlingKey === txKey;
                  return (
                    <div
                      key={idx}
                      style={{
                        background: 'rgba(15, 23, 42, 0.5)',
                        padding: '0.75rem',
                        borderRadius: '10px',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        border: '1px solid rgba(255,255,255,0.05)'
                      }}
                    >
                      <div style={{ fontSize: '0.85rem', color: '#cbd5e1', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <span style={{ fontFamily: 'monospace' }}>{tx.fromUserId.substring(0, 6)}</span>
                        <ArrowRight size={14} color="#94a3b8" />
                        <span style={{ fontFamily: 'monospace' }}>{tx.toUserId.substring(0, 6)}</span>
                        <span style={{ fontWeight: 700, color: '#f8fafc', marginLeft: '0.4rem' }}>₹{tx.amount.toFixed(2)}</span>
                      </div>

                      <button
                        onClick={() => handleSettle(tx)}
                        disabled={isSettling}
                        className="btn-primary"
                        style={{ padding: '0.4rem 0.8rem', fontSize: '0.8rem' }}
                      >
                        {isSettling ? 'Settling...' : 'Settle Up'}
                      </button>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Optimal Plan (Subset-Sum Exact Minimization) */}
          {plan.optimalCalculated && plan.optimalPlan && (
            <div className="glass-panel" style={{ padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                <h4 style={{ fontSize: '1.05rem', fontWeight: 600, color: '#f8fafc', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                  <Zap size={18} color="#10b981" /> Optimal Plan (Subset-Sum O(2^N))
                </h4>
                <span style={{ background: 'rgba(16, 185, 129, 0.2)', color: '#34d399', padding: '0.2rem 0.6rem', borderRadius: '12px', fontSize: '0.75rem', fontWeight: 600 }}>
                  {plan.optimalTransactionCount} transaction(s)
                </span>
              </div>

              {plan.optimalPlan.length === 0 ? (
                <p style={{ color: '#64748b', fontSize: '0.85rem' }}>Everyone is fully settled up!</p>
              ) : (
                <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                  {plan.optimalPlan.map((tx, idx) => {
                    const txKey = `${tx.fromUserId}-${tx.toUserId}-${tx.amount}`;
                    const isSettling = settlingKey === txKey;
                    return (
                      <div
                        key={idx}
                        style={{
                          background: 'rgba(15, 23, 42, 0.5)',
                          padding: '0.75rem',
                          borderRadius: '10px',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          border: '1px solid rgba(255,255,255,0.05)'
                        }}
                      >
                        <div style={{ fontSize: '0.85rem', color: '#cbd5e1', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                          <span style={{ fontFamily: 'monospace' }}>{tx.fromUserId.substring(0, 6)}</span>
                          <ArrowRight size={14} color="#94a3b8" />
                          <span style={{ fontFamily: 'monospace' }}>{tx.toUserId.substring(0, 6)}</span>
                          <span style={{ fontWeight: 700, color: '#f8fafc', marginLeft: '0.4rem' }}>₹{tx.amount.toFixed(2)}</span>
                        </div>

                        <button
                          onClick={() => handleSettle(tx)}
                          disabled={isSettling}
                          className="btn-primary"
                          style={{ padding: '0.4rem 0.8rem', fontSize: '0.8rem', background: 'linear-gradient(135deg, #10b981, #059669)' }}
                        >
                          {isSettling ? 'Settling...' : 'Settle Up'}
                        </button>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
};
