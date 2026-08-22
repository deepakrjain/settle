import React, { useState, useEffect } from 'react';
import { ExpenseResponse, Group } from '../types';
import { api } from '../api/client';
import { CreateExpenseModal } from '../components/CreateExpenseModal';
import { ExpenseList } from '../components/ExpenseList';
import { ArrowLeft, Users, UserPlus, Shield, Calendar, PlusCircle, Receipt } from 'lucide-react';

interface GroupDetailProps {
  groupId: string;
  onBack: () => void;
}

export const GroupDetail: React.FC<GroupDetailProps> = ({ groupId, onBack }) => {
  const [group, setGroup] = useState<Group | null>(null);
  const [expenses, setExpenses] = useState<ExpenseResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [expensesLoading, setExpensesLoading] = useState(true);

  const [newUserId, setNewUserId] = useState('');
  const [addingMember, setAddingMember] = useState(false);
  const [showCreateExpense, setShowCreateExpense] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchGroup = async () => {
    try {
      const data = await api.getGroupDetails(groupId);
      setGroup(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load group details');
    } finally {
      setLoading(false);
    }
  };

  const fetchExpenses = async () => {
    setExpensesLoading(true);
    try {
      const pageRes = await api.getGroupExpenses(groupId);
      setExpenses(pageRes.content);
    } catch (err: any) {
      console.error('Failed to load expenses', err);
    } finally {
      setExpensesLoading(false);
    }
  };

  useEffect(() => {
    fetchGroup();
    fetchExpenses();
  }, [groupId]);

  const handleAddMember = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newUserId.trim()) return;
    setAddingMember(true);
    setError(null);
    try {
      await api.addMember(groupId, newUserId.trim());
      setNewUserId('');
      fetchGroup();
    } catch (err: any) {
      setError(err.message || 'Failed to add member');
    } finally {
      setAddingMember(false);
    }
  };

  if (loading) {
    return <div style={{ textAlign: 'center', padding: '4rem', color: '#94a3b8' }}>Loading group...</div>;
  }

  if (!group) {
    return (
      <div style={{ maxWidth: '800px', margin: '2rem auto', padding: '0 1rem' }}>
        <button onClick={onBack} className="btn-secondary" style={{ marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
          <ArrowLeft size={16} /> Back to Groups
        </button>
        <div className="glass-panel" style={{ padding: '2rem', color: '#f87171' }}>
          {error || 'Group not found'}
        </div>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: '1000px', margin: '2rem auto', padding: '0 1rem' }}>
      <button onClick={onBack} className="btn-secondary" style={{ marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
        <ArrowLeft size={16} /> Back to Groups
      </button>

      {error && (
        <div style={{ background: 'rgba(239, 68, 68, 0.15)', border: '1px solid rgba(239, 68, 68, 0.3)', color: '#f87171', padding: '0.75rem 1rem', borderRadius: '10px', fontSize: '0.85rem', marginBottom: '1.5rem' }}>
          {error}
        </div>
      )}

      {/* Header Banner */}
      <div className="glass-panel" style={{ padding: '2rem', marginBottom: '2rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <h1 style={{ fontSize: '2.25rem', fontWeight: 700, color: '#f8fafc', marginBottom: '0.5rem' }}>{group.name}</h1>
            <div style={{ display: 'flex', gap: '1.5rem', color: '#94a3b8', fontSize: '0.9rem' }}>
              <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <Calendar size={16} /> Created {new Date(group.createdAt).toLocaleDateString()}
              </span>
              <span style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <Users size={16} /> {group.members.length} Members
              </span>
            </div>
          </div>

          <button 
            onClick={() => setShowCreateExpense(true)}
            className="btn-primary"
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', padding: '0.85rem 1.4rem', fontSize: '0.95rem' }}
          >
            <PlusCircle size={20} /> Log Expense
          </button>
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 340px', gap: '1.5rem' }}>
        {/* Main Content: Expense List */}
        <div>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 600, color: '#f8fafc', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Receipt size={22} color="#6366f1" /> Expense History
            </h3>
          </div>

          <ExpenseList expenses={expenses} loading={expensesLoading} />
        </div>

        {/* Sidebar: Roster & Add Member */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {/* Members Roster */}
          <div className="glass-panel" style={{ padding: '1.5rem' }}>
            <h3 style={{ fontSize: '1.15rem', fontWeight: 600, color: '#f8fafc', marginBottom: '1.25rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Users size={20} color="#6366f1" /> Member Roster
            </h3>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              {group.members.map((member) => (
                <div 
                  key={member.id}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justify: 'space-between',
                    padding: '0.75rem 0.85rem',
                    background: 'rgba(15, 23, 42, 0.5)',
                    borderRadius: '10px',
                    border: '1px solid rgba(255,255,255,0.05)'
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                    <div style={{ background: 'rgba(99, 102, 241, 0.2)', width: '32px', height: '32px', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#818cf8', fontWeight: 600, fontSize: '0.8rem' }}>
                      {member.userId.substring(0, 2).toUpperCase()}
                    </div>
                    <div>
                      <div style={{ color: '#f8fafc', fontSize: '0.85rem', fontWeight: 500, fontFamily: 'monospace' }}>
                        {member.userId.substring(0, 8)}...
                      </div>
                    </div>
                  </div>

                  {member.userId === group.createdBy && (
                    <span style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', background: 'rgba(16, 185, 129, 0.15)', color: '#34d399', padding: '0.2rem 0.5rem', borderRadius: '20px', fontSize: '0.7rem', fontWeight: 600 }}>
                      <Shield size={10} /> Creator
                    </span>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Add Member Form */}
          <div className="glass-panel" style={{ padding: '1.5rem' }}>
            <h3 style={{ fontSize: '1.15rem', fontWeight: 600, color: '#f8fafc', marginBottom: '0.75rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <UserPlus size={20} color="#10b981" /> Add Member
            </h3>
            <p style={{ color: '#94a3b8', fontSize: '0.8rem', marginBottom: '1rem' }}>
              Paste User UUID to add to roster.
            </p>

            <form onSubmit={handleAddMember} style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
              <input 
                type="text"
                required
                className="form-input"
                placeholder="User UUID"
                value={newUserId}
                onChange={(e) => setNewUserId(e.target.value)}
              />

              <button type="submit" disabled={addingMember} className="btn-primary" style={{ width: '100%', padding: '0.65rem' }}>
                {addingMember ? 'Adding...' : 'Add Member'}
              </button>
            </form>
          </div>
        </div>
      </div>

      {/* Log Expense Modal */}
      {showCreateExpense && (
        <CreateExpenseModal 
          groupId={groupId}
          members={group.members}
          onClose={() => setShowCreateExpense(false)}
          onSuccess={() => {
            fetchExpenses();
          }}
        />
      )}
    </div>
  );
};
