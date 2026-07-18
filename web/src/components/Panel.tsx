export function Panel({ title, empty, children }: { title: string; empty: string; children: React.ReactNode }) {
  const hasChildren = Array.isArray(children) ? children.length > 0 : Boolean(children);
  return (
    <section className="panel">
      <h2>{title}</h2>
      <div className="panel-list">{hasChildren ? children : <p className="empty">{empty}</p>}</div>
    </section>
  );
}
