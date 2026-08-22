import React, { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Navbar } from './components/Navbar';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { GroupList } from './pages/GroupList';
import { GroupDetail } from './pages/GroupDetail';

const AppContent: React.FC = () => {
  const { user } = useAuth();
  const [authView, setAuthView] = useState<'login' | 'register'>('login');
  const [currentView, setCurrentView] = useState<'groups' | 'group-detail'>('groups');
  const [selectedGroupId, setSelectedGroupId] = useState<string | null>(null);

  const handleNavigate = (view: 'groups' | 'group-detail', groupId?: string) => {
    setCurrentView(view);
    if (groupId) {
      setSelectedGroupId(groupId);
    }
  };

  if (!user) {
    return (
      <div>
        <Navbar onNavigate={handleNavigate} />
        {authView === 'login' ? (
          <Login onSwitchToRegister={() => setAuthView('register')} />
        ) : (
          <Register onSwitchToLogin={() => setAuthView('login')} />
        )}
      </div>
    );
  }

  return (
    <div>
      <Navbar onNavigate={handleNavigate} />
      {currentView === 'groups' || !selectedGroupId ? (
        <GroupList onSelectGroup={(id) => handleNavigate('group-detail', id)} />
      ) : (
        <GroupDetail groupId={selectedGroupId} onBack={() => setCurrentView('groups')} />
      )}
    </div>
  );
};

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
};

export default App;
