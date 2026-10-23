// Reports which build this server is running. Never cached, so the app can notice a new release.
export const dynamic = "force-dynamic";

export async function GET() {
  return Response.json(
    { id: process.env.NEXT_PUBLIC_BUILD_ID || "dev" },
    { headers: { "Cache-Control": "no-store, max-age=0" } }
  );
}
