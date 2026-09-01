export function ErrorPanel({
  title,
  message = "Check that the backend is reachable, then try again.",
}: {
  title: string;
  message?: string;
}) {
  return (
    <div className="panel">
      <div className="panel__body">
        <p className="eyebrow">Backend unavailable</p>
        <h2 className="section-title mt-2">{title}</h2>
        <p className="mt-3 text-muted">{message}</p>
      </div>
    </div>
  );
}
