import { createContext, useContext } from 'react';
import type { Session } from './api';

export interface SessionContextValue {
  session: Session | null;
  signIn: (session: Session) => void;
  signOut: () => void;
}

export const SessionContext = createContext<SessionContextValue>({
  session: null,
  signIn: () => {},
  signOut: () => {},
});

export function useSession(): SessionContextValue {
  return useContext(SessionContext);
}
