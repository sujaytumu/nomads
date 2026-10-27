import { api } from "./api";

export type TripPhoto = { id: number; caption?: string | null; dataUrl: string; createdAt?: string };

export const MAX_PHOTOS = 12;

export async function fetchPhotos(tripId: number | string): Promise<TripPhoto[]> {
  const { data } = await api.get(`/api/trips/${tripId}/photos`);
  return Array.isArray(data) ? data : [];
}

export async function fetchSharedPhotos(token: string): Promise<TripPhoto[]> {
  const { data } = await api.get(`/api/share/${token}/photos`);
  return Array.isArray(data) ? data : [];
}

export async function addPhoto(tripId: number | string, dataUrl: string, caption?: string): Promise<TripPhoto> {
  const { data } = await api.post(`/api/trips/${tripId}/photos`, { dataUrl, caption: caption || null });
  return data;
}

export async function deletePhoto(tripId: number | string, photoId: number): Promise<void> {
  await api.delete(`/api/trips/${tripId}/photos/${photoId}`);
}

/** Shrinks a picture from the camera or gallery to a small JPEG so it uploads quickly. */
export function compressImage(file: File, maxSide = 1280, quality = 0.72): Promise<string> {
  return new Promise((resolve, reject) => {
    if (!/^image\//.test(file.type)) {
      reject(new Error("Please choose an image file"));
      return;
    }
    const url = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => {
      URL.revokeObjectURL(url);
      const scale = Math.min(1, maxSide / Math.max(img.width, img.height));
      const w = Math.max(1, Math.round(img.width * scale));
      const h = Math.max(1, Math.round(img.height * scale));
      const canvas = document.createElement("canvas");
      canvas.width = w;
      canvas.height = h;
      const ctx = canvas.getContext("2d");
      if (!ctx) {
        reject(new Error("Couldn't read that picture"));
        return;
      }
      ctx.drawImage(img, 0, 0, w, h);
      let q = quality;
      let out = canvas.toDataURL("image/jpeg", q);
      // Stay under the server's size limit
      while (out.length > 600000 && q > 0.35) {
        q -= 0.1;
        out = canvas.toDataURL("image/jpeg", q);
      }
      if (out.length > 690000) {
        reject(new Error("That picture is too large"));
        return;
      }
      resolve(out);
    };
    img.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error("Couldn't read that picture"));
    };
    img.src = url;
  });
}
