import React, { useState } from 'react';
import { CreateExpensePayload, GroupMember, ItemizedItem, SplitType } from '../types';
import { api } from '../api/client';
import { X, AlertCircle, Plus, Trash2, CheckCircle2 } from 'lucide-react';

interface CreateExpenseModalProps {
  groupId: string;
  members: GroupMember[];
  onClose: () => void;
  onSuccess: () => void;
}

export const CreateExpenseModal: React.FC<CreateExpenseModalProps> = ({
  groupId,
  members,
  onClose,
  onSuccess
}) => {
  const [description, setDescription] = useState('');
  const [amount, setAmount] = useState('');
  const [category, setCategory] = useState('Dining');
  const [paidByUserId, setPaidByUserId] = useState(members[0]?.userId || '');
  const [splitType, setSplitType] = useState<SplitType>('EQUAL');

  // Selected participants for EQUAL, PERCENTAGE, EXACT, SHARES
  const [selectedParticipants, setSelectedParticipants] = useState<string[]>(
    members.map((m) => m.userId)
  );

  // Split type specific states
  const [percentages, setPercentages] = useState<Record<string, string>>({});
  const [exactAmounts, setExactAmounts] = useState<Record<string, string>>({});
  const [shares, setShares] = useState<Record<string, string>>({});

  // Itemized state
  const [items, setItems] = useState<ItemizedItem[]>([
    { itemName: 'Item 1', amount: 0, participantUserIds: members.map((m) => m.userId) }
  ]);
  const [taxAndTip, setTaxAndTip] = useState('0');

  const [loading, setLoading] = useState(false);
  const [clientError, setClientError] = useState<string | null>(null);

  const toggleParticipant = (userId: string) => {
    if (selectedParticipants.includes(userId)) {
      setSelectedParticipants(selectedParticipants.filter((id) => id !== userId));
    } else {
      setSelectedParticipants([...selectedParticipants, userId]);
    }
  };

  // Client-side validation matching backend rules
  const validate = (): CreateExpensePayload | null => {
    setClientError(null);

    if (!description.trim()) {
      setClientError('Description is required.');
      return null;
    }

    if (!paidByUserId) {
      setClientError('Please select who paid for this expense.');
      return null;
    }

    const numericAmount = parseFloat(amount);

    if (splitType !== 'ITEMIZED') {
      if (isNaN(numericAmount) || numericAmount <= 0) {
        setClientError('Amount must be a positive number.');
        return null;
      }
      if (selectedParticipants.length === 0) {
        setClientError('Select at least one participant.');
        return null;
      }
    }

    if (splitType === 'EQUAL') {
      return {
        paidByUserId,
        amount: numericAmount,
        currency: 'INR',
        description: description.trim(),
        category,
        splitType: 'EQUAL',
        participantUserIds: selectedParticipants
      };
    }

    if (splitType === 'PERCENTAGE') {
      let sum = 0;
      const parsedMap: Record<string, number> = {};
      for (const userId of selectedParticipants) {
        const val = parseFloat(percentages[userId] || '0');
        if (isNaN(val) || val < 0) {
          setClientError('Percentage must be non-negative.');
          return null;
        }
        sum += val;
        parsedMap[userId] = val;
      }

      if (Math.abs(sum - 100) > 0.01) {
        setClientError(`Percentages sum to ${sum.toFixed(2)}%, but must equal exactly 100%.`);
        return null;
      }

      return {
        paidByUserId,
        amount: numericAmount,
        currency: 'INR',
        description: description.trim(),
        category,
        splitType: 'PERCENTAGE',
        percentages: parsedMap
      };
    }

    if (splitType === 'EXACT') {
      let sum = 0;
      const parsedMap: Record<string, number> = {};
      for (const userId of selectedParticipants) {
        const val = parseFloat(exactAmounts[userId] || '0');
        if (isNaN(val) || val < 0) {
          setClientError('Exact amount must be non-negative.');
          return null;
        }
        sum += val;
        parsedMap[userId] = val;
      }

      if (Math.abs(sum - numericAmount) > 0.01) {
        setClientError(`Exact shares sum to ₹${sum.toFixed(2)}, but must equal total expense ₹${numericAmount.toFixed(2)}.`);
        return null;
      }

      return {
        paidByUserId,
        amount: numericAmount,
        currency: 'INR',
        description: description.trim(),
        category,
        splitType: 'EXACT',
        exactAmounts: parsedMap
      };
    }

    if (splitType === 'SHARES') {
      let totalShares = 0;
      const parsedMap: Record<string, number> = {};
      for (const userId of selectedParticipants) {
        const val = parseInt(shares[userId] || '1', 10);
        if (isNaN(val) || val <= 0) {
          setClientError('Share count must be at least 1.');
          return null;
        }
        totalShares += val;
        parsedMap[userId] = val;
      }

      if (totalShares <= 0) {
        setClientError('Total shares must be greater than 0.');
        return null;
      }

      return {
        paidByUserId,
        amount: numericAmount,
        currency: 'INR',
        description: description.trim(),
        category,
        splitType: 'SHARES',
        shares: parsedMap
      };
    }

    if (splitType === 'ITEMIZED') {
      if (items.length === 0) {
        setClientError('Add at least one item receipt.');
        return null;
      }

      for (let i = 0; i < items.length; i++) {
        const item = items[i];
        if (!item.itemName.trim()) {
          setClientError(`Item #${i + 1} name is required.`);
          return null;
        }
        if (item.amount <= 0) {
          setClientError(`Item #${i + 1} amount must be greater than 0.`);
          return null;
        }
        if (item.participantUserIds.length === 0) {
          setClientError(`Item #${i + 1} must have at least one participant.`);
          return null;
        }
      }

      const parsedTaxTip = parseFloat(taxAndTip) || 0;

      return {
        paidByUserId,
        amount: isNaN(numericAmount) ? undefined : numericAmount,
        currency: 'INR',
        description: description.trim(),
        category,
        splitType: 'ITEMIZED',
        items,
        taxAndTip: parsedTaxTip
      };
    }

    return null;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const payload = validate();
    if (!payload) return;

    setLoading(true);
    try {
      await api.createExpense(groupId, payload);
      onSuccess();
      onClose();
    } catch (err: any) {
      setClientError(err.message || 'Failed to create expense');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      background: 'rgba(0, 0, 0, 0.75)',
      backdropFilter: 'blur(8px)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 100,
      padding: '1rem'
    }}>
      <div className="glass-panel" style={{ width: '100%', maxWidth: '650px', maxHeight: '90vh', overflowY: 'auto', padding: '2rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 700, color: '#f8fafc' }}>Log New Expense</h2>
          <button onClick={onClose} style={{ background: 'none', border: 'none', color: '#94a3b8', cursor: 'pointer' }}>
            <X size={24} />
          </button>
        </div>

        {clientError && (
          <div style={{ background: 'rgba(239, 68, 68, 0.15)', border: '1px solid rgba(239, 68, 68, 0.3)', color: '#f87171', padding: '0.75rem 1rem', borderRadius: '10px', fontSize: '0.85rem', marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <AlertCircle size={18} />
            {clientError}
          </div>
        )}

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div>
              <label className="label">Description</label>
              <input 
                type="text"
                required
                className="form-input"
                placeholder="Dinner, Taxi, Grocery bill..."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
              />
            </div>

            <div>
              <label className="label">Category</label>
              <select className="form-input" value={category} onChange={(e) => setCategory(e.target.value)}>
                <option value="Dining">Dining</option>
                <option value="Travel">Travel</option>
                <option value="Rent">Rent</option>
                <option value="Utilities">Utilities</option>
                <option value="Entertainment">Entertainment</option>
                <option value="General">General</option>
              </select>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
            <div>
              <label className="label">Amount (₹)</label>
              <input 
                type="number"
                step="0.01"
                required={splitType !== 'ITEMIZED'}
                className="form-input"
                placeholder="100.00"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
              />
            </div>

            <div>
              <label className="label">Paid By</label>
              <select className="form-input" value={paidByUserId} onChange={(e) => setPaidByUserId(e.target.value)}>
                {members.map((m) => (
                  <option key={m.userId} value={m.userId}>
                    {m.userDisplayName ? `${m.userDisplayName} (${m.userEmail || m.userId.substring(0, 6)})` : m.userId}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Split Type Selector */}
          <div>
            <label className="label">Split Type</label>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: '0.5rem' }}>
              {(['EQUAL', 'PERCENTAGE', 'EXACT', 'SHARES', 'ITEMIZED'] as SplitType[]).map((type) => (
                <button
                  type="button"
                  key={type}
                  onClick={() => setSplitType(type)}
                  style={{
                    padding: '0.6rem 0.2rem',
                    borderRadius: '8px',
                    fontSize: '0.75rem',
                    fontWeight: 600,
                    border: '1px solid',
                    borderColor: splitType === type ? '#6366f1' : 'rgba(255,255,255,0.1)',
                    background: splitType === type ? 'rgba(99, 102, 241, 0.2)' : 'rgba(15, 23, 42, 0.5)',
                    color: splitType === type ? '#818cf8' : '#94a3b8',
                    cursor: 'pointer'
                  }}
                >
                  {type}
                </button>
              ))}
            </div>
          </div>

          {/* Strategy Specific Dynamic Inputs */}
          {splitType !== 'ITEMIZED' && (
            <div style={{ background: 'rgba(15, 23, 42, 0.4)', padding: '1rem', borderRadius: '12px', border: '1px solid rgba(255,255,255,0.05)' }}>
              <label className="label" style={{ marginBottom: '0.75rem' }}>Split Participants</label>
              
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
                {members.map((m) => {
                  const isChecked = selectedParticipants.includes(m.userId);
                  const nameDisplay = m.userDisplayName || m.userId;
                  return (
                    <div key={m.userId} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '1rem' }}>
                      <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#f8fafc', fontSize: '0.85rem', cursor: 'pointer' }}>
                        <input 
                          type="checkbox"
                          checked={isChecked}
                          onChange={() => toggleParticipant(m.userId)}
                        />
                        {nameDisplay}
                      </label>

                      {isChecked && splitType === 'PERCENTAGE' && (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                          <input 
                            type="number"
                            step="0.1"
                            className="form-input"
                            style={{ width: '80px', padding: '0.35rem 0.5rem', fontSize: '0.85rem' }}
                            placeholder="%"
                            value={percentages[m.userId] || ''}
                            onChange={(e) => setPercentages({ ...percentages, [m.userId]: e.target.value })}
                          />
                          <span style={{ color: '#94a3b8', fontSize: '0.8rem' }}>%</span>
                        </div>
                      )}

                      {isChecked && splitType === 'EXACT' && (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                          <span style={{ color: '#94a3b8', fontSize: '0.8rem' }}>₹</span>
                          <input 
                            type="number"
                            step="0.01"
                            className="form-input"
                            style={{ width: '100px', padding: '0.35rem 0.5rem', fontSize: '0.85rem' }}
                            placeholder="0.00"
                            value={exactAmounts[m.userId] || ''}
                            onChange={(e) => setExactAmounts({ ...exactAmounts, [m.userId]: e.target.value })}
                          />
                        </div>
                      )}

                      {isChecked && splitType === 'SHARES' && (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                          <input 
                            type="number"
                            min="1"
                            className="form-input"
                            style={{ width: '70px', padding: '0.35rem 0.5rem', fontSize: '0.85rem' }}
                            placeholder="1"
                            value={shares[m.userId] || '1'}
                            onChange={(e) => setShares({ ...shares, [m.userId]: e.target.value })}
                          />
                          <span style={{ color: '#94a3b8', fontSize: '0.8rem' }}>share(s)</span>
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* Itemized Receipts Builder */}
          {splitType === 'ITEMIZED' && (
            <div style={{ background: 'rgba(15, 23, 42, 0.4)', padding: '1rem', borderRadius: '12px', border: '1px solid rgba(255,255,255,0.05)' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
                <label className="label">Itemized Items</label>
                <button
                  type="button"
                  onClick={() => setItems([...items, { itemName: `Item ${items.length + 1}`, amount: 0, participantUserIds: members.map(m => m.userId) }])}
                  className="btn-secondary"
                  style={{ padding: '0.35rem 0.75rem', fontSize: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.3rem' }}
                >
                  <Plus size={14} /> Add Item
                </button>
              </div>

              {items.map((item, idx) => (
                <div key={idx} style={{ background: 'rgba(30, 41, 59, 0.5)', padding: '0.75rem', borderRadius: '8px', marginBottom: '0.75rem', border: '1px solid rgba(255,255,255,0.05)' }}>
                  <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.5rem' }}>
                    <input 
                      type="text"
                      className="form-input"
                      style={{ flex: 2, padding: '0.35rem 0.6rem', fontSize: '0.85rem' }}
                      placeholder="Item name"
                      value={item.itemName}
                      onChange={(e) => {
                        const copy = [...items];
                        copy[idx].itemName = e.target.value;
                        setItems(copy);
                      }}
                    />
                    <input 
                      type="number"
                      step="0.01"
                      className="form-input"
                      style={{ flex: 1, padding: '0.35rem 0.6rem', fontSize: '0.85rem' }}
                      placeholder="Amount ₹"
                      value={item.amount || ''}
                      onChange={(e) => {
                        const copy = [...items];
                        copy[idx].amount = parseFloat(e.target.value) || 0;
                        setItems(copy);
                      }}
                    />
                    {items.length > 1 && (
                      <button 
                        type="button"
                        onClick={() => setItems(items.filter((_, i) => i !== idx))}
                        style={{ background: 'none', border: 'none', color: '#ef4444', padding: '0 0.25rem', cursor: 'pointer' }}
                      >
                        <Trash2 size={16} />
                      </button>
                    )}
                  </div>

                  <div style={{ fontSize: '0.75rem', color: '#94a3b8' }}>
                    Split item with:
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem', marginTop: '0.25rem' }}>
                      {members.map((m) => {
                        const inItem = item.participantUserIds.includes(m.userId);
                        return (
                          <button
                            type="button"
                            key={m.userId}
                            onClick={() => {
                              const copy = [...items];
                              if (inItem) {
                                copy[idx].participantUserIds = copy[idx].participantUserIds.filter(id => id !== m.userId);
                              } else {
                                copy[idx].participantUserIds = [...copy[idx].participantUserIds, m.userId];
                              }
                              setItems(copy);
                            }}
                            style={{
                              padding: '0.2rem 0.5rem',
                              borderRadius: '4px',
                              fontSize: '0.7rem',
                              border: '1px solid',
                              borderColor: inItem ? '#10b981' : 'rgba(255,255,255,0.1)',
                              background: inItem ? 'rgba(16, 185, 129, 0.2)' : 'transparent',
                              color: inItem ? '#34d399' : '#64748b'
                            }}
                          >
                            {m.userDisplayName || m.userId.substring(0, 6)}
                          </button>
                        );
                      })}
                    </div>
                  </div>
                </div>
              ))}

              <div style={{ marginTop: '0.75rem' }}>
                <label className="label">Tax & Tip Amount (₹)</label>
                <input 
                  type="number"
                  step="0.01"
                  className="form-input"
                  placeholder="0.00"
                  value={taxAndTip}
                  onChange={(e) => setTaxAndTip(e.target.value)}
                />
              </div>
            </div>
          )}

          <button 
            type="submit" 
            disabled={loading}
            className="btn-primary"
            style={{ width: '100%', padding: '0.8rem', marginTop: '0.5rem', display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '0.5rem' }}
          >
            {loading ? 'Logging Expense...' : (
              <>
                <CheckCircle2 size={18} /> Save Expense
              </>
            )}
          </button>
        </form>
      </div>
    </div>
  );
};
