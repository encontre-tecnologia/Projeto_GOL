const maxDataUrlLength = 780_000;
const compressionSteps = [
  { maxDimension: 1600, quality: 0.82 },
  { maxDimension: 1280, quality: 0.76 },
  { maxDimension: 1024, quality: 0.7 },
  { maxDimension: 800, quality: 0.65 },
];

export async function compressImageForFirestore(file: File): Promise<{ dataUrl: string; type: string; size: number }> {
  if (!file.type.startsWith("image/")) {
    throw new Error("Sem armazenamento externo, envie uma imagem em JPG, PNG ou WebP.");
  }

  const sourceUrl = URL.createObjectURL(file);
  try {
    const image = await loadImage(sourceUrl);
    for (const step of compressionSteps) {
      const scale = Math.min(1, step.maxDimension / Math.max(image.naturalWidth, image.naturalHeight));
      const canvas = document.createElement("canvas");
      canvas.width = Math.max(1, Math.round(image.naturalWidth * scale));
      canvas.height = Math.max(1, Math.round(image.naturalHeight * scale));
      const context = canvas.getContext("2d");
      if (!context) throw new Error("Nao foi possivel preparar a imagem.");
      context.drawImage(image, 0, 0, canvas.width, canvas.height);
      const blob = await canvasToBlob(canvas, "image/jpeg", step.quality);
      const dataUrl = await blobToDataUrl(blob);
      if (dataUrl.length <= maxDataUrlLength) return { dataUrl, type: "image/jpeg", size: blob.size };
    }
    throw new Error("A imagem e muito grande. Escolha uma foto menor.");
  } finally {
    URL.revokeObjectURL(sourceUrl);
  }
}

export async function prepareRecordAttachmentForFirestore(file: File): Promise<{ dataUrl: string; type: string; size: number }> {
  if (file.type.startsWith("image/")) return compressImageForFirestore(file);
  if (file.type !== "application/pdf") throw new Error("Envie uma foto ou um arquivo PDF.");
  if (file.size > 550_000) throw new Error("O PDF esta grande demais para sincronizar. Envie um arquivo de ate 550 KB.");
  const dataUrl = await blobToDataUrl(file);
  if (dataUrl.length > maxDataUrlLength) throw new Error("O PDF esta grande demais para sincronizar.");
  return { dataUrl, type: file.type, size: file.size };
}

function loadImage(url: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const image = new Image();
    image.onload = () => resolve(image);
    image.onerror = () => reject(new Error("Nao foi possivel ler a imagem."));
    image.src = url;
  });
}

function canvasToBlob(canvas: HTMLCanvasElement, type: string, quality: number): Promise<Blob> {
  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => blob ? resolve(blob) : reject(new Error("Nao foi possivel compactar a imagem.")), type, quality);
  });
}

function blobToDataUrl(blob: Blob): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => typeof reader.result === "string" ? resolve(reader.result) : reject(new Error("Nao foi possivel preparar a imagem."));
    reader.onerror = () => reject(reader.error || new Error("Nao foi possivel preparar a imagem."));
    reader.readAsDataURL(blob);
  });
}
