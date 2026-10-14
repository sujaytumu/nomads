import { api } from "./api";

export type WishlistPackage = {
  id: number;
  name: string;
  shortDescription: string;
  price: number;
  imageUrl?: string;
};

export async function fetchWishlist() {
  const { data } = await api.get("/api/wishlist");
  return data as WishlistPackage[];
}

export async function addToWishlist(packageId: number) {
  await api.put(`/api/wishlist/${packageId}`);
}

export async function removeFromWishlist(packageId: number) {
  await api.delete(`/api/wishlist/${packageId}`);
}
