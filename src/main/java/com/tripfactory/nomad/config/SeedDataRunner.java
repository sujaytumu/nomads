package com.tripfactory.nomad.config;

import java.util.List;
import org.springframework.lang.NonNull;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.tripfactory.nomad.domain.entity.Place;
import com.tripfactory.nomad.domain.entity.Vehicle;
import com.tripfactory.nomad.domain.enums.AvailabilityStatus;
import com.tripfactory.nomad.domain.enums.InterestType;
import com.tripfactory.nomad.domain.enums.VehicleType;
import com.tripfactory.nomad.repository.PlaceRepository;
import com.tripfactory.nomad.repository.VehicleRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SeedDataRunner implements CommandLineRunner {

    @Value("${nomad.seed-data:false}")
    private boolean seedData;

    private final PlaceRepository placeRepository;
    private final VehicleRepository vehicleRepository;

    @Override
    public void run(String... args) {
        if (!seedData) {
            return;
        }

        if (placeRepository.count() == 0) {
            placeRepository.saveAll(buildPlaces());
        }

        if (vehicleRepository.count() == 0) {
            vehicleRepository.saveAll(buildVehicles());
        }
    }

    private List<Place> buildPlaces() {
        return List.of(
                createPlace("Lalbagh Botanical Garden", "Bengaluru", 12.9507, 77.5848, InterestType.NATURE, 4.6),
                createPlace("Cubbon Park", "Bengaluru", 12.9763, 77.5929, InterestType.NATURE, 4.5),
                createPlace("Bangalore Palace", "Bengaluru", 12.9987, 77.5921, InterestType.CULTURE, 4.3),
                createPlace("VV Puram Food Street", "Bengaluru", 12.9453, 77.5741, InterestType.FOOD, 4.4),
                createPlace("ISKCON Temple", "Bengaluru", 13.0099, 77.5511, InterestType.CULTURE, 4.5),
                createPlace("Ulsoor Lake", "Bengaluru", 12.9836, 77.6191, InterestType.RELAXATION, 4.2),

                createPlace("Marine Drive", "Mumbai", 18.9440, 72.8238, InterestType.RELAXATION, 4.7),
                createPlace("Gateway of India", "Mumbai", 18.9218, 72.8347, InterestType.CULTURE, 4.6),
                createPlace("Colaba Causeway", "Mumbai", 18.9220, 72.8327, InterestType.SHOPPING, 4.4),
                createPlace("Chowpatty Beach", "Mumbai", 18.9547, 72.8133, InterestType.NATURE, 4.2),
                createPlace("Sanjay Gandhi National Park", "Mumbai", 19.2147, 72.9106, InterestType.NATURE, 4.5),
                createPlace("Bandra Bandstand", "Mumbai", 19.0466, 72.8205, InterestType.NIGHTLIFE, 4.3),

                createPlace("India Gate", "Delhi", 28.6129, 77.2295, InterestType.CULTURE, 4.6),
                createPlace("Qutub Minar", "Delhi", 28.5244, 77.1855, InterestType.CULTURE, 4.5),
                createPlace("Hauz Khas Village", "Delhi", 28.5530, 77.1949, InterestType.NIGHTLIFE, 4.3),
                createPlace("Lodhi Garden", "Delhi", 28.5933, 77.2197, InterestType.NATURE, 4.4),
                createPlace("Chandni Chowk", "Delhi", 28.6506, 77.2303, InterestType.FOOD, 4.2),
                createPlace("Akshardham", "Delhi", 28.6127, 77.2773, InterestType.CULTURE, 4.6));
    }

    private List<Vehicle> buildVehicles() {
        return List.of(
                createVehicle(VehicleType.CAB, 3, "Arjun Kumar", "KA01AB1234"),
                createVehicle(VehicleType.CAB, 3, "Riya Sharma", "MH02CD5678"),
                createVehicle(VehicleType.MINI_BUS, 8, "Vikram Singh", "DL03EF9012"),
                createVehicle(VehicleType.MINI_BUS, 8, "Meera Nair", "KA04GH3456"),
                createVehicle(VehicleType.BUS, 20, "Sandeep Rao", "MH05IJ7890"));
    }

    private Place createPlace(String name, String city, double lat, double lon, InterestType category, double rating) {
        Place place = new Place();
        place.setName(name);
        place.setCity(city);
        place.setLatitude(lat);
        place.setLongitude(lon);
        place.setCategory(category);
        place.setRating(rating);
        return place;
    }

    private Vehicle createVehicle(VehicleType type, int capacity, String driver, String number) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleType(type);
        vehicle.setCapacity(capacity);
        vehicle.setDriverName(driver);
        vehicle.setVehicleNumber(number);
        vehicle.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        return vehicle;
    }
}