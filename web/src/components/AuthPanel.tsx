import { useState } from "react";
import {
  browserLocalPersistence,
  createUserWithEmailAndPassword,
  setPersistence,
  signInWithEmailAndPassword,
  signInWithPopup,
} from "firebase/auth";
import { getFirebaseAuth, googleProvider, isFirebaseConfigured } from "../firebase";

type AuthPanelProps = {
  accessError?: string;
};

export function AuthPanel({ accessError = "" }: AuthPanelProps) {
  const [mode, setMode] = useState<"entrar" | "criar">("entrar");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit() {
    setBusy(true);
    setError("");
    try {
      const auth = getFirebaseAuth();
      await setPersistence(auth, browserLocalPersistence);
      if (mode === "entrar") {
        await signInWithEmailAndPassword(auth, email, password);
      } else {
        await createUserWithEmailAndPassword(auth, email, password);
      }
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Falha ao autenticar.");
    } finally {
      setBusy(false);
    }
  }

  async function googleSignIn() {
    setBusy(true);
    setError("");
    try {
      const auth = getFirebaseAuth();
      await setPersistence(auth, browserLocalPersistence);
      await signInWithPopup(auth, googleProvider);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Falha ao entrar com Google.");
    } finally {
      setBusy(false);
    }
  }

  if (!isFirebaseConfigured) {
    return (
      <section className="auth-card">
        <p className="eyebrow">Configuracao necessaria</p>
        <h1>Zellu Frotas</h1>
        <p>
          O dashboard ja esta pronto para Firebase Authentication. Preencha um arquivo
          <code>.env</code> em <code>web</code> usando o modelo <code>env.example</code>.
        </p>
      </section>
    );
  }

  return (
    <section className="auth-card">
      <p className="eyebrow">Dashboard corporativo</p>
      <h1>Zellu Frotas</h1>
      <p>Entre com a mesma identidade Firebase usada no ecossistema Zellu. Para criar frota propria, a conta precisa ter plano Frota ou Enterprise; convidados entram pela empresa que liberou o acesso.</p>
      <div className="segmented">
        <button className={mode === "entrar" ? "active" : ""} onClick={() => setMode("entrar")}>
          Entrar
        </button>
        <button className={mode === "criar" ? "active" : ""} onClick={() => setMode("criar")}>
          Criar conta
        </button>
      </div>
      <label>
        E-mail
        <input autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} />
      </label>
      <label>
        Senha
        <input
          autoComplete={mode === "entrar" ? "current-password" : "new-password"}
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />
      </label>
      {(error || accessError) && <p className="error">{error || accessError}</p>}
      <button className="primary" disabled={busy || !email || password.length < 6} onClick={submit}>
        {busy ? "Aguarde..." : mode === "entrar" ? "Entrar" : "Criar acesso"}
      </button>
      <button className="secondary" disabled={busy} onClick={googleSignIn}>
        Entrar com Google
      </button>
    </section>
  );
}
