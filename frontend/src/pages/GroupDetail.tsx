import React, { useState, useEffect } from 'react';
import { Group } from '../types';
import { api } from '../api/client';
import { ArrowLeft, Users, UserPlus, Shield, Calendar } from 'lucide-react';

interface GroupDetailProps {
  groupId: string;
  onBack: () => void;
}

export const GroupDetail: React.FC<GroupDetailProps> = ({ groupId, onBack }) => {
  const [group, setGroup] = useState<Group | null>(null);
  const [loading, setLoading] = useState(true);
  const [newUserId, setNewUserId] = useState('');
  const [addingMember, setAddingMember] = useState(false);
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

  useEffect(() => {
    fetchGroup();
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
    <div style={{ maxWidth: '900px', margin: '2rem auto', padding: '0 1rem' }}>
      <button onClick={onBack} className="btn-secondary" style={{ marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
        <ArrowLeft size={16} /> Back to Groups
      </button>

      {error && (
        <div style={{ background: 'rgba(239, 68, 68, 0.15)', border: '1px solid rgba(239, 68, 68, 0.3)', color: '#f87171', padding: '0.75rem 1rem', borderRadius: '10px', fontSize: '0.85rem', marginBottom: '1.5rem' }}>
          {error}
        </div>
      )}

      {/* Header Banner */}
      <div className="glass-panel" style={{ padding: '2rem', marginBottom: '2rem', position: 'relative', overflow: 'hidden' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
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
        </div>
      </div>

      {/* Roster & Add Member */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 340px', gap: '1.5rem' }}>
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
                  padding: '0.85rem 1rem',
                  background: 'rgba(15, 23, 42, 0.5)',
                  borderRadius: '10px',
                  border: '1px solid rgba(255,255,255,0.05)'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                  <div style={{ background: 'rgba(99, 102, 241, 0.2)', width: '36px', height: '36px', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#818cf8', fontWeight: 600, fontSize: '0.85rem' }}>
                    {member.userId.substring(0, 2).toUpperCase()}
                  </div>
                  <div>
                    <div style={{ color: '#f8fafc', fontSize: '0.9rem', fontWeight: 500, fontFamily: 'monospace' }}>
                      {member.userId}
                    </div>
                    <div style={{ color: '#64748b', fontSize: '0.75rem' }}>
                      Joined {new Date(member.joinedAt).toLocaleDateString()}
                    </div>
                  </div>
                </div>

                {member.userId === group.createdBy && (
                  <span style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', background: 'rgba(16, 185, 129, 0.15)', color: '#34d399', padding: '0.25rem 0.6rem', borderRadius: '20px', fontSize: '0.75rem', fontWeight: 600 }}>
                    <Shield size={12} /> Creator
                  </span>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Add Member Form */}
        <div className="glass-panel" style={{ padding: '1.5rem', height: 'fit-content' }}>
          <h3 style={{ fontSize: '1.15rem', fontWeight: 600, color: '#f8fafc', marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <UserPlus size={20} color="#10b981" /> Add Member
          </h3>
          <p style={{ color: '#94a3b8', fontSize: '0.85rem', marginBottom: '1.25rem' }}>
            Paste the User UUID to add them to this group.
          </p>

          <form onSubmit={handleAddMember} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div>
              <label className="label">User UUID</label>
              <input 
                type="text"
                required
                className="form-input"
                placeholder="e.g. 123e4567-e89b-12d3-a456-426614174000"
                value={newUserId}
                onChange={(e) => setNewUserId(e.target.value)}
              />
            </div>

            <button type="submit" disabled={addingMember} className="btn-primary" style={{ width: '100%', padding: '0.75rem' }}>
              {addingMember ? 'Adding...' : 'Add to Group'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};
