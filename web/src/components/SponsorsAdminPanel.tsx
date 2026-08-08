import { useEffect, useState } from "react";
import {
  collection,
  deleteDoc,
  doc,
  onSnapshot,
  orderBy,
  query,
  serverTimestamp,
  setDoc,
} from "firebase/firestore";
import type { FirestoreError } from "firebase/firestore";
import type { User } from "firebase/auth";
import { getFirebaseDb } from "../firebase";

/**
 * Precisa espelhar exatamente os labels de TipoManutencao em CarLembreteModels.kt —
 * e por esse texto que o app casa um patrocinado com a categoria que o usuario esta
 * vendo (PrestadoresPatrocinadosSync.buscar usa tipoSelecionado.label).
 */
const CATEGORIAS = [
  "Corrente", "Lubrificação", "Pedivela", "Acessórios", "Conforto", "Pneu",
  "Transmissão", "Revisão", "Óleo", "Lavagem", "Posto", "Elétrica", "Vidros",
  "Mecânica", "Funilaria", "Freio", "Licença", "IPVA", "Seguro", "Outros",
];

type Sponsor = {
  id: string;
  nome: string;
  telefone: string;
  tipoServico: string;
  cidade: string;
  estado: string;
  ativo: boolean;
  posicao: number;
  expiraEm: number | null;
  cliques: number;
};

const PRIORIDADES = ["1", "2", "3", "4", "5"];

const emptyForm = {
  nome: "",
  telefone: "",
  tipoServico: CATEGORIAS[0],
  cidade: "",
  estado: "",
  posicao: "1",
  expiraEm: "",
  ativo: true,
};

