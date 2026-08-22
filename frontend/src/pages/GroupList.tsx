import React, { useState, useEffect } from 'react';
import { Group } from '../types';
import { api } from '../api/client';
import { Users, Plus, ChevronRight, Layers } from 'lucide-react';

interface GroupListProps {
  onSelectGroup: (groupId: string) => void;
}

export const GroupList: React.FC<GroupListProps> = ({ onSelectGroup }) => {
  const [groups, setGroups] = useState<Group[]>([]);
  const [loading, setLoading] = useState(true);
  const [newGroupName, setNewGroupName] = useState('');
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchGroups = async () => {
    try {
      const data = await api.getGroups();
      setGroups(data);
    } catch (err: any) {
      setError(err.message || 'Failed to load groups');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchGroups();
  }, []);

  const handleCreateGroup = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newGroupName.trim()) return;
    setCreating(true);
    setError(null);
    try {
      await api.createGroup(newGroupName.trim());
      setNewGroupName('');
      fetchGroups();
    } catch (err: any) {
      setError(err.message || 'Failed to create group');
    } finally {
      setCreating(false);
    }
  };

  return (
    <div style={{ maxWidth: '900px', margin: '2rem auto', padding: '0 1rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
        <div>
          <h1 style={{ fontSize: '2rem', fontWeight: 700, color: '#f8fafc', marginBottom: '0.25rem' }}>My Groups</h1>
          <p style={{ color: '#94a3b8', fontSize: '0.95rem' }}>Select a group to manage shared expenses and view net balances</p>
        </div>
      </div>

      {error && (
        <div style={{ background: 'rgba(239, 68, 68, 0.15)', border: '1px solid rgba(239, 68, 68, 0.3)', color: '#f87171', padding: '0.75rem 1rem', borderRadius: '10px', fontSize: '0.85rem', marginBottom: '1.5rem' }}>
          {error}
        </div>
      )}

      {/* Create Group Form */}
      <div className="glass-panel" style={{ padding: '1.5rem', marginBottom: '2rem' }}>
        <h3 style={{ fontSize: '1.1rem', fontWeight: 600, color: '#f8fafc', marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <Plus size={20} color="#6366f1" /> Create New Group
        </h3>
        <form onSubmit={handleCreateGroup} style={{ display: 'flex', gap: '1rem' }}>
          <input 
            type="text"
            required
            className="form-input"
            placeholder="e.g. Goa Trip 2026, Apartment 4B Utilities"
            value={newGroupName}
            onChange={(e) => setNewGroupName(e.target.value)}
          />
          <button type="submit" disabled={creating} className="btn-primary" style={{ whiteSpace: 'nowrap' }}>
            {creating ? 'Creating...' : 'Create Group'}
          </button>
        </form>
      </div>

      {/* Groups List */}
      {loading ? (
        <div style={{ textAlign: 'center', padding: '3rem', color: '#94a3b8' }}>Loading groups...</div>
      ) : groups.length === 0 ? (
        <div className="glass-panel" style={{ textAlign: 'center', padding: '4rem 2rem' }}>
          <Layers size={48} color="#64748b" style={{ marginBottom: '1rem' }} />
          <h3 style={{ fontSize: '1.25rem', fontWeight: 600, color: '#f8fafc', marginBottom: '0.5rem' }}>No groups yet</h3>
          <p style={{ color: '#94a3b8', fontSize: '0.9rem' }}>Create your first group above to start splitting expenses!</p>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: '1.25rem' }}>
          {groups.map((group) => (
            <div 
              key={group.id}
              className="glass-panel"
              onClick={() => onSelectGroup(group.id)}
              style={{
                padding: '1.5rem',
                cursor: 'pointer',
                transition: 'all 0.25 ease',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between'
              }}
            >
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '1rem' }}>
                  <h3 style={{ fontSize: '1.2rem', fontWeight: 600, color: '#f8fafc' }}>{group.name}</h3>
                  <ChevronRight size={20} color="#94a3b8" />
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#94a3b8', fontSize: '0.85rem', marginBottom: '0.5rem' }}>
                  <Users size={16} />
                  <span>{group.members ? group.members.length : 1} Members</span>
                </div>
              </div>
              <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '1rem', borderTop: '1px solid rgba(255,255,255,0.05)', paddingTop: '0.75rem' }}>
                Created {new Date(group.createdAt).toLocaleDateString()}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
