import { api } from "./api";

export async function createPayment(payload: { tripRequestId: number; amount: number }) {
  const { data } = await api.post("/api/payment/create", payload);
  return data;
}

export async function verifyPayment(payload: {
  razorpayOrderId: string;
  razorpayPaymentId: string;
  razorpaySignature: string;
}) {
  const { data } = await api.post("/api/payment/verify", payload);
  return data;
}
