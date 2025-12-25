package com.example.cafe_finder.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class CafeService {
    public record Cafe(String name, double lat, double lon){}

    // public overpass API endpoint (uses OpenStreetMap data)
    private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";

    // reusable HTTP client
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    // JSON parser for reading Overpass responses
    private final JsonMapper mapper = JsonMapper.builder().build();

    // Finds cafes inside the map bounding box
    public List<Cafe> findCafesInBbox(double swLat, double swLng, double neLat, double neLng, int limit) {
        // guardrails
        limit = Math.max(1, Math.min(limit, 200));
        if (Math.abs(neLat - swLat) > 1.5 || Math.abs(neLng - swLng) > 1.5) {
            throw new IllegalArgumentException("Area too large. Zoom in an try again.");
        }

        // bbox order: south, west, north, east
        String query = """
                [out:json][timeout:25];
                (
                  node["amenity"="cafe"](%f,%f,%f,%f);
                  way["amenity"="cafe"](%f,%f,%f,%f);
                  relation["amenity"="cafe"](%f,%f,%f,%f);
                );
                out tags center;
                """.formatted(
                swLat, swLng, neLat, neLng,
                swLat, swLng, neLat, neLng,
                swLat, swLng, neLat, neLng
        );

        // overpass wants query in POST body field named "data"
        String body = "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(OVERPASS_URL))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() != 200) {
                throw new RuntimeException("Overpass HTTP " + resp.statusCode() + ":\n" + resp.body());
            }

            return parseOverpassJson(resp.body(), limit);

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch cafes: " + e.getMessage(), e);
        }
    }

    // Parse Overpass JSON into a List<Cafe>
    private List<Cafe> parseOverpassJson(String json, int limit) throws Exception {
        JsonNode root = mapper.readTree(json);
        JsonNode elements = root.get("elements");

        List<Cafe> out = new ArrayList<>();
        if (elements == null || !elements.isArray()) return out;

        for (JsonNode el : elements) {
            JsonNode tags = el.path("tags");
            String name = tags.path("name").asText("Cafe");

            double lat = el.has("lat")
                    ? el.get("lat").asDouble()
                    : el.path("center").path("lat").asDouble(Double.NaN);
            double lon = el.has("lon")
                    ? el.get("lon").asDouble()
                    : el.path("center").path("lon").asDouble(Double.NaN);

            // skip anything without coordinates
            if (Double.isNaN(lat) || Double.isNaN(lon)) continue;

            out.add(new Cafe(name, lat, lon));
            if (out.size() >= limit) break;
        }

        return out;
    }


}



