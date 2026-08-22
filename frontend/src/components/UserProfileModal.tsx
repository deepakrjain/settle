import React, { useState } from 'react';
import { User } from '../types';
import { X, User as UserIcon, Mail, Copy, Check, Shield } from 'lucide-react';

interface UserProfileModalProps {
  user: User;
  onClose: () => void;
}

export const UserProfileModal: React.FC<UserProfileModalProps> = ({ user, onClose }) => {
  const [copied, setCopied] = useState(false);

  const handleCopyId = () => {
    navigator.clipboard.writeText(user.id);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      background: 'rgba(15, 23, 42, 0.75)',
      backdropFilter: 'blur(8px)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 100,
      padding: '1rem'
    }}>
      <div 
        className="glass-panel"
        style={{
          maxWidth: '440px',
          width: '100%',
          padding: '2rem',
          position: 'relative',
          borderRadius: '16px',
          border: '1px solid rgba(255, 255, 255, 0.12)',
          boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.5)'
        }}
      >
        {/* Close Button */}
        <button 
          onClick={onClose}
          style={{
            position: 'absolute',
            top: '1.25rem',
            right: '1.25rem',
            background: 'rgba(255, 255, 255, 0.05)',
            border: 'none',
            color: '#94a3b8',
            padding: '0.4rem',
            borderRadius: '8px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center'
          }}
        >
          <X size={18} />
        </button>

        {/* Header Avatar */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', marginBottom: '1.5rem' }}>
          <div style={{
            width: '64px',
            height: '64px',
            borderRadius: '50%',
            background: 'linear-gradient(135deg, #6366f1, #10b981)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#ffffff',
            fontWeight: 700,
            fontSize: '1.5rem',
            marginBottom: '0.75rem',
            boxShadow: '0 10px 20px -5px rgba(99, 102, 241, 0.4)'
          }}>
            {user.displayName ? user.displayName.charAt(0).toUpperCase() : 'U'}
          </div>
          <h2 style={{ fontSize: '1.35rem', fontWeight: 700, color: '#f8fafc', margin: 0 }}>
            {user.displayName}
          </h2>
          <span style={{ fontSize: '0.8rem', color: '#94a3b8', marginTop: '0.2rem' }}>
            Account Profile
          </span>
        </div>

        {/* User Details Form-like Card */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          {/* Display Name */}
          <div>
            <label style={{ fontSize: '0.75rem', fontWeight: 600, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em', display: 'flex', alignItems: 'center', gap: '0.4rem', marginBottom: '0.4rem' }}>
              <UserIcon size={14} color="#6366f1" /> Full Name
            </label>
            <div style={{ background: 'rgba(15, 23, 42, 0.6)', padding: '0.75rem 1rem', borderRadius: '10px', border: '1px solid rgba(255,255,255,0.06)', color: '#f8fafc', fontSize: '0.95rem' }}>
              {user.displayName}
            </div>
          </div>

          {/* Email Address */}
          <div>
            <label style={{ fontSize: '0.75rem', fontWeight: 600, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em', display: 'flex', alignItems: 'center', gap: '0.4rem', marginBottom: '0.4rem' }}>
              <Mail size={14} color="#6366f1" /> Email Address
            </label>
            <div style={{ background: 'rgba(15, 23, 42, 0.6)', padding: '0.75rem 1rem', borderRadius: '10px', border: '1px solid rgba(255,255,255,0.06)', color: '#f8fafc', fontSize: '0.95rem' }}>
              {user.email}
            </div>
          </div>

          {/* User ID (with copy action) */}
          <div>
            <label style={{ fontSize: '0.75rem', fontWeight: 600, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em', display: 'flex', alignItems: 'center', gap: '0.4rem', marginBottom: '0.4rem' }}>
              <Shield size={14} color="#10b981" /> User ID
            </label>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              background: 'rgba(15, 23, 42, 0.6)',
              padding: '0.65rem 0.85rem',
              borderRadius: '10px',
              border: '1px solid rgba(16, 185, 129, 0.3)'
            }}>
              <span style={{ color: '#34d399', fontFamily: 'monospace', fontSize: '0.85rem', fontWeight: 600, wordBreak: 'break-all' }}>
                {user.id}
              </span>
              <button
                onClick={handleCopyId}
                className="btn-primary"
                style={{
                  padding: '0.4rem 0.75rem',
                  fontSize: '0.8rem',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.35rem',
                  marginLeft: '0.5rem',
                  whiteSpace: 'nowrap',
                  background: copied ? '#10b981' : undefined
                }}
              >
                {copied ? <Check size={14} /> : <Copy size={14} />}
                {copied ? 'Copied!' : 'Copy'}
              </button>
            </div>
            <p style={{ color: '#64748b', fontSize: '0.75rem', marginTop: '0.4rem', margin: '0.4rem 0 0 0' }}>
              Share your User ID with group creators to be added to group rosters.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
