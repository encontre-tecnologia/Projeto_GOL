export function Row({ title, meta, badge }: { title: string; meta: string; badge: string }) {
  return (
    <article className="row">
      <div>
        <strong>{title}</strong>
        <span>{meta}</span>
      </div>
      <em>{badge}</em>
    </article>
  );
}
