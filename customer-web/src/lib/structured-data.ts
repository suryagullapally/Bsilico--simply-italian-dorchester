import {
  BASILICO_ADDRESS,
  BASILICO_LOGO_PATH,
  BASILICO_OG_IMAGE_PATH,
  BASILICO_PHONE_E164,
  SITE_NAME,
  SITE_URL,
  siteUrl,
} from "@/lib/site-config";

export function restaurantJsonLd() {
  return {
    "@context": "https://schema.org",
    "@type": "Restaurant",
    "@id": `${SITE_URL}/#restaurant`,
    name: SITE_NAME,
    url: SITE_URL,
    logo: siteUrl(BASILICO_LOGO_PATH),
    image: [
      siteUrl(BASILICO_OG_IMAGE_PATH),
      siteUrl("/images/restaurant/interior-01.webp"),
      siteUrl("/images/restaurant/interior-03.webp"),
    ],
    telephone: BASILICO_PHONE_E164,
    priceRange: "££",
    servesCuisine: ["Italian", "Pizza", "Neapolitan pizza"],
    acceptsReservations: true,
    menu: siteUrl("/menu"),
    hasMenu: siteUrl("/menu"),
    address: {
      "@type": "PostalAddress",
      streetAddress: BASILICO_ADDRESS.street,
      addressLocality: BASILICO_ADDRESS.locality,
      addressRegion: BASILICO_ADDRESS.region,
      postalCode: BASILICO_ADDRESS.postalCode,
      addressCountry: BASILICO_ADDRESS.country,
    },
    openingHoursSpecification: [
      {
        "@type": "OpeningHoursSpecification",
        dayOfWeek: [
          "Wednesday",
          "Thursday",
          "Friday",
          "Saturday",
          "Sunday",
          "Monday",
        ],
        opens: "12:00",
        closes: "23:00",
      },
    ],
    potentialAction: {
      "@type": "ReserveAction",
      target: siteUrl("/book"),
    },
  };
}
