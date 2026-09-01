const BASILICO_BACKEND_ORIGIN = "https://basilico-backend-702862739924.europe-west2.run.app";

export default {
  async fetch(request) {
    const incomingUrl = new URL(request.url);
    const originUrl = new URL(BASILICO_BACKEND_ORIGIN);

    originUrl.pathname = incomingUrl.pathname;
    originUrl.search = incomingUrl.search;

    try {
      return await fetch(new Request(originUrl.toString(), request));
    } catch {
      return Response.json(
        { error: "API temporarily unavailable" },
        {
          status: 502,
          headers: {
            "Cache-Control": "no-store"
          }
        }
      );
    }
  }
};
