import { formatStatus, statusTone } from "@/lib/status";

export function StatusBadge({ value }: { value: string }) {
  return (
    <span className={`status-badge status-badge--${statusTone(value)}`}>
      {formatStatus(value)}
    </span>
  );
}
