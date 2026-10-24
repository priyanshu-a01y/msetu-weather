package com.summitsky.model;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Single source of truth for named location scenes. Unknown cities use the weather-only sky. */
public final class LocationVisualCatalog {
    private static final String COMMONS = "https://commons.wikimedia.org/wiki/Special:FilePath/";
    private static final Map<String, LocationVisual> LOCATIONS = Map.ofEntries(
        entry("Manali", "Manali_Mountains.jpg?width=1400", "Snowy Himalayan peaks above Manali", "D Bown · CC BY-SA 2.0", "Manali_Mountains.jpg", "himalaya", "50% 48%"),
        entry("Shimla", "Deodar_forests_of_Shimla_in_monsoon.jpg?width=1400", "Misty deodar forest in the Shimla hills", "RayAdvait · CC BY 4.0", "Deodar_forests_of_Shimla_in_monsoon.jpg", "green-hills", "50% 52%"),
        entry("Leh", "Ladakh_landscape_near_Leh.jpg?width=1400", "Barren high-altitude Ladakh landscape near Leh", "Yann Forget · CC BY-SA 4.0", "Ladakh_landscape_near_Leh.jpg", "high-desert", "50% 50%"),
        entry("Srinagar", "Dal_Lake_Kashmir_Where_Naipaul_Spent_5_Months.jpg?width=1400", "Dal Lake in Srinagar with Kashmir Valley beyond", "Somyadeep Paul · CC BY 2.0", "Dal_Lake_Kashmir_Where_Naipaul_Spent_5_Months.jpg", "kashmir", "50% 48%"),
        entry("Gangtok", "Gangtok_View_Point%2C_Gangtok%2C_Sikkim.jpg?width=1400", "Clouded green hills and forest around Gangtok", "Prabhat114 · CC BY-SA 4.0", "Gangtok_View_Point,_Gangtok,_Sikkim.jpg", "eastern-himalaya", "50% 54%"),
        entry("Mussoorie", "Beautiful_Mussoorie.jpg?width=1400", "Layered green Himalayan hills in Mussoorie", "Bibek2011 · CC BY-SA 3.0", "Beautiful_Mussoorie.jpg", "hill-station", "50% 54%"),
        entry("Prayagraj", "Arail-ghatt.jpg?width=1400", "Arail ghat on the Ganges in Prayagraj", "Vanshiikaa · CC0 1.0", "Arail-ghatt.jpg", "river-city", "50% 48%"),
        entry("Delhi", "Old_Delhi_city_skyline_from_Jama_Masjid%2C_Delhi%2C_India.jpg?width=1400", "Old Delhi city skyline viewed from Jama Masjid", "Vyacheslav Argenberg · CC BY 4.0", "Old_Delhi_city_skyline_from_Jama_Masjid,_Delhi,_India.jpg", "urban-delhi", "50% 46%"),
        entry("Mumbai", "Skyline_of_Mumbai_from_sea_link.jpg?width=1400", "Mumbai skyline from the sea link", "Creater903a · CC0", "Skyline_of_Mumbai_from_sea_link.jpg", "coastal-mumbai", "50% 50%"));

    private static final List<LocationVisual> FEATURED = List.of(
        LOCATIONS.get("manali"), LOCATIONS.get("shimla"), LOCATIONS.get("leh"),
        LOCATIONS.get("srinagar"), LOCATIONS.get("gangtok"), LOCATIONS.get("mussoorie"),
        LOCATIONS.get("prayagraj"));

    private LocationVisualCatalog() {}

    private static Map.Entry<String, LocationVisual> entry(String city, String file, String alt,
            String credit, String sourceFile, String sceneClass, String imagePosition) {
        return Map.entry(key(city), new LocationVisual(city, COMMONS + file, alt, credit,
                "https://commons.wikimedia.org/wiki/File:" + sourceFile, sceneClass, imagePosition));
    }

    public static List<LocationVisual> featured() { return FEATURED; }

    public static LocationVisual forLocation(String resolvedName, String searchQuery) {
        LocationVisual match = LOCATIONS.get(key(resolvedName));
        if (match == null) match = LOCATIONS.get(key(searchQuery));
        if (match != null) return match;
        String name = resolvedName == null || resolvedName.isBlank() ? "Selected location" : resolvedName;
        return new LocationVisual(name, null, "Neutral atmospheric sky; no location-specific image is available.",
                null, null, "atmosphere", "50% 50%");
    }

    private static String key(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (normalized.equals("allahabad") || normalized.equals("allahabadcity")) return "prayagraj";
        if (normalized.equals("newdelhi") || normalized.equals("delhincr")) return "delhi";
        if (normalized.equals("bombay")) return "mumbai";
        if (normalized.equals("mussorie")) return "mussoorie";
        return normalized;
    }
}
