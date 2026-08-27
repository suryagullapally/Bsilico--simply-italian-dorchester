const gbpFormatter = new Intl.NumberFormat("en-GB", {
  currency: "GBP",
  style: "currency",
});

export function formatGbpPrice(price: number) {
  return gbpFormatter.format(price);
}

export function priceToPennies(price: number) {
  return Math.round(price * 100);
}

export function formatGbpPennies(pennies: number) {
  return gbpFormatter.format(pennies / 100);
}
