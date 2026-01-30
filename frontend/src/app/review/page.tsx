"use client";

import ProtectedPage from "@/components/ProtectedPage";
import { createReview, fetchReviews } from "@/lib/reviewApi";
import { useState } from "react";

export default function ReviewPage() {
  const [tripId, setTripId] = useState("");
  const [rating, setRating] = useState("5");
  const [comment, setComment] = useState("");
  const [reviews, setReviews] = useState<any[]>([]);

  const handleCreate = async () => {
    await createReview({ tripRequestId: Number(tripId), rating: Number(rating), comment });
    const data = await fetchReviews(Number(tripId));
    setReviews(data);
  };

  const handleFetch = async () => {
    const data = await fetchReviews(Number(tripId));
    setReviews(data);
  };

  return (
    <ProtectedPage>
      <div className="section py-12 space-y-6">
        <div className="card p-6 space-y-4">
        <h2 className="text-2xl font-bold">Review & Feedback</h2>
        <div className="grid md:grid-cols-3 gap-4">
          <input value={tripId} onChange={(e) => setTripId(e.target.value)} placeholder="Trip Request ID" className="border rounded-xl px-4 py-2" />
          <input value={rating} onChange={(e) => setRating(e.target.value)} placeholder="Rating (1-5)" className="border rounded-xl px-4 py-2" />
          <input value={comment} onChange={(e) => setComment(e.target.value)} placeholder="Comment" className="border rounded-xl px-4 py-2" />
        </div>
        <div className="flex gap-3">
          <button className="btn-primary" onClick={handleCreate}>Submit Review</button>
          <button className="btn-outline" onClick={handleFetch}>Fetch Reviews</button>
        </div>
      </div>

        <div className="grid gap-4">
          {reviews.map((review) => (
            <div key={review.id} className="card p-4">
              <p className="font-semibold">Rating: {review.rating}</p>
              <p className="text-sm text-slate-600">{review.comment}</p>
              <p className="text-xs text-slate-400">{review.createdAt}</p>
            </div>
          ))}
        </div>
      </div>
    </ProtectedPage>
  );
}
