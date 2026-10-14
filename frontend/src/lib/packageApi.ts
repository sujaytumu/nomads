import { api } from "./api";

export type PackageSort = "" | "price_asc" | "price_desc" | "name";

export type PackageQuery = {
  q?: string;
  minPrice?: string;
  maxPrice?: string;
  sort?: PackageSort;
};

export async function searchPackages(query: PackageQuery) {
  const params: Record<string, string> = {};
  if (query.q?.trim()) params.q = query.q.trim();
  if (query.minPrice) params.minPrice = query.minPrice;
  if (query.maxPrice) params.maxPrice = query.maxPrice;
  if (query.sort) params.sort = query.sort;
  const { data } = await api.get("/api/packages/search", { params });
  return data as any[];
}
