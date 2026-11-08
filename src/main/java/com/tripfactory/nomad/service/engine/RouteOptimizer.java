package com.tripfactory.nomad.service.engine;

import java.util.ArrayList;
import java.util.List;

import com.tripfactory.nomad.domain.entity.Place;
import com.tripfactory.nomad.service.util.GeoUtils;

/**
 * Finds a short way to visit a set of places, starting from home and not coming back.
 * It builds a first route by always going to the nearest unvisited place, then keeps improving it with 2-opt:
 * reverse a stretch of the route whenever that makes the total shorter, until nothing helps.
 * The start stays fixed. For the handful of stops in a trip this is instant and near-optimal.
 */
public final class RouteOptimizer {

    private RouteOptimizer() {
    }

    public static List<Place> optimize(double homeLat, double homeLon, List<Place> places) {
        int n = places.size();
        if (n <= 2) {
            return nearestFirst(homeLat, homeLon, places);
        }
        List<Place> route = nearestFirst(homeLat, homeLon, places);
        boolean improved = true;
        int guard = 0;
        while (improved && guard++ < 200) {
            improved = false;
            for (int i = 0; i < n - 1; i++) {
                for (int j = i + 1; j < n; j++) {
                    double beforeI = i == 0 ? distHome(homeLat, homeLon, route.get(0)) : dist(route.get(i - 1), route.get(i));
                    double newFirst = i == 0 ? distHome(homeLat, homeLon, route.get(j)) : dist(route.get(i - 1), route.get(j));
                    double afterOld = j < n - 1 ? dist(route.get(j), route.get(j + 1)) : 0;
                    double afterNew = j < n - 1 ? dist(route.get(i), route.get(j + 1)) : 0;
                    double delta = (newFirst + afterNew) - (beforeI + afterOld);
                    if (delta < -1e-9) {
                        reverse(route, i, j);
                        improved = true;
                    }
                }
            }
        }
        return route;
    }

    /** Length of visiting the places in the given order, starting from home. */
    public static double pathKm(double homeLat, double homeLon, List<Place> order) {
        double total = 0;
        double lat = homeLat;
        double lon = homeLon;
        for (Place p : order) {
            total += GeoUtils.haversineKm(lat, lon, p.getLatitude(), p.getLongitude());
            lat = p.getLatitude();
            lon = p.getLongitude();
        }
        return total;
    }

    private static List<Place> nearestFirst(double homeLat, double homeLon, List<Place> places) {
        List<Place> left = new ArrayList<>(places);
        List<Place> route = new ArrayList<>();
        double lat = homeLat;
        double lon = homeLon;
        while (!left.isEmpty()) {
            Place best = left.get(0);
            double bestD = Double.MAX_VALUE;
            for (Place p : left) {
                double d = GeoUtils.haversineKm(lat, lon, p.getLatitude(), p.getLongitude());
                if (d < bestD) {
                    bestD = d;
                    best = p;
                }
            }
            route.add(best);
            left.remove(best);
            lat = best.getLatitude();
            lon = best.getLongitude();
        }
        return route;
    }

    private static void reverse(List<Place> route, int i, int j) {
        while (i < j) {
            Place t = route.get(i);
            route.set(i, route.get(j));
            route.set(j, t);
            i++;
            j--;
        }
    }

    private static double dist(Place a, Place b) {
        return GeoUtils.haversineKm(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude());
    }

    private static double distHome(double lat, double lon, Place p) {
        return GeoUtils.haversineKm(lat, lon, p.getLatitude(), p.getLongitude());
    }
}
