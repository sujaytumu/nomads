import { api } from "./api";

export async function fetchWishlistIds(): Promise<number[]> {
  const { data } = await api.get("/api/wishlist/ids");
  return data as number[];
}

export async function fetchWishlist() {
  const { data } = await api.get("/api/wishlist");
  return data as any[];
}

export async function addToWishlist(packageId: number) {
  await api.post(`/api/wishlist/${packageId}`);
}

export async function removeFromWishlist(packageId: number) {
  await api.delete(`/api/wishlist/${packageId}`);
}
