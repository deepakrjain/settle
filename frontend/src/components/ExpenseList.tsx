import React, { useState } from 'react';
import { ExpenseResponse } from '../types';
import { Receipt, Tag, ChevronDown, ChevronUp, UserCheck } from 'lucide-react';

interface ExpenseListProps {
  expenses: ExpenseResponse[];
  loading: boolean;
}

export const ExpenseList: React.FC<ExpenseListProps> = ({ expenses, loading }) => {
  const [expandedId, setExpandedId] = useState<string | null>(null);

  const toggleExpand = (id: string) => {
    setExpandedId(expandedId === id ? null : id);
  };

  if (loading) {
    return <div style={{ textAlign: 'center', padding: '2rem', color: '#94a3b8' }}>Loading expense history...</div>;
  }

  if (expenses.length === 0) {
    return (
      <div style={{ textAlign: 'center', padding: '3rem 1rem', color: '#64748b' }}>
        <Receipt size={40} style={{ marginBottom: '0.75rem', opacity: 0.5 }} />
        <p style={{ fontSize: '0.95rem' }}>No expenses logged yet in this group.</p>
      </div>
    );
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
      {expenses.map((expense) => {
        const isExpanded = expandedId === expense.id;
        return (
          <div 
            key={expense.id}
            className="glass-panel"
            style={{ padding: '1.25rem', transition: 'all 0.2s ease' }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                <div style={{ background: 'rgba(99, 102, 241, 0.15)', padding: '0.75rem', borderRadius: '12px', color: '#818cf8' }}>
                  <Receipt size={24} />
                </div>

                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem', marginBottom: '0.25rem' }}>
                    <h4 style={{ fontSize: '1.1rem', fontWeight: 600, color: '#f8fafc' }}>{expense.description}</h4>
                    <span style={{
                      background: 'rgba(255, 255, 255, 0.08)',
                      color: '#94a3b8',
                      padding: '0.15rem 0.5rem',
                      borderRadius: '12px',
                      fontSize: '0.75rem',
                      fontWeight: 500,
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.3rem'
                    }}>
                      <Tag size={12} /> {expense.category}
                    </span>
                    <span style={{
                      background: 'rgba(99, 102, 241, 0.2)',
                      color: '#818cf8',
                      padding: '0.15rem 0.5rem',
                      borderRadius: '12px',
                      fontSize: '0.75rem',
                      fontWeight: 600
                    }}>
                      {expense.splitType}
                    </span>
                  </div>

                  <div style={{ fontSize: '0.85rem', color: '#94a3b8', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                    <UserCheck size={14} /> Paid by <span style={{ color: '#f8fafc', fontWeight: 500, fontFamily: 'monospace' }}>{expense.paidByUserId.substring(0, 8)}...</span>
                  </div>
                </div>
              </div>

              <div style={{ textAlign: 'right', display: 'flex', alignItems: 'center', gap: '1rem' }}>
                <div>
                  <div style={{ fontSize: '1.25rem', fontWeight: 700, color: '#f8fafc' }}>
                    ₹{expense.amount.toFixed(2)}
                  </div>
                  <div style={{ fontSize: '0.75rem', color: '#64748b' }}>
                    {new Date(expense.createdAt).toLocaleDateString()}
                  </div>
                </div>

                <button 
                  onClick={() => toggleExpand(expense.id)}
                  style={{ background: 'rgba(255,255,255,0.05)', border: 'none', color: '#94a3b8', padding: '0.4rem', borderRadius: '8px', cursor: 'pointer' }}
                >
                  {isExpanded ? <ChevronUp size={20} /> : <ChevronDown size={20} />}
                </button>
              </div>
            </div>

            {/* Expandable Split Breakdown */}
            {isExpanded && (
              <div style={{ marginTop: '1rem', paddingTop: '1rem', borderTop: '1px solid rgba(255,255,255,0.08)' }}>
                <div style={{ fontSize: '0.8rem', fontWeight: 600, color: '#94a3b8', marginBottom: '0.6rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                  Split Breakdown ({expense.splits.length} participants)
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: '0.5rem' }}>
                  {expense.splits.map((split) => (
                    <div 
                      key={split.id}
                      style={{
                        background: 'rgba(15, 23, 42, 0.5)',
                        padding: '0.6rem 0.8rem',
                        borderRadius: '8px',
                        display: 'flex',
                        justify: 'space-between',
                        alignItems: 'center',
                        fontSize: '0.85rem'
                      }}
                    >
                      <span style={{ color: '#cbd5e1', fontFamily: 'monospace' }}>
                        {split.userId.substring(0, 8)}...
                      </span>
                      <span style={{ fontWeight: 600, color: '#f8fafc' }}>
                        ₹{split.shareAmount.toFixed(2)}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
};
