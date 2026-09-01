export function LoadingPanel({ label = "Loading..." }: { label?: string }) {
  return (
    <div className="panel">
      <div className="panel__body">
        <p className="eyebrow">Basilico Admin</p>
        <p className="mt-2 text-muted">{label}</p>
      </div>
    </div>
  );
}
