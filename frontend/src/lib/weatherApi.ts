import { api } from "./api";

export type DailyForecast = {
  date: string;
  maxTempC: number;
  minTempC: number;
  precipitationChance: number;
  description: string;
};

export type WeatherForecast = {
  city: string;
  days: DailyForecast[];
};

export async function fetchTripWeather(tripRequestId: number): Promise<WeatherForecast> {
  const { data } = await api.get(`/api/trips/${tripRequestId}/weather`);
  return data;
}
