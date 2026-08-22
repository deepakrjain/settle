import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { UserProfileModal } from './UserProfileModal';
import { Wallet, LogOut, User as UserIcon } from 'lucide-react';

interface NavbarProps {
  onNavigate: (view: 'groups' | 'group-detail', groupId?: string) => void;
}

export const Navbar: React.FC<NavbarProps> = ({ onNavigate }) => {
  const { user, logout } = useAuth();
  const [showProfile, setShowProfile] = useState(false);

  return (
    <>
      <nav style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        padding: '1rem 2rem',
        background: 'rgba(15, 23, 42, 0.8)',
        backdropFilter: 'blur(12px)',
        borderBottom: '1px solid rgba(255, 255, 255, 0.1)',
        position: 'sticky',
        top: 0,
        zIndex: 50
      }}>
        <div 
          onClick={() => onNavigate('groups')}
          style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', cursor: 'pointer' }}
        >
          <div style={{
            background: 'linear-gradient(135deg, #6366f1, #10b981)',
            padding: '0.5rem',
            borderRadius: '12px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}>
            <Wallet size={24} color="#ffffff" />
          </div>
          <span style={{ fontSize: '1.4rem', fontWeight: 700, letterSpacing: '-0.02em', background: 'linear-gradient(to right, #ffffff, #94a3b8)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
            Settle
          </span>
        </div>

        {user && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
            {/* Clickable Profile Chip */}
            <button
              onClick={() => setShowProfile(true)}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.6rem',
                background: 'rgba(255, 255, 255, 0.06)',
                border: '1px solid rgba(255, 255, 255, 0.12)',
                padding: '0.45rem 0.85rem',
                borderRadius: '20px',
                color: '#f8fafc',
                cursor: 'pointer',
                transition: 'all 0.2s ease'
              }}
              title="Click to view My Profile & User ID"
            >
              <div style={{
                width: '24px',
                height: '24px',
                borderRadius: '50%',
                background: 'rgba(99, 102, 241, 0.3)',
                color: '#818cf8',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontSize: '0.75rem',
                fontWeight: 700
              }}>
                {user.displayName ? user.displayName.charAt(0).toUpperCase() : 'U'}
              </div>
              <span style={{ fontSize: '0.9rem', fontWeight: 500 }}>{user.displayName}</span>
            </button>

            <button 
              onClick={logout}
              className="btn-secondary"
              style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', padding: '0.5rem 0.9rem' }}
            >
              <LogOut size={16} />
              Logout
            </button>
          </div>
        )}
      </nav>

      {/* User Profile Modal */}
      {showProfile && user && (
        <UserProfileModal user={user} onClose={() => setShowProfile(false)} />
      )}
    </>
  );
};
