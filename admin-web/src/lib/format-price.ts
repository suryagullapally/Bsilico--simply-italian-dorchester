const gbpFormatter = new Intl.NumberFormat("en-GB", {
  currency: "GBP",
  style: "currency",
});

export function formatGbpPennies(pricePence: number) {
  return gbpFormatter.format(pricePence / 100);
}

export function formatPenniesForInput(pricePence: number) {
  const pounds = Math.floor(pricePence / 100);
  const pennies = Math.abs(pricePence % 100).toString().padStart(2, "0");

  return `${pounds}.${pennies}`;
}

export function parseGbpToPennies(value: string) {
  const normalized = value.trim().replace(/^£/, "");

  if (!/^\d+(\.\d{1,2})?$/.test(normalized)) {
    return null;
  }

  const [pounds, pennies = ""] = normalized.split(".");

  return Number(pounds) * 100 + Number(pennies.padEnd(2, "0"));
}
