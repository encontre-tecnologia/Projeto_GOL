import { useEffect, useState } from "react";
import { onAuthStateChanged, type User } from "firebase/auth";
import { getFirebaseAuth, isFirebaseConfigured } from "./firebase";
import { AuthPanel } from "./components/AuthPanel";
import { Dashboard } from "./components/Dashboard";

export default function App() {
  const [user, setUser] = useState<User | null>(null);
  const [initializing, setInitializing] = useState(true);

  useEffect(() => {
    if (!isFirebaseConfigured) {
      setInitializing(false);
      return;
    }
    return onAuthStateChanged(getFirebaseAuth(), (currentUser) => {
      setUser(currentUser);
      setInitializing(false);
    });
  }, []);

  if (initializing) return <main className="center-page">Carregando...</main>;
  if (!user) return <main className="center-page"><AuthPanel /></main>;
  return <Dashboard user={user} />;
}
