import { bookingStatuses, formatStatus } from "@/lib/status";
import type { BookingStatus } from "@/types/admin";

export function BookingFilters({
  date,
  status,
}: {
  date?: string;
  status?: BookingStatus;
}) {
  return (
    <form action="/bookings" className="panel">
      <div className="panel__body form-grid form-grid--two lg:grid-cols-3">
        <div className="field">
          <label htmlFor="date">Date</label>
          <input defaultValue={date ?? ""} id="date" name="date" type="date" />
        </div>
        <div className="field">
          <label htmlFor="status">Status</label>
          <select defaultValue={status ?? ""} id="status" name="status">
            <option value="">All</option>
            {bookingStatuses.map((value) => (
              <option key={value} value={value}>
                {formatStatus(value)}
              </option>
            ))}
          </select>
        </div>
        <div className="flex items-end">
          <button className="button-secondary w-full" type="submit">
            Apply filters
          </button>
        </div>
      </div>
    </form>
  );
}
