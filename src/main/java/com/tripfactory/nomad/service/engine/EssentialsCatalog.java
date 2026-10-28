package com.tripfactory.nomad.service.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Where to stay, scenic spots and hospitals for each city. Stay entries are neighbourhoods (not single
 * hotels) with a typical nightly price; hospitals are large 24x7 emergency hospitals. Coordinates are only used
 * to rank by distance. Directions always search by name, so a small coordinate error can't send anyone astray.
 * Format: city | kind | name | latitude | longitude | extra... (see parse).
 */
public final class EssentialsCatalog {

    public enum Kind { STAY, SCENIC, MEDICAL }

    /** extra1: STAY = tier (BUDGET, COMFORT, PREMIUM), SCENIC = best time, MEDICAL = Government or Private. extra2: STAY = typical price per night in rupees, else blank. note: why it's worth it. */
    public record Item(String city, Kind kind, String name, double lat, double lon, String extra1, int price, String note) {
    }

    private static final String DATA = """
        Bengaluru|STAY|MG Road and Brigade Road|12.9756|77.6068|COMFORT|3000|Central, on the metro, close to Cubbon Park and Church Street
        Bengaluru|STAY|Indiranagar|12.9719|77.6412|COMFORT|3200|Lively cafes and evenings, easy cabs
        Bengaluru|STAY|Majestic and Kempegowda|12.9767|77.5713|BUDGET|1100|Cheapest, beside the railway and bus stations
        Bengaluru|STAY|Jayanagar|12.9250|77.5826|BUDGET|1600|Quiet and clean, near Lalbagh and good food
        Bengaluru|STAY|UB City and Lavelle Road|12.9716|77.5963|PREMIUM|7000|Luxury hotels in the middle of the city
        Bengaluru|SCENIC|Nandi Hills|13.3702|77.6835|Sunrise|0|Sea of clouds at dawn. Leave by 4 am
        Bengaluru|SCENIC|Lalbagh Lake|12.9507|77.5848|Morning|0|Rocks, old trees and the glasshouse
        Bengaluru|SCENIC|Ulsoor Lake|12.9836|77.6191|Evening|0|Boating and a calm sunset
        Bengaluru|SCENIC|Hebbal Lake|13.0450|77.5950|Sunset|0|Birds and open sky
        Bengaluru|SCENIC|Savandurga Hill|12.9211|77.2951|Morning|0|A huge monolith to climb, a day trip
        Bengaluru|MEDICAL|Victoria Hospital|12.9626|77.5740|Government|0|Large government hospital with 24-hour emergency
        Bengaluru|MEDICAL|Manipal Hospital, Old Airport Road|12.9585|77.6489|Private|0|Multi-speciality, 24-hour emergency
        Bengaluru|MEDICAL|St. John's Medical College Hospital|12.9306|77.6200|Private|0|Big teaching hospital, 24-hour emergency
        Bengaluru|MEDICAL|Fortis Hospital, Bannerghatta Road|12.8950|77.5990|Private|0|Multi-speciality, 24-hour emergency

        Mumbai|STAY|Colaba|18.9067|72.8147|PREMIUM|6500|Beside the Gateway of India, walk to the sights
        Mumbai|STAY|Bandra West|19.0596|72.8295|COMFORT|4500|Cafes, sea view, quick to Juhu and the sea link
        Mumbai|STAY|Dadar|19.0178|72.8478|BUDGET|1900|Central, trains to every part of the city
        Mumbai|STAY|Andheri East|19.1136|72.8697|BUDGET|2200|Near the airport and metro, many options
        Mumbai|STAY|Juhu|19.1075|72.8263|COMFORT|5000|Beachside, relaxed
        Mumbai|SCENIC|Marine Drive|18.9440|72.8238|Sunset|0|The Queen's Necklace at dusk
        Mumbai|SCENIC|Worli Sea Face|19.0176|72.8157|Sunset|0|Sea-link view and a long walk
        Mumbai|SCENIC|Bandra Fort|19.0426|72.8186|Sunset|0|Sea, the sea link and the skyline
        Mumbai|SCENIC|Hanging Gardens|18.9569|72.8051|Evening|0|City lights from the hill
        Mumbai|SCENIC|Kanheri Caves|19.2051|72.9068|Morning|0|Forest hill with rock caves
        Mumbai|MEDICAL|KEM Hospital, Parel|18.9990|72.8420|Government|0|Large government hospital, 24-hour emergency
        Mumbai|MEDICAL|Lilavati Hospital, Bandra|19.0510|72.8290|Private|0|Multi-speciality, 24-hour emergency
        Mumbai|MEDICAL|Kokilaben Hospital, Andheri|19.1313|72.8250|Private|0|Multi-speciality, 24-hour emergency
        Mumbai|MEDICAL|JJ Hospital|18.9630|72.8330|Government|0|South Mumbai government hospital, 24-hour emergency

        Delhi|STAY|Connaught Place|28.6315|77.2167|COMFORT|3800|Central, on the metro, close to many sights
        Delhi|STAY|Paharganj|28.6430|77.2140|BUDGET|1100|Cheapest, next to New Delhi station
        Delhi|STAY|Karol Bagh|28.6519|77.1909|BUDGET|1900|Metro, shops and food, good value
        Delhi|STAY|Hauz Khas and South Delhi|28.5494|77.2001|COMFORT|3200|Quieter, cafes and parks
        Delhi|STAY|Aerocity|28.5493|77.1210|PREMIUM|6000|Near the airport, big hotels
        Delhi|SCENIC|India Gate|28.6129|77.2295|Evening|0|Lit up after dark, lawns to sit on
        Delhi|SCENIC|Sunder Nursery|28.5937|77.2437|Morning|0|Gardens around Humayun's Tomb
        Delhi|SCENIC|Humayun's Tomb|28.5933|77.2507|Golden hour|0|Red sandstone in warm light
        Delhi|SCENIC|Lodhi Garden|28.5933|77.2197|Morning|0|Tombs among lawns and trees
        Delhi|SCENIC|Hauz Khas Lake|28.5494|77.1930|Sunset|0|Lake and ruins at dusk
        Delhi|MEDICAL|AIIMS|28.5672|77.2100|Government|0|India's top government hospital, 24-hour emergency
        Delhi|MEDICAL|Safdarjung Hospital|28.5680|77.2060|Government|0|Large government hospital, 24-hour emergency
        Delhi|MEDICAL|Ram Manohar Lohia Hospital|28.6260|77.2010|Government|0|Close to Connaught Place, 24-hour emergency
        Delhi|MEDICAL|Max Super Speciality Hospital, Saket|28.5280|77.2130|Private|0|Multi-speciality, 24-hour emergency

        Hyderabad|STAY|Banjara Hills|17.4156|78.4347|COMFORT|3600|Cafes and malls, quick to Hussain Sagar
        Hyderabad|STAY|Abids and Nampally|17.3910|78.4750|BUDGET|1600|Close to Charminar and the old city
        Hyderabad|STAY|Hitech City|17.4435|78.3772|COMFORT|3200|Modern area near the IT hub
        Hyderabad|STAY|Secunderabad|17.4399|78.4983|BUDGET|1600|By the railway station and Paradise biryani
        Hyderabad|STAY|Begumpet|17.4440|78.4640|COMFORT|3000|Central between the old and new city
        Hyderabad|SCENIC|Hussain Sagar Lake|17.4239|78.4738|Sunset|0|Buddha statue on the water, lit at night
        Hyderabad|SCENIC|Golconda Fort|17.3833|78.4011|Sunrise|0|Hill top view and a fort walk
        Hyderabad|SCENIC|Durgam Cheruvu|17.4344|78.3917|Sunset|0|Secret lake with a cable bridge
        Hyderabad|SCENIC|Necklace Road|17.4140|78.4670|Evening|0|Lake-side promenade
        Hyderabad|SCENIC|Osman Sagar Lake|17.3770|78.3010|Sunset|0|Wide lake just outside the city
        Hyderabad|MEDICAL|Osmania General Hospital|17.3730|78.4740|Government|0|Large government hospital, 24-hour emergency
        Hyderabad|MEDICAL|NIMS, Punjagutta|17.4240|78.4510|Government|0|Large specialist hospital, 24-hour emergency
        Hyderabad|MEDICAL|Apollo Hospitals, Jubilee Hills|17.4170|78.4110|Private|0|Multi-speciality, 24-hour emergency
        Hyderabad|MEDICAL|Yashoda Hospital, Somajiguda|17.4240|78.4580|Private|0|Multi-speciality, 24-hour emergency

        Vishakapatnam|STAY|Beach Road|17.7170|83.3290|COMFORT|3500|Sea-facing hotels, near RK Beach and the museums
        Vishakapatnam|STAY|Dwaraka Nagar|17.7290|83.3100|BUDGET|1500|Busy and cheap, near the station and bus stand
        Vishakapatnam|STAY|MVP Colony|17.7420|83.3350|BUDGET|2200|Residential, quiet, close to the beach
        Vishakapatnam|STAY|Rushikonda|17.7826|83.3851|COMFORT|4200|Resorts by the beach, further from the city
        Vishakapatnam|STAY|Siripuram|17.7200|83.3150|COMFORT|3000|Central, shops and restaurants
        Vishakapatnam|SCENIC|Kailasagiri Hill Park|17.7505|83.3426|Sunset|0|Hill top view of the bay
        Vishakapatnam|SCENIC|Dolphin's Nose|17.7088|83.3341|Sunrise|0|Lighthouse cliff over the sea
        Vishakapatnam|SCENIC|RK Beach|17.7126|83.3239|Sunrise|0|Long sandy walk by the city
        Vishakapatnam|SCENIC|Rushikonda Beach|17.7826|83.3851|Morning|0|Clean beach with blue water
        Vishakapatnam|SCENIC|Yarada Beach|17.6560|83.2740|Sunset|0|Quiet beach between hills
        Vishakapatnam|MEDICAL|King George Hospital|17.7140|83.3080|Government|0|Main government hospital, 24-hour emergency
        Vishakapatnam|MEDICAL|Apollo Hospitals, Waltair|17.7140|83.3230|Private|0|Multi-speciality, 24-hour emergency
        Vishakapatnam|MEDICAL|CARE Hospitals, Ramnagar|17.7230|83.3100|Private|0|Multi-speciality, 24-hour emergency

        Chennai|STAY|T. Nagar|13.0418|80.2341|BUDGET|2000|Shops and food, close to everything by auto
        Chennai|STAY|Mylapore|13.0337|80.2697|COMFORT|2800|Temples, filter coffee and the beach
        Chennai|STAY|Egmore|13.0732|80.2609|BUDGET|1500|By the railway station and the museum
        Chennai|STAY|Nungambakkam|13.0610|80.2430|PREMIUM|5500|Quiet, central, business hotels
        Chennai|STAY|Besant Nagar|12.9992|80.2720|COMFORT|3200|Next to Elliot's Beach, calm evenings
        Chennai|SCENIC|Marina Beach|13.0500|80.2824|Sunrise|0|One of the longest city beaches
        Chennai|SCENIC|Elliot's Beach|13.0003|80.2723|Sunrise|0|Cleaner and calmer than Marina
        Chennai|SCENIC|Mahabalipuram Shore Temple|12.6166|80.1993|Sunrise|0|Stone temple over the sea, a day trip
        Chennai|SCENIC|Theosophical Society Gardens|13.0087|80.2584|Morning|0|Giant banyan tree and quiet paths
        Chennai|SCENIC|Semmozhi Poonga|13.0505|80.2556|Evening|0|Garden in the middle of the city
        Chennai|MEDICAL|Government General Hospital|13.0810|80.2760|Government|0|Large government hospital, 24-hour emergency
        Chennai|MEDICAL|Apollo Hospitals, Greams Road|13.0630|80.2530|Private|0|Multi-speciality, 24-hour emergency
        Chennai|MEDICAL|Kauvery Hospital, Alwarpet|13.0350|80.2530|Private|0|Multi-speciality, 24-hour emergency
        Chennai|MEDICAL|MIOT International|13.0230|80.1830|Private|0|Multi-speciality, 24-hour emergency

        Kolkata|STAY|Park Street|22.5535|88.3520|COMFORT|3800|Heritage hotels, cafes, close to the Maidan
        Kolkata|STAY|Esplanade and New Market|22.5646|88.3510|COMFORT|2600|Central, trams and metro
        Kolkata|STAY|Howrah Station area|22.5850|88.3420|BUDGET|1200|Cheapest, by the station and Howrah Bridge
        Kolkata|STAY|Salt Lake Sector V|22.5750|88.4330|COMFORT|3000|Modern, quiet, near Eco Park
        Kolkata|STAY|Ballygunge|22.5280|88.3650|BUDGET|2000|Residential, good local food
        Kolkata|SCENIC|Princep Ghat|22.5598|88.3303|Sunset|0|Hooghly river and the Vidyasagar Setu
        Kolkata|SCENIC|Victoria Memorial|22.5448|88.3426|Golden hour|0|White marble in evening light
        Kolkata|SCENIC|Mullick Ghat, Howrah Bridge|22.5855|88.3495|Sunrise|0|Flower market and the bridge at dawn
        Kolkata|SCENIC|Eco Park|22.6030|88.4640|Morning|0|Huge lake park with gardens
        Kolkata|SCENIC|Rabindra Sarobar|22.5110|88.3500|Morning|0|Lake walk with trees and birds
        Kolkata|MEDICAL|SSKM Hospital|22.5390|88.3440|Government|0|Large government hospital, 24-hour emergency
        Kolkata|MEDICAL|Medical College Hospital|22.5750|88.3680|Government|0|College Street area, 24-hour emergency
        Kolkata|MEDICAL|Apollo Gleneagles Hospital|22.5750|88.4000|Private|0|Multi-speciality, 24-hour emergency
        Kolkata|MEDICAL|AMRI Hospital, Dhakuria|22.5100|88.3640|Private|0|Multi-speciality, 24-hour emergency

        Pune|STAY|Koregaon Park|18.5362|73.8938|COMFORT|3600|Cafes and nightlife, calm lanes, near Osho Teerth
        Pune|STAY|Shivajinagar and FC Road|18.5300|73.8480|COMFORT|2600|Central, students and food, near old Pune
        Pune|STAY|Camp|18.5128|73.8790|BUDGET|1600|Station side, cheaper rooms
        Pune|STAY|Viman Nagar|18.5679|73.9143|COMFORT|3200|Near the airport and Phoenix Marketcity
        Pune|STAY|Deccan Gymkhana|18.5170|73.8410|BUDGET|2000|Walk to the old city and the river
        Pune|SCENIC|Sinhagad Fort|18.3663|73.7559|Sunrise|0|Hill fort above the clouds, a half-day trip
        Pune|SCENIC|Parvati Hill|18.4967|73.8420|Sunrise|0|Steps up for a view of the whole city
        Pune|SCENIC|Khadakwasla Dam|18.4400|73.7660|Sunset|0|Lake with hills behind it
        Pune|SCENIC|Mulshi Lake|18.5100|73.5100|Morning|0|Green hills and water, a day trip
        Pune|SCENIC|Pashan Lake|18.5380|73.7900|Morning|0|Bird watching close to the city
        Pune|MEDICAL|Sassoon General Hospital|18.5270|73.8750|Government|0|Large government hospital, 24-hour emergency
        Pune|MEDICAL|Ruby Hall Clinic|18.5330|73.8770|Private|0|Multi-speciality, 24-hour emergency
        Pune|MEDICAL|Deenanath Mangeshkar Hospital|18.5070|73.8330|Private|0|Multi-speciality, 24-hour emergency
        Pune|MEDICAL|Jehangir Hospital|18.5290|73.8770|Private|0|Multi-speciality, 24-hour emergency
        """;

    private EssentialsCatalog() {
    }

    public static List<Item> items() {
        List<Item> out = new ArrayList<>();
        for (String line : DATA.split("\n")) {
            String t = line.trim();
            if (t.isEmpty()) {
                continue;
            }
            String[] f = t.split("\\|", -1);
            out.add(new Item(f[0], Kind.valueOf(f[1]), f[2], Double.parseDouble(f[3]), Double.parseDouble(f[4]),
                    f[5], Integer.parseInt(f[6]), f[7]));
        }
        return out;
    }
}
