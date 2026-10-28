package com.tripfactory.nomad.service.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.tripfactory.nomad.service.engine.EssentialsCatalog.Item;
import com.tripfactory.nomad.service.engine.EssentialsCatalog.Kind;
import com.tripfactory.nomad.service.util.GeoUtils;

/**
 * Picks the stay areas, scenic spots and hospitals that suit one trip. Everything is measured from the trip's own
 * stops (or from the traveller's home when there are none), so the picks change with the itinerary.
 */
public final class EssentialsPlanner {

    public record Point(double lat, double lon) {
    }

    /** A ranked pick with how far it is from the trip. */
    public record Pick(Item item, double kmFromTrip, double avgKmToStops) {
    }

    private EssentialsPlanner() {
    }

    public static Point centre(List<Point> stops, Point fallback) {
        if (stops == null || stops.isEmpty()) {
            return fallback;
        }
        double lat = 0;
        double lon = 0;
        for (Point p : stops) {
            lat += p.lat();
            lon += p.lon();
        }
        return new Point(lat / stops.size(), lon / stops.size());
    }

    public static double avgKm(Item item, List<Point> stops, Point centre) {
        if (stops == null || stops.isEmpty()) {
            return GeoUtils.haversineKm(item.lat(), item.lon(), centre.lat(), centre.lon());
        }
        double sum = 0;
        for (Point p : stops) {
            sum += GeoUtils.haversineKm(item.lat(), item.lon(), p.lat(), p.lon());
        }
        return sum / stops.size();
    }

    private static List<Pick> ranked(List<Item> all, String city, Kind kind, List<Point> stops, Point centre) {
        List<Pick> picks = new ArrayList<>();
        for (Item i : all) {
            if (i.kind() == kind && i.city().equalsIgnoreCase(city)) {
                picks.add(new Pick(i, GeoUtils.haversineKm(i.lat(), i.lon(), centre.lat(), centre.lon()), avgKm(i, stops, centre)));
            }
        }
        return picks;
    }

    /** Closest first. */
    public static List<Pick> nearest(List<Item> all, String city, Kind kind, List<Point> stops, Point centre, int limit) {
        List<Pick> picks = ranked(all, city, kind, stops, centre);
        picks.sort(Comparator.comparingDouble(Pick::kmFromTrip));
        return picks.subList(0, Math.min(limit, picks.size()));
    }

    /**
     * One pick per price level first (cheapest area, a comfortable one, a premium one), each the closest to the
     * trip within its level, so the traveller sees real trade-offs between money and travel time.
     */
    public static List<Pick> stays(List<Item> all, String city, List<Point> stops, Point centre, int limit) {
        List<Pick> picks = ranked(all, city, Kind.STAY, stops, centre);
        picks.sort(Comparator.comparingDouble(Pick::avgKmToStops));
        List<Pick> out = new ArrayList<>();
        for (String tier : new String[] { "BUDGET", "COMFORT", "PREMIUM" }) {
            picks.stream().filter(p -> p.item().extra1().equals(tier)).findFirst().ifPresent(out::add);
        }
        for (Pick p : picks) {
            if (out.size() >= limit) {
                break;
            }
            if (!out.contains(p)) {
                out.add(p);
            }
        }
        out.sort(Comparator.comparingInt((Pick p) -> p.item().price()));
        return out.subList(0, Math.min(limit, out.size()));
    }
}
