import { jsPDF } from "jspdf";
import type { Trip } from "../types";

type Input = { trips: Trip[]; companyName: string };
type SignaturePoint = { x: number; y: number };

const dateTime = (value?: Date | null) => value ? value.toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" }) : "Sem registro";
const km = (value?: number) => typeof value === "number" ? `${new Intl.NumberFormat("pt-BR").format(value)} km` : "Sem registro";
const distance = (trip: Trip) => typeof trip.odometerStartKm === "number" && typeof trip.odometerEndKm === "number"
  ? `${new Intl.NumberFormat("pt-BR").format(Math.max(0, trip.odometerEndKm - trip.odometerStartKm))} km`
  : "Sem registro";

function strokes(signature?: string): SignaturePoint[][] {
  if (!signature) return [];
  try {
    const parsed = JSON.parse(signature) as { strokes?: SignaturePoint[][] };
    return Array.isArray(parsed.strokes) ? parsed.strokes : [];
  } catch {
    return [];
  }
}

export function downloadTripHistoryPdf({ trips, companyName }: Input) {
  const pdf = new jsPDF({ unit: "mm", format: "a4" });
  const pageWidth = pdf.internal.pageSize.getWidth();
  const pageHeight = pdf.internal.pageSize.getHeight();
  const margin = 16;
  let y = 52;

  const nextPage = (needed = 20) => {
    if (y + needed <= pageHeight - 18) return;
    pdf.addPage();
    y = 18;
  };
  const text = (value: string, x: number, size = 9, color: [number, number, number] = [71, 85, 105], bold = false) => {
    pdf.setFont("helvetica", bold ? "bold" : "normal");
    pdf.setFontSize(size);
    pdf.setTextColor(...color);
    pdf.text(value, x, y);
  };
  /*
   * Endereco nao cabe em uma linha, e o jsPDF nao corta o que passa da caixa: ele desenha por
   * cima do que estiver ao lado. Entao o valor e quebrado no espaco util e a caixa cresce junto.
   */
  const LABEL_WIDTH = 78;
  const LABEL_MAX_LINES = 3;
  const LABEL_LINE_HEIGHT = 4;

  const labelLines = (value: string, width = LABEL_WIDTH): string[] => {
    pdf.setFont("helvetica", "bold");
    pdf.setFontSize(9);
    const lines = pdf.splitTextToSize(value, width - 8) as string[];
    if (lines.length <= LABEL_MAX_LINES) return lines;
    // Teto de linhas para um endereco enorme nao esticar o cartao pagina afora.
    const kept = lines.slice(0, LABEL_MAX_LINES);
    kept[LABEL_MAX_LINES - 1] = `${kept[LABEL_MAX_LINES - 1].trimEnd().slice(0, -1)}...`;
    return kept;
  };

  const labelHeight = (lines: string[]) => 14 + Math.max(0, lines.length - 1) * LABEL_LINE_HEIGHT;

  const label = (title: string, lines: string[], x: number, height: number, width = LABEL_WIDTH) => {
    pdf.setFillColor(247, 249, 252);
    pdf.setDrawColor(226, 232, 240);
    pdf.roundedRect(x, y, width, height, 2, 2, "FD");
    pdf.setFont("helvetica", "normal");
    pdf.setFontSize(7);
    pdf.setTextColor(100, 116, 139);
    pdf.text(title.toUpperCase(), x + 4, y + 5);
    pdf.setFont("helvetica", "bold");
    pdf.setFontSize(9);
    pdf.setTextColor(15, 23, 42);
    // Linha a linha, para o espacamento nao depender do lineHeightFactor global do documento.
    lines.forEach((line, index) => pdf.text(line, x + 4, y + 10.5 + index * LABEL_LINE_HEIGHT));
  };
  const signature = (title: string, value: string | undefined, x: number, width = 78) => {
    pdf.setFillColor(247, 249, 252);
    pdf.setDrawColor(203, 213, 225);
    pdf.roundedRect(x, y, width, 30, 2, 2, "FD");
    pdf.setFont("helvetica", "normal");
    pdf.setFontSize(7);
    pdf.setTextColor(100, 116, 139);
    pdf.text(title.toUpperCase(), x + 4, y + 5);
    const lines = strokes(value);
    if (lines.length) {
      pdf.setDrawColor(15, 23, 42);
      pdf.setLineWidth(0.55);
      lines.forEach((stroke) => {
        stroke.forEach((point, index) => {
          if (index === 0) return;
          const previous = stroke[index - 1];
          pdf.line(x + 4 + previous.x * (width - 8), y + 8 + previous.y * 18, x + 4 + point.x * (width - 8), y + 8 + point.y * 18);
        });
      });
    } else {
      pdf.setFontSize(8);
      pdf.setTextColor(100, 116, 139);
      pdf.text("Assinatura nao registrada", x + 4, y + 19);
    }
  };

  pdf.setFillColor(15, 47, 93);
  pdf.rect(0, 0, pageWidth, 38, "F");
  pdf.setTextColor(255, 255, 255);
  pdf.setFont("helvetica", "bold");
  pdf.setFontSize(20);
  pdf.text("Zellu Frotas", margin, 17);
  pdf.setFont("helvetica", "normal");
  pdf.setFontSize(10);
  pdf.text("Historico de viagens da frota", margin, 25);
  pdf.text(companyName, margin, 32);

  pdf.setFont("helvetica", "bold");
  pdf.setTextColor(15, 23, 42);
  pdf.setFontSize(17);
  pdf.text("Registros de viagens", margin, y);
  y += 7;
  text(`${trips.length} viagem(ns) com retirada registrada`, margin, 9);
  y += 12;

  if (!trips.length) {
    text("Nenhuma retirada por QR Code foi registrada ainda.", margin, 10);
  } else {
    trips.forEach((trip, index) => {
      const vehicleTitle = `${index + 1}. ${trip.vehicleName || "Veiculo nao informado"}`;
      const titleLines = pdf.splitTextToSize(vehicleTitle, pageWidth - margin * 2 - 14) as string[];

      /*
       * As linhas sao medidas antes de desenhar porque o retangulo do cartao e o primeiro traco
       * na pagina: sem saber a altura final, um endereco de duas linhas vazava para fora dele.
       */
      const rows: Array<[string, string, string, string | null]> = [
        ["Retirada", dateTime(trip.startedAt), "Devolucao", dateTime(trip.endedAt)],
        ["KM retirada", km(trip.odometerStartKm), "KM devolucao", km(trip.odometerEndKm)],
        ["Saida", trip.origin || "Nao informada", "Destino", trip.destination || "Nao informado"],
        ["Percurso", distance(trip), "", null],
      ];
      const measured = rows.map(([leftTitle, leftValue, rightTitle, rightValue]) => {
        const left = labelLines(leftValue);
        const right = rightValue === null ? [] : labelLines(rightValue);
        // As duas caixas da fileira compartilham a altura, senao ficam desalinhadas entre si.
        const height = Math.max(labelHeight(left), right.length ? labelHeight(right) : 0);
        return { leftTitle, left, rightTitle, right: rightValue === null ? null : right, height };
      });

      const headerHeight = 9 + titleLines.length * 6 + 1 + 7;
      const rowsHeight = measured.reduce((total, row) => total + row.height + 4, 0);
      const cardHeight = headerHeight + rowsHeight + 30 + 8;

      nextPage(cardHeight + 16);
      const cardTop = y;
      pdf.setDrawColor(203, 213, 225);
      pdf.setFillColor(252, 253, 255);
      pdf.roundedRect(margin, cardTop, pageWidth - margin * 2, cardHeight, 3, 3, "FD");
      pdf.setFillColor(37, 134, 230);
      pdf.rect(margin, cardTop, 2, cardHeight, "F");

      y += 9;
      pdf.setFont("helvetica", "bold");
      pdf.setFontSize(11);
      pdf.setTextColor(15, 23, 42);
      pdf.text(titleLines, margin + 7, y);
      y += titleLines.length * 6 + 1;
      text(trip.driverName || "Motorista nao informado", margin + 7, 9);
      y += 7;

      measured.forEach((row) => {
        label(row.leftTitle, row.left, margin + 7, row.height);
        if (row.right) label(row.rightTitle, row.right, margin + 89, row.height);
        y += row.height + 4;
      });

      signature("Assinatura retirada", trip.pickupSignature, margin + 7);
      signature("Assinatura devolucao", trip.returnSignature, margin + 89);
      // Volta pela altura declarada do cartao, em vez de somar passos: assim o proximo cartao
      // comeca no lugar certo mesmo quando uma fileira cresceu.
      y = cardTop + cardHeight + 10;
    });
  }

  const pages = pdf.getNumberOfPages();
  for (let page = 1; page <= pages; page += 1) {
    pdf.setPage(page);
    pdf.setFont("helvetica", "normal");
    pdf.setFontSize(8);
    pdf.setTextColor(100, 116, 139);
    pdf.text(`Gerado em ${new Date().toLocaleString("pt-BR")} - Pagina ${page} de ${pages}`, margin, pageHeight - 8);
  }
  pdf.save(`historico-viagens-${new Date().toISOString().slice(0, 10)}.pdf`);
}
