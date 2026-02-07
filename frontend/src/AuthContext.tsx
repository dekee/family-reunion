import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react';

interface AuthUser {
  email: string;
  name: string;
  isAdmin: boolean;
}

interface AuthContextType {
  user: AuthUser | null;
  token: string | null;
  isAdmin: boolean;
  loading: boolean;
  login: (credential: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType>({
  user: null,
  token: null,
  isAdmin: false,
  loading: true,
  login: async () => {},
  logout: () => {},
});

export function useAuth() {
  return useContext(AuthContext);
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const clearAuth = useCallback(() => {
    setUser(null);
    setToken(null);
    localStorage.removeItem('auth_token');
  }, []);

  const verifyToken = useCallback(async (storedToken: string) => {
    try {
      const res = await fetch('/api/auth/me', {
        headers: { Authorization: `Bearer ${storedToken}` },
      });
      if (!res.ok) {
        clearAuth();
        return;
      }
      const data = await res.json();
      setUser(data);
      setToken(storedToken);
    } catch {
      clearAuth();
    }
  }, [clearAuth]);

  useEffect(() => {
    const storedToken = localStorage.getItem('auth_token');
    if (storedToken) {
      verifyToken(storedToken).finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, [verifyToken]);

  const login = async (credential: string) => {
    localStorage.setItem('auth_token', credential);
    setToken(credential);
    try {
      const res = await fetch('/api/auth/me', {
        headers: { Authorization: `Bearer ${credential}` },
      });
      if (!res.ok) {
        clearAuth();
        throw new Error('Authentication failed');
      }
      const data = await res.json();
      setUser(data);
    } catch (err) {
      clearAuth();
      throw err;
    }
  };

  const logout = () => {
    clearAuth();
  };

  return (
    <AuthContext.Provider value={{ user, token, isAdmin: user?.isAdmin ?? false, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
