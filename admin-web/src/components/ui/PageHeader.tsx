import { RefreshButton } from "@/components/ui/RefreshButton";

type PageHeaderProps = {
  eyebrow: string;
  title: string;
  children?: React.ReactNode;
  showRefresh?: boolean;
};

export function PageHeader({
  eyebrow,
  title,
  children,
  showRefresh = true,
}: PageHeaderProps) {
  return (
    <div className="page-header">
      <div>
        <p className="eyebrow">{eyebrow}</p>
        <h1 className="page-title">{title}</h1>
        {children ? <div className="mt-3 max-w-2xl text-muted">{children}</div> : null}
      </div>
      {showRefresh ? <RefreshButton /> : null}
    </div>
  );
}
