import { jsPDF } from "jspdf";
import type { CorporateAlert, Vehicle, VehicleHistoryItem } from "../types";

type Input = {
  vehicle: Vehicle;
  companyName: string;
  history: VehicleHistoryItem[];
  upcomingAlerts: CorporateAlert[];
};

const currency = (value: number) => new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(value);
const dateLabel = (value?: Date | null) => value ? value.toLocaleDateString("pt-BR") : "Sem data";
const kmLabel = (value?: number) => value ? `${new Intl.NumberFormat("pt-BR").format(value)} km` : "Nao informado";
const attachmentData = (item: VehicleHistoryItem) => item.cloudFileData || item.cloudImageData || "";
const isImageAttachment = (item: VehicleHistoryItem, data: string) => Boolean(data && (item.fileType?.startsWith("image/") || data.startsWith("data:image/")));
const imageFormat = (type?: string) => type?.includes("png") ? "PNG" : type?.includes("webp") ? "WEBP" : "JPEG";

export function downloadVehicleHistoryPdf({ vehicle, companyName, history, upcomingAlerts }: Input) {
  const pdf = new jsPDF({ unit: "mm", format: "a4" });
  const pageWidth = pdf.internal.pageSize.getWidth();
  const margin = 18;
  let y = 20;
  const totalSpent = history.reduce((total, item) => total + (item.cost || 0), 0);

  const nextPage = (needed = 20) => {
    if (y + needed <= 276) return;
    pdf.addPage();
    y = 18;
  };
  const section = (title: string) => {
    nextPage(16);
    pdf.setFillColor(234, 242, 255);
    pdf.roundedRect(margin, y, pageWidth - margin * 2, 10, 2, 2, "F");
    pdf.setTextColor(29, 78, 216);
    pdf.setFont("helvetica", "bold");
    pdf.setFontSize(11);
    pdf.text(title.toUpperCase(), margin + 4, y + 6.5);
    y += 16;
  };
  const line = (label: string, value: string) => {
    nextPage(10);
    pdf.setFont("helvetica", "normal");
    pdf.setTextColor(71, 85, 105);
    pdf.setFontSize(9);
    pdf.text(label, margin, y);
    pdf.setFont("helvetica", "bold");
    pdf.setTextColor(15, 23, 42);
    pdf.text(value, margin + 42, y);
    y += 7;
  };

  pdf.setFillColor(15, 47, 93);
  pdf.rect(0, 0, pageWidth, 42, "F");
  pdf.setTextColor(255, 255, 255);
  pdf.setFont("helvetica", "bold");
  pdf.setFontSize(20);
  pdf.text("Zellu Frotas", margin, 20);
  pdf.setFont("helvetica", "normal");
  pdf.setFontSize(10);
  pdf.text("Dossie de historico do veiculo", margin, 28);
  pdf.text(companyName, margin, 35);
  y = 54;

  pdf.setFont("helvetica", "bold");
  pdf.setTextColor(15, 23, 42);
  pdf.setFontSize(18);
  pdf.text(vehicle.name, margin, y);
  y += 8;
  pdf.setFont("helvetica", "normal");
  pdf.setTextColor(71, 85, 105);
  pdf.setFontSize(10);
  pdf.text([vehicle.plate || "Sem placa", vehicle.year || vehicle.model || "Sem ano", vehicle.fuel || ""].filter(Boolean).join(" - "), margin, y);
  y += 14;

  section("Resumo para revenda");
  line("Quilometragem", kmLabel(vehicle.odometerKm));
  line("Gasto em servicos", currency(totalSpent));
  line("Registros comprovados", String(history.length));
  line("Cuidados programados", String(upcomingAlerts.length));

  section("Proximos cuidados");
  if (upcomingAlerts.length === 0) {
    line("Status", "Nenhum aviso pendente");
  } else {
    upcomingAlerts.forEach((alert) => {
      nextPage(20);
      pdf.setFont("helvetica", "bold");
      pdf.setTextColor(15, 23, 42);
      pdf.setFontSize(11);
      pdf.text(alert.title, margin, y);
      y += 6;
      pdf.setFont("helvetica", "normal");
      pdf.setTextColor(71, 85, 105);
      pdf.setFontSize(9);
      pdf.text(`Prazo: ${dateLabel(alert.dueDate)}  |  KM: ${kmLabel(alert.dueOdometerKm)}`, margin, y);
      y += 9;
    });
  }

  section("Servicos realizados");
  if (history.length === 0) {
    line("Status", "Nenhum registro de manutencao disponivel");
  } else {
    history.forEach((item) => {
      const description = item.notes || "Servico registrado sem observacoes adicionais.";
      const lines = pdf.splitTextToSize(description, pageWidth - margin * 2);
      const attachment = attachmentData(item);
      const imageAttachment = isImageAttachment(item, attachment);
      // Reserve the full image area plus the label and bottom breathing room.
      const attachmentHeight = imageAttachment ? 72 : attachment ? 18 : 0;
      const cardHeight = 19 + lines.length * 4 + attachmentHeight;
      nextPage(22 + lines.length * 4 + attachmentHeight);
      pdf.setDrawColor(203, 213, 225);
      pdf.roundedRect(margin, y - 4, pageWidth - margin * 2, cardHeight, 2, 2, "S");
      pdf.setFont("helvetica", "bold");
      pdf.setTextColor(15, 23, 42);
      pdf.setFontSize(11);
      pdf.text(item.title, margin + 4, y + 3);
      y += 9;
      pdf.setFont("helvetica", "normal");
      pdf.setTextColor(71, 85, 105);
      pdf.setFontSize(9);
      pdf.text(`Data: ${dateLabel(item.serviceDate || item.createdAt)}  |  KM: ${kmLabel(item.odometerKm)}  |  Valor: ${currency(item.cost || 0)}`, margin + 4, y);
      y += 6;
      pdf.text(lines, margin + 4, y);
      y += lines.length * 4 + 7;

      if (imageAttachment) {
        pdf.setFont("helvetica", "bold");
        pdf.setFontSize(8);
        pdf.setTextColor(71, 85, 105);
        pdf.text("COMPROVANTE ANEXADO", margin + 4, y);
        y += 4;
        try {
          const image = pdf.getImageProperties(attachment);
          const maxWidth = pageWidth - margin * 2 - 8;
          const maxHeight = 48;
          const scale = Math.min(maxWidth / image.width, maxHeight / image.height, 1);
          const width = image.width * scale;
          const height = image.height * scale;
          pdf.addImage(attachment, imageFormat(item.fileType), margin + 4, y, width, height, undefined, "FAST");
          y += height + 5;
        } catch {
          pdf.setFont("helvetica", "normal");
          pdf.setFontSize(8);
          pdf.text("Nao foi possivel visualizar a imagem no PDF.", margin + 4, y + 4);
          y += 9;
        }
      } else if (attachment) {
        pdf.setFont("helvetica", "normal");
        pdf.setFontSize(8);
        pdf.setTextColor(71, 85, 105);
        pdf.text(`COMPROVANTE ANEXADO: ${item.fileName || "arquivo PDF"} (${item.fileType || "application/pdf"})`, margin + 4, y + 4);
        y += 11;
      } else {
        y += 2;
      }
      y += 5;
    });
  }

  const pages = pdf.getNumberOfPages();
  for (let index = 1; index <= pages; index += 1) {
    pdf.setPage(index);
    pdf.setFont("helvetica", "normal");
    pdf.setTextColor(100, 116, 139);
    pdf.setFontSize(8);
    pdf.text(`Gerado em ${new Date().toLocaleDateString("pt-BR")} - Pagina ${index} de ${pages}`, margin, 289);
  }
  pdf.save(`historico-${(vehicle.plate || vehicle.name).replace(/[^a-z0-9]+/gi, "-").toLowerCase()}.pdf`);
}
