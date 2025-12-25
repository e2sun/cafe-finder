package com.example.cafe_finder.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class CafeController {

    public record Cafe(String name, double lat, double lon){}

    @GetMapping("/api/cafes")
    public List<Cafe> cafes() {
        return List.of(
                new Cafe("Demo Cafe A", 40.73061, -73.935242),
                new Cafe("Demo Cafe B", 40.73250, -73.99000)
        );
    }
}
