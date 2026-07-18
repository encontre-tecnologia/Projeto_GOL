export function MetricCard({ label, value, tone }: { label: string; value: string | number; tone?: string }) {
  return (
    <article className={`metric ${tone || ""}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}
