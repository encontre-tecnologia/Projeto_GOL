import { useState } from "react";
import { createPortal } from "react-dom";

type ViewerState = { url: string; name: string; type?: string } | null;

/**
 * Visualizador de anexo dentro da dashboard, no lugar de aba nova.
 *
 * Alem de nao arrancar a pessoa do contexto, aba nova com data: URL e bloqueada pelos
 * navegadores modernos — o caminho antigo dependia de reembrulhar o anexo em blob para
 * driblar isso. Aqui o data URL entra direto no <img>/<iframe>.
 *
 * O overlay sai por portal para o document.body de proposito: renderizado inline, um
 * ancestral com transform (o hover dos cards de aviso tem translateY) vira o containing
 * block do position fixed, e o backdrop fica preso e cortado dentro do proprio card.
 *
 * Uso: `const { openAttachment, attachmentViewer } = useAttachmentViewer();` — chame
 * `openAttachment(url, nome, tipo?)` no clique e renderize `{attachmentViewer}` no fim
 * do componente.
 */
export function useAttachmentViewer() {
  const [state, setState] = useState<ViewerState>(null);

  function openAttachment(data: string | undefined, name = "comprovante", type?: string) {
    if (!data) return;
    setState({ url: data, name, type });
  }

  // Imagem vira <img> para ganhar o ajuste de tamanho; todo o resto (PDF, URL http)
  // cai no <iframe>, que renderiza ambos. Blob URL nao carrega o mime no endereco,
  // entao quem tem o tipo em maos passa por parametro.
  const isImage = state
    ? (state.type?.startsWith("image/") ?? false) || state.url.startsWith("data:image/")
    : false;

  const attachmentViewer = state
    ? createPortal(
        <div className="dialog-backdrop" onClick={() => setState(null)}>
          <div
            className="doc-viewer-card"
            role="dialog"
            aria-modal="true"
            aria-label={state.name}
            onClick={(event) => event.stopPropagation()}
          >
            <header className="doc-viewer-head">
              <strong>{state.name}</strong>
              <button className="dialog-close" type="button" onClick={() => setState(null)} aria-label="Fechar">
                ×
              </button>
            </header>
            {isImage ? (
              <img src={state.url} alt={state.name} className="doc-viewer-image" />
            ) : (
              <iframe src={state.url} title={state.name} className="doc-viewer-frame" />
            )}
          </div>
        </div>,
        document.body,
      )
    : null;

  return { openAttachment, attachmentViewer };
}