export function SponsorsAdminPanel({ user }: { user: User }) {
  const [sponsors, setSponsors] = useState<Sponsor[]>([]);
  const [accessDenied, setAccessDenied] = useState(false);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => {
    const db = getFirebaseDb();
    const unsubscribe = onSnapshot(
      query(collection(db, "prestadores_patrocinados"), orderBy("nome")),
      (snap) => {
        setLoading(false);
        setSponsors(
          snap.docs.map((item) => {
            const data = item.data();
            return {
              id: item.id,
              nome: String(data.nome || ""),
              telefone: String(data.telefone || ""),
              tipoServico: String(data.tipoServico || ""),
              cidade: String(data.cidade || ""),
              estado: String(data.estado || ""),
              ativo: Boolean(data.ativo),
              // `prioridade` e o nome usado pelos primeiros cadastros. Mantemos a
              // leitura como fallback para que eles possam ser editados sem sumir.
              posicao: Number(data.posicao ?? data.prioridade ?? 1),
              expiraEm: typeof data.expiraEm === "number" ? data.expiraEm : null,
              cliques: Number(data.cliques || 0),
            };
          }),
        );
      },
      (error: FirestoreError) => {
        setLoading(false);
        if (error.code === "permission-denied") setAccessDenied(true);
      },
    );
    return unsubscribe;
  }, []);

  function startEdit(sponsor: Sponsor) {
    setEditingId(sponsor.id);
    setForm({
      nome: sponsor.nome,
      telefone: sponsor.telefone,
      tipoServico: sponsor.tipoServico || CATEGORIAS[0],
      cidade: sponsor.cidade,
      estado: sponsor.estado,
      posicao: String(sponsor.posicao),
      expiraEm: sponsor.expiraEm ? new Date(sponsor.expiraEm).toISOString().slice(0, 10) : "",
      ativo: sponsor.ativo,
    });
    setMessage("");
  }

  function cancelEdit() {
    setEditingId(null);
    setForm(emptyForm);
    setMessage("");
  }

  async function saveSponsor() {
    const nome = form.nome.trim();
    const telefone = form.telefone.trim();
    if (!nome || !telefone) {
      setMessage("Preencha nome e telefone.");
      return;
    }

    setBusy(true);
    setMessage("");
    try {
      const db = getFirebaseDb();
      const id = editingId || doc(collection(db, "prestadores_patrocinados")).id;
      const expiraEm = form.expiraEm
        ? new Date(`${form.expiraEm}T23:59:59`).getTime()
        : null;
      const payload: Record<string, unknown> = {
        nome,
        telefone,
        tipoServico: form.tipoServico,
        cidade: form.cidade.trim(),
        estado: form.estado.trim().toUpperCase(),
        posicao: Math.min(5, Math.max(1, Number(form.posicao) || 1)),
        expiraEm,
        ativo: form.ativo,
        criadoPorUid: user.uid,
        criadoPorEmail: user.email || "",
        atualizadoEm: serverTimestamp(),
      };
      // cliques so e inicializado na criacao — editar nao pode zerar o contador existente.
      if (!editingId) payload.cliques = 0;
      await setDoc(doc(db, "prestadores_patrocinados", id), payload, { merge: true });
      setMessage(editingId ? "Patrocinador atualizado." : "Patrocinador cadastrado.");
      cancelEdit();
    } catch (reason) {
      setMessage(reason instanceof Error ? reason.message : "Nao foi possivel salvar.");
    } finally {
      setBusy(false);
    }
  }

  async function removeSponsor(sponsor: Sponsor) {
    const confirmed = window.confirm(`Remover o patrocinio de ${sponsor.nome}?`);
    if (!confirmed) return;
    try {
      await deleteDoc(doc(getFirebaseDb(), "prestadores_patrocinados", sponsor.id));
    } catch (reason) {
      setMessage(reason instanceof Error ? reason.message : "Nao foi possivel remover.");
    }
  }

  if (accessDenied) {
    return (
      <section className="organization-page">
        <div className="organization-hero">
          <div>
            <p className="eyebrow">Patrocinadores</p>
            <h2>Sem acesso a esta area</h2>
            <span>
              A conta {user.email} nao esta liberada como administracao da plataforma. Peca
              para um administrador ja validado adicionar seu e-mail em admin_settings/access.
            </span>
          </div>
        </div>
      </section>
    );
  }

  return (
    <section className="organization-page">
      <div className="organization-hero">
        <div>
          <p className="eyebrow">Painel Zellu Admin</p>
          <h2>Prestadores patrocinados</h2>
          <span>Cadastre anuncios pagos que aparecem destacados na tela de selecao de prestador do app.</span>
        </div>
        <div className="organization-summary">
          <article>
            <span>Cadastrados</span>
            <strong>{sponsors.length}</strong>
          </article>
          <article>
            <span>Ativos</span>
            <strong>{sponsors.filter((sponsor) => sponsor.ativo).length}</strong>
          </article>
        </div>
      </div>

      <div className="organization-layout">
        <article className="organization-card organization-invite-card">
          <div className="organization-card-head">
            <div>
              <p className="eyebrow">{editingId ? "Editar" : "Novo"}</p>
              <h3>{editingId ? "Editar patrocinador" : "Adicionar patrocinador"}</h3>
            </div>
            {editingId && <span className="organization-muted">Editando {form.nome}</span>}
          </div>

          <div className="form-grid">
            <label>
              Nome do prestador
              <input value={form.nome} onChange={(event) => setForm({ ...form, nome: event.target.value })} placeholder="Ex.: Auto Center Silva" />
            </label>
            <label>
              Telefone (WhatsApp)
              <input value={form.telefone} onChange={(event) => setForm({ ...form, telefone: event.target.value })} placeholder="(16) 99999-9999" />
            </label>
            <label>
              Categoria
              <select value={form.tipoServico} onChange={(event) => setForm({ ...form, tipoServico: event.target.value })}>
                {CATEGORIAS.map((categoria) => (
                  <option key={categoria} value={categoria}>{categoria}</option>
                ))}
              </select>
            </label>
            <label>
              Posição na lista do app
              <select value={form.posicao} onChange={(event) => setForm({ ...form, posicao: event.target.value })}>
                {PRIORIDADES.map((valor) => (
                  <option key={valor} value={valor}>{valor}º</option>
                ))}
              </select>
            </label>
            <label>
              Cidade (vazio = qualquer cidade)
              <input value={form.cidade} onChange={(event) => setForm({ ...form, cidade: event.target.value })} placeholder="Ex.: São Carlos" />
            </label>
            <label>
              UF (vazio = qualquer estado)
              <input value={form.estado} onChange={(event) => setForm({ ...form, estado: event.target.value.toUpperCase() })} placeholder="SP" maxLength={2} />
            </label>
            <label>
              Expira em (vazio = sem validade)
              <input type="date" value={form.expiraEm} onChange={(event) => setForm({ ...form, expiraEm: event.target.value })} />
            </label>
            <label>
              Ativo
              <select value={form.ativo ? "sim" : "nao"} onChange={(event) => setForm({ ...form, ativo: event.target.value === "sim" })}>
                <option value="sim">Sim</option>
                <option value="nao">Nao</option>
              </select>
            </label>
          </div>

          <div className="invite-form" style={{ marginTop: 12 }}>
            <button className="primary" disabled={busy} onClick={saveSponsor}>
              {busy ? "Salvando..." : editingId ? "Salvar alteracoes" : "Cadastrar patrocinador"}
            </button>
            {editingId && (
              <button className="secondary" disabled={busy} onClick={cancelEdit}>
                Cancelar edicao
              </button>
            )}
          </div>

          {message && <p className="org-message">{message}</p>}
        </article>

        <article className="organization-card">
          <div className="organization-card-head">
            <div>
              <p className="eyebrow">Cadastrados</p>
              <h3>Patrocinadores</h3>
            </div>
            <span className="organization-muted">{sponsors.length} registro(s)</span>
          </div>

          {loading ? (
            <p className="organization-muted">Carregando...</p>
          ) : sponsors.length > 0 ? (
            <div className="invite-table">
              {sponsors.map((sponsor) => (
                <div key={sponsor.id}>
                  <span className="invite-avatar">{sponsor.nome.charAt(0).toUpperCase()}</span>
                  <div>
                    <strong>{sponsor.nome}</strong>
                    <span>
                      {[sponsor.tipoServico, sponsor.cidade, sponsor.estado].filter(Boolean).join(" • ") || "Sem categoria/cidade"}
                      {` • ${sponsor.posicao}º na lista`}
                    </span>
                  </div>
                  <em>{sponsor.ativo ? "Ativo" : "Inativo"}</em>
                  <div style={{ display: "flex", gap: 8 }}>
                    <button className="secondary" onClick={() => startEdit(sponsor)}>Editar</button>
                    <button className="invite-remove-btn" onClick={() => removeSponsor(sponsor)}>Remover</button>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="organization-empty">
              <strong>Nenhum patrocinador cadastrado ainda.</strong>
              <span>Use o formulario ao lado para cadastrar o primeiro anuncio.</span>
            </div>
          )}
        </article>
      </div>
    </section>
  );
}
