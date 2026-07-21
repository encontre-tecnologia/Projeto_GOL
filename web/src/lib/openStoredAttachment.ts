export async function openStoredAttachment(data: string | undefined, fileName = "comprovante") {
  if (!data) return;

  if (!data.startsWith("data:")) {
    const viewer = window.open(data, "_blank", "noopener,noreferrer");
    if (!viewer) window.location.href = data;
    return;
  }

  if (data.startsWith("data:image/")) {
    const html = buildImageViewerHtml(data, fileName);
    const url = URL.createObjectURL(new Blob([html], { type: "text/html" }));
    openUrlInNewTab(url);
    window.setTimeout(() => URL.revokeObjectURL(url), 300_000);
    return;
  }

  const response = await fetch(data);
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  openUrlInNewTab(url);
  window.setTimeout(() => URL.revokeObjectURL(url), 300_000);
}

function openUrlInNewTab(url: string) {
  const viewer = window.open(url, "_blank", "noopener,noreferrer");
  if (viewer) return;

  const link = document.createElement("a");
  link.href = url;
  link.target = "_blank";
  link.rel = "noopener noreferrer";
  document.body.appendChild(link);
  link.click();
  link.remove();
}

function buildImageViewerHtml(src: string, fileName: string) {
  return `<!doctype html>
<html>
  <head>
    <title>${escapeHtml(fileName)}</title>
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <style>
      body { align-items: center; background: #111827; display: flex; margin: 0; min-height: 100vh; padding: 24px; }
      img { background: white; border-radius: 8px; box-shadow: 0 18px 50px rgba(0,0,0,.35); display: block; margin: auto; max-height: calc(100vh - 48px); max-width: calc(100vw - 48px); object-fit: contain; }
    </style>
  </head>
  <body>
    <img alt="${escapeHtml(fileName)}" src="${src}" />
  </body>
</html>`;
}

function escapeHtml(value: string) {
  return value.replace(/[&<>"']/g, (char) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    "\"": "&quot;",
    "'": "&#039;",
  }[char] || char));
}
