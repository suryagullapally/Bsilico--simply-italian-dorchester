const londonTimeZone = "Europe/London";

export function formatDateTime(value: string) {
  return new Intl.DateTimeFormat("en-GB", {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: londonTimeZone,
  }).format(new Date(value));
}

export function formatDate(value: string) {
  return new Intl.DateTimeFormat("en-GB", {
    dateStyle: "medium",
    timeZone: londonTimeZone,
  }).format(new Date(`${value}T12:00:00`));
}

export function formatTime(value: string | null) {
  if (!value) {
    return "ASAP";
  }

  return value.slice(0, 5);
}

export function getLondonDateKey(value: string) {
  return new Date(value).toLocaleDateString("en-CA", {
    timeZone: londonTimeZone,
  });
}

export function todayLondonKey() {
  return new Date().toLocaleDateString("en-CA", {
    timeZone: londonTimeZone,
  });
}
