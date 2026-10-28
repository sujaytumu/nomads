package com.tripfactory.nomad.config;

import java.util.ArrayList;
import java.util.List;

import com.tripfactory.nomad.domain.enums.InterestType;

/**
 * More places for every city, so longer trips (up to 7 days) have enough good, nearby choices.
 * One place per line: city | name | latitude | longitude | category | rating.
 */
final class PlaceCatalog {

    record Entry(String city, String name, double lat, double lon, InterestType category, double rating) {
    }

    private static final String DATA = """
        Bengaluru|Vidhana Soudha|12.9796|77.5906|CULTURE|4.5
        Bengaluru|Tipu Sultan's Summer Palace|12.9593|77.5737|CULTURE|4.2
        Bengaluru|Bull Temple (Dodda Basavana Gudi)|12.9429|77.5680|CULTURE|4.5
        Bengaluru|Visvesvaraya Industrial and Technological Museum|12.9749|77.5961|CULTURE|4.4
        Bengaluru|Jawaharlal Nehru Planetarium|12.9840|77.5890|CULTURE|4.4
        Bengaluru|Bengaluru Fort|12.9631|77.5762|CULTURE|4.0
        Bengaluru|Ranga Shankara Theatre|12.9097|77.5903|CULTURE|4.6
        Bengaluru|Bannerghatta National Park|12.8002|77.5772|ADVENTURE|4.3
        Bengaluru|Wonderla Amusement Park|12.8345|77.4010|ADVENTURE|4.5
        Bengaluru|Savandurga Hill|12.9211|77.2951|ADVENTURE|4.4
        Bengaluru|Nandi Hills|13.3702|77.6835|NATURE|4.5
        Bengaluru|Hebbal Lake|13.0450|77.5950|NATURE|4.2
        Bengaluru|Sankey Tank|13.0089|77.5732|RELAXATION|4.3
        Bengaluru|Lumbini Gardens|13.0369|77.6063|RELAXATION|4.0
        Bengaluru|Commercial Street|12.9822|77.6083|SHOPPING|4.2
        Bengaluru|UB City Mall|12.9716|77.5963|SHOPPING|4.4
        Bengaluru|Jayanagar 4th Block Market|12.9250|77.5826|SHOPPING|4.3
        Bengaluru|Phoenix Marketcity Whitefield|12.9975|77.6963|SHOPPING|4.4
        Bengaluru|MTR (Mavalli Tiffin Rooms)|12.9551|77.5857|FOOD|4.5
        Bengaluru|Vidyarthi Bhavan|12.9459|77.5733|FOOD|4.5
        Bengaluru|Church Street|12.9753|77.6055|NIGHTLIFE|4.4
        Bengaluru|Indiranagar 100 Feet Road|12.9719|77.6412|NIGHTLIFE|4.4
        Bengaluru|Koramangala Pubs|12.9352|77.6245|NIGHTLIFE|4.3

        Mumbai|Chhatrapati Shivaji Terminus|18.9398|72.8355|CULTURE|4.6
        Mumbai|Haji Ali Dargah|18.9827|72.8089|CULTURE|4.5
        Mumbai|Siddhivinayak Temple|19.0169|72.8302|CULTURE|4.6
        Mumbai|Chhatrapati Shivaji Maharaj Vastu Sangrahalaya|18.9269|72.8326|CULTURE|4.5
        Mumbai|Elephanta Caves|18.9633|72.9315|CULTURE|4.3
        Mumbai|Global Vipassana Pagoda|19.2310|72.8068|CULTURE|4.5
        Mumbai|Prithvi Theatre|19.1069|72.8264|CULTURE|4.5
        Mumbai|Dhobi Ghat|18.9826|72.8362|CULTURE|4.1
        Mumbai|Kanheri Caves|19.2051|72.9068|ADVENTURE|4.4
        Mumbai|Juhu Beach|19.0988|72.8266|RELAXATION|4.2
        Mumbai|Worli Sea Face|19.0176|72.8157|RELAXATION|4.4
        Mumbai|Aksa Beach|19.1767|72.7943|RELAXATION|4.0
        Mumbai|Hanging Gardens|18.9569|72.8051|NATURE|4.1
        Mumbai|Powai Lake|19.1273|72.9048|NATURE|4.1
        Mumbai|Crawford Market|18.9473|72.8342|SHOPPING|4.2
        Mumbai|Linking Road|19.0640|72.8337|SHOPPING|4.3
        Mumbai|Phoenix Palladium|18.9941|72.8259|SHOPPING|4.4
        Mumbai|Mohammed Ali Road|18.9577|72.8350|FOOD|4.4
        Mumbai|Leopold Cafe|18.9229|72.8317|FOOD|4.3
        Mumbai|Lower Parel Pubs|18.9937|72.8300|NIGHTLIFE|4.3

        Delhi|Red Fort|28.6562|77.2410|CULTURE|4.5
        Delhi|Humayun's Tomb|28.5933|77.2507|CULTURE|4.6
        Delhi|Jama Masjid|28.6507|77.2334|CULTURE|4.5
        Delhi|Lotus Temple|28.5535|77.2588|CULTURE|4.6
        Delhi|Gurudwara Bangla Sahib|28.6264|77.2090|CULTURE|4.7
        Delhi|National Museum|28.6117|77.2195|CULTURE|4.4
        Delhi|Agrasen ki Baoli|28.6257|77.2252|CULTURE|4.4
        Delhi|Purana Qila|28.6096|77.2437|CULTURE|4.3
        Delhi|Raj Ghat|28.6410|77.2488|CULTURE|4.4
        Delhi|National Rail Museum|28.5827|77.1829|CULTURE|4.4
        Delhi|Sunder Nursery|28.5937|77.2437|NATURE|4.6
        Delhi|Garden of Five Senses|28.5116|77.1969|NATURE|4.2
        Delhi|Okhla Bird Sanctuary|28.5590|77.3170|NATURE|4.1
        Delhi|Kingdom of Dreams|28.4690|77.0680|ADVENTURE|4.3
        Delhi|Connaught Place|28.6315|77.2167|SHOPPING|4.4
        Delhi|Khan Market|28.6006|77.2275|SHOPPING|4.3
        Delhi|Dilli Haat INA|28.5726|77.2070|SHOPPING|4.3
        Delhi|Sarojini Nagar Market|28.5750|77.1990|SHOPPING|4.2
        Delhi|Karim's, Jama Masjid|28.6495|77.2335|FOOD|4.4
        Delhi|Paranthe Wali Gali|28.6560|77.2300|FOOD|4.2
        Delhi|Majnu ka Tila|28.7020|77.2300|FOOD|4.3
        Delhi|Cyber Hub, Gurugram|28.4950|77.0889|NIGHTLIFE|4.4

        Hyderabad|Chowmahalla Palace|17.3578|78.4717|CULTURE|4.5
        Hyderabad|Salar Jung Museum|17.3713|78.4804|CULTURE|4.5
        Hyderabad|Birla Mandir|17.4062|78.4691|CULTURE|4.6
        Hyderabad|Mecca Masjid|17.3604|78.4736|CULTURE|4.5
        Hyderabad|Qutb Shahi Tombs|17.3943|78.3951|CULTURE|4.4
        Hyderabad|Taramati Baradari|17.3840|78.3900|CULTURE|4.2
        Hyderabad|Necklace Road|17.4140|78.4670|RELAXATION|4.2
        Hyderabad|Lumbini Park|17.4101|78.4730|RELAXATION|4.1
        Hyderabad|Durgam Cheruvu|17.4344|78.3917|RELAXATION|4.5
        Hyderabad|Osman Sagar Lake|17.3770|78.3010|RELAXATION|4.2
        Hyderabad|Nehru Zoological Park|17.3503|78.4513|NATURE|4.4
        Hyderabad|KBR National Park|17.4235|78.4140|NATURE|4.3
        Hyderabad|Mrugavani National Park|17.3434|78.3126|NATURE|4.2
        Hyderabad|Wonderla Hyderabad|17.2380|78.5987|ADVENTURE|4.5
        Hyderabad|Shilparamam|17.4529|78.3818|SHOPPING|4.2
        Hyderabad|Inorbit Mall|17.4343|78.3869|SHOPPING|4.4
        Hyderabad|Nimrah Cafe|17.3611|78.4742|FOOD|4.5
        Hyderabad|Shah Ghouse Cafe, Tolichowki|17.4015|78.4140|FOOD|4.3
        Hyderabad|Hitech City Pubs|17.4435|78.3772|NIGHTLIFE|4.2

        Vishakapatnam|Simhachalam Temple|17.7663|83.2505|CULTURE|4.6
        Vishakapatnam|Thotlakonda Buddhist Complex|17.8090|83.4062|CULTURE|4.3
        Vishakapatnam|Visakha Museum|17.7172|83.3225|CULTURE|4.2
        Vishakapatnam|Ross Hill Church|17.7000|83.3070|CULTURE|4.3
        Vishakapatnam|Sri Kanaka Mahalakshmi Temple|17.7110|83.3040|CULTURE|4.4
        Vishakapatnam|Kambalakonda Wildlife Sanctuary|17.8000|83.3500|NATURE|4.3
        Vishakapatnam|Tenneti Park|17.7436|83.3450|NATURE|4.3
        Vishakapatnam|Dolphin's Nose|17.7088|83.3341|NATURE|4.3
        Vishakapatnam|Matsyadarshini Aquarium|17.7167|83.3230|NATURE|4.1
        Vishakapatnam|Katiki Waterfalls|18.3000|82.9700|NATURE|4.3
        Vishakapatnam|Borra Caves|18.2805|83.0398|ADVENTURE|4.5
        Vishakapatnam|Yarada Beach|17.6560|83.2740|RELAXATION|4.4
        Vishakapatnam|Lawsons Bay Beach|17.7280|83.3350|RELAXATION|4.2
        Vishakapatnam|Sagar Nagar Beach|17.7580|83.3640|RELAXATION|4.3
        Vishakapatnam|Bheemunipatnam Beach|17.8925|83.4510|RELAXATION|4.2
        Vishakapatnam|Gangavaram Beach|17.6280|83.2210|RELAXATION|4.2
        Vishakapatnam|VUDA Park|17.7140|83.3220|RELAXATION|4.2
        Vishakapatnam|CMR Central Mall|17.7240|83.3050|SHOPPING|4.2
        Vishakapatnam|Poorna Market|17.7220|83.2990|SHOPPING|4.1
        Vishakapatnam|Beach Road Pubs|17.7170|83.3290|NIGHTLIFE|4.0

        Chennai|Marina Beach|13.0500|80.2824|RELAXATION|4.5
        Chennai|Elliot's Beach|13.0003|80.2723|RELAXATION|4.4
        Chennai|Anna Nagar Tower Park|13.0850|80.2100|RELAXATION|4.2
        Chennai|Kapaleeshwarar Temple|13.0337|80.2697|CULTURE|4.6
        Chennai|San Thome Basilica|13.0337|80.2780|CULTURE|4.6
        Chennai|Fort St. George|13.0795|80.2874|CULTURE|4.3
        Chennai|Government Museum Egmore|13.0732|80.2562|CULTURE|4.3
        Chennai|Valluvar Kottam|13.0500|80.2418|CULTURE|4.2
        Chennai|Ashtalakshmi Temple|12.9992|80.2720|CULTURE|4.5
        Chennai|Birla Planetarium|13.0063|80.2430|CULTURE|4.4
        Chennai|Mahabalipuram Shore Temple|12.6166|80.1993|CULTURE|4.6
        Chennai|DakshinaChitra|12.8244|80.2335|CULTURE|4.4
        Chennai|Guindy National Park|13.0044|80.2290|NATURE|4.2
        Chennai|Semmozhi Poonga|13.0505|80.2556|NATURE|4.4
        Chennai|Theosophical Society Gardens|13.0087|80.2584|NATURE|4.5
        Chennai|Chetpet Eco Park|13.0734|80.2410|NATURE|4.2
        Chennai|Muttukadu Boat House|12.8040|80.2480|ADVENTURE|4.2
        Chennai|Madras Crocodile Bank|12.7424|80.2437|ADVENTURE|4.3
        Chennai|Ranganathan Street, T. Nagar|13.0405|80.2337|SHOPPING|4.3
        Chennai|Phoenix Marketcity Velachery|12.9908|80.2181|SHOPPING|4.5
        Chennai|Express Avenue Mall|13.0586|80.2640|SHOPPING|4.4
        Chennai|Mylapore Food Walk|13.0340|80.2680|FOOD|4.5
        Chennai|Murugan Idli Shop, T. Nagar|13.0430|80.2330|FOOD|4.5
        Chennai|Nungambakkam Pubs|13.0610|80.2430|NIGHTLIFE|4.2

        Kolkata|Victoria Memorial|22.5448|88.3426|CULTURE|4.7
        Kolkata|Howrah Bridge|22.5851|88.3468|CULTURE|4.6
        Kolkata|Dakshineswar Kali Temple|22.6547|88.3577|CULTURE|4.6
        Kolkata|Belur Math|22.6322|88.3556|CULTURE|4.6
        Kolkata|Kalighat Kali Temple|22.5200|88.3420|CULTURE|4.5
        Kolkata|Indian Museum|22.5580|88.3510|CULTURE|4.4
        Kolkata|Marble Palace|22.5832|88.3600|CULTURE|4.3
        Kolkata|St. Paul's Cathedral|22.5446|88.3470|CULTURE|4.5
        Kolkata|Science City|22.5400|88.3963|CULTURE|4.4
        Kolkata|Kumartuli|22.6000|88.3600|CULTURE|4.2
        Kolkata|Eco Park|22.6030|88.4640|NATURE|4.5
        Kolkata|Acharya Jagadish Chandra Bose Botanic Garden|22.5600|88.2900|NATURE|4.2
        Kolkata|Alipore Zoological Garden|22.5400|88.3300|NATURE|4.1
        Kolkata|Princep Ghat|22.5598|88.3303|RELAXATION|4.4
        Kolkata|Rabindra Sarobar|22.5110|88.3500|RELAXATION|4.5
        Kolkata|Millennium Park|22.5540|88.3310|RELAXATION|4.3
        Kolkata|Maidan|22.5584|88.3440|RELAXATION|4.4
        Kolkata|Nicco Park|22.5894|88.4181|ADVENTURE|4.0
        Kolkata|New Market|22.5600|88.3510|SHOPPING|4.2
        Kolkata|College Street Book Market|22.5750|88.3640|SHOPPING|4.4
        Kolkata|South City Mall|22.5016|88.3615|SHOPPING|4.4
        Kolkata|Quest Mall|22.5370|88.3650|SHOPPING|4.4
        Kolkata|Flurys|22.5545|88.3517|FOOD|4.4
        Kolkata|Arsalan, Park Circus|22.5396|88.3640|FOOD|4.4
        Kolkata|Dacres Lane Street Food|22.5690|88.3490|FOOD|4.4
        Kolkata|Tangra Chinatown|22.5560|88.3970|FOOD|4.3
        Kolkata|Park Street|22.5535|88.3520|NIGHTLIFE|4.4

        Pune|Shaniwar Wada|18.5195|73.8553|CULTURE|4.5
        Pune|Aga Khan Palace|18.5524|73.9018|CULTURE|4.5
        Pune|Dagdusheth Halwai Ganpati Temple|18.5164|73.8561|CULTURE|4.7
        Pune|Parvati Hill|18.4967|73.8420|CULTURE|4.5
        Pune|Pataleshwar Cave Temple|18.5290|73.8470|CULTURE|4.3
        Pune|Raja Dinkar Kelkar Museum|18.5120|73.8540|CULTURE|4.5
        Pune|Lal Mahal|18.5190|73.8550|CULTURE|4.1
        Pune|Sinhagad Fort|18.3663|73.7559|ADVENTURE|4.5
        Pune|Lohagad Fort|18.7110|73.4780|ADVENTURE|4.5
        Pune|Kamshet Paragliding|18.7700|73.5890|ADVENTURE|4.4
        Pune|Osho Teerth Park|18.5410|73.8960|NATURE|4.4
        Pune|Pune-Okayama Friendship Garden|18.4780|73.8360|NATURE|4.4
        Pune|Mulshi Lake|18.5100|73.5100|NATURE|4.4
        Pune|Pashan Lake|18.5380|73.7900|NATURE|4.2
        Pune|Empress Garden|18.5090|73.8920|NATURE|4.1
        Pune|Katraj Snake Park|18.4540|73.8560|NATURE|4.1
        Pune|Khadakwasla Dam|18.4400|73.7660|RELAXATION|4.4
        Pune|Sarasbaug|18.5018|73.8553|RELAXATION|4.3
        Pune|Panshet Dam|18.4000|73.6330|RELAXATION|4.3
        Pune|Tulshibaug Market|18.5120|73.8560|SHOPPING|4.2
        Pune|FC Road|18.5240|73.8410|SHOPPING|4.4
        Pune|Phoenix Marketcity Viman Nagar|18.5620|73.9180|SHOPPING|4.5
        Pune|Vaishali Restaurant|18.5207|73.8410|FOOD|4.4
        Pune|Bedekar Misal|18.5110|73.8480|FOOD|4.5
        Pune|German Bakery|18.5360|73.8940|FOOD|4.3
        Pune|Koregaon Park Pubs|18.5362|73.8938|NIGHTLIFE|4.5
        """;

    private PlaceCatalog() {
    }

    static List<Entry> entries() {
        List<Entry> out = new ArrayList<>();
        for (String line : DATA.split("\n")) {
            String t = line.trim();
            if (t.isEmpty()) {
                continue;
            }
            String[] f = t.split("\\|");
            out.add(new Entry(f[0], f[1], Double.parseDouble(f[2]), Double.parseDouble(f[3]),
                    InterestType.valueOf(f[4]), Double.parseDouble(f[5])));
        }
        return out;
    }
}
