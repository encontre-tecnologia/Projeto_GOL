import { useEffect, useState } from "react";
import { onAuthStateChanged, signOut, type User } from "firebase/auth";
import { getFirebaseAuth, isFirebaseConfigured } from "./firebase";
import { AuthPanel } from "./components/AuthPanel";
import { Dashboard } from "./components/Dashboard";
import { SponsorsAdminPanel } from "./components/SponsorsAdminPanel";
import { getFleetDashboardAccess, type FleetDashboardAccess } from "./lib/company";

// Rota separada da dashboard de frota: patrocinadores e um recurso global da
// plataforma (nao pertence a nenhuma empresa), gerido por quem esta em
// admin_settings/access — nunca pela checagem de plano Frota/Enterprise abaixo.
const isSponsorsAdminRoute = window.location.pathname === "/admin/patrocinadores";

export default function App() {
  const [user, setUser] = useState<User | null>(null);
  const [initializing, setInitializing] = useState(true);
  const [checkingAccess, setCheckingAccess] = useState(false);
  const [accessError, setAccessError] = useState("");
  const [accessInfo, setAccessInfo] = useState<FleetDashboardAccess | null>(null);

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

  useEffect(() => {
    if (!user || isSponsorsAdminRoute) return;
    let cancelled = false;
    setCheckingAccess(true);
    setAccessError("");
    setAccessInfo(null);

    getFleetDashboardAccess(user)
      .then(async (access) => {
        if (cancelled) return;
        setAccessInfo(access);
        if (access.allowed) return;
        setAccessError("Nao encontramos um plano Frota ou Enterprise ativo nesta conta, nem um convite de empresa vinculado a este e-mail.");
      })
      .catch(async () => {
        if (cancelled) return;
        setAccessInfo({
          allowed: false,
          planLabel: "Nao foi possivel validar",
          accessLevel: "Validacao indisponivel",
        });
        setAccessError("Nao conseguimos confirmar um plano Frota ou Enterprise ativo nem um convite de empresa para este e-mail.");
      })
      .finally(() => {
        if (!cancelled) setCheckingAccess(false);
      });

    return () => { cancelled = true; };
  }, [user]);

  if (initializing) return <main className="center-page">Carregando...</main>;
  if (!user) return <main className="center-page"><AuthPanel accessError={accessError} /></main>;
  if (isSponsorsAdminRoute) return <SponsorsAdminPanel user={user} />;
  if (checkingAccess) return <main className="center-page">Validando acesso...</main>;
  if (accessError) return <main className="center-page"><FleetAccessDeniedScreen user={user} message={accessError} accessInfo={accessInfo} /></main>;
  return <Dashboard user={user} />;
}

function FleetAccessDeniedScreen({ user, message, accessInfo }: { user: User; message: string; accessInfo: FleetDashboardAccess | null }) {
  async function leaveAccount() {
    await signOut(getFirebaseAuth());
  }

  return (
    <section className="auth-card access-denied-card">
      <div className="access-denied-icon">!</div>
      <p className="eyebrow">Dashboard bloqueado</p>
      <h1>Esta conta ainda nao tem acesso a uma frota</h1>
      <p>{message}</p>
      <div className="access-denied-summary">
        <div>
          <span>Conta logada</span>
          <strong>{user.email || "E-mail nao informado"}</strong>
        </div>
        <div>
          <span>Plano detectado</span>
          <strong>{accessInfo?.planLabel || "Verificando"}</strong>
        </div>
        <div>
          <span>Nivel de acesso</span>
          <strong>{accessInfo?.accessLevel || "Sem acesso liberado"}</strong>
        </div>
      </div>
      <div className="access-denied-box">
        <strong>Como liberar o acesso</strong>
        <span>Assine o plano Frota ou Enterprise no app Zellu para criar e administrar sua propria frota.</span>
        <span>Ou peca para o gestor da empresa enviar um convite para o mesmo e-mail usado neste login.</span>
      </div>
      <button className="secondary" onClick={leaveAccount}>Usar outro e-mail</button>
    </section>
  );
}
