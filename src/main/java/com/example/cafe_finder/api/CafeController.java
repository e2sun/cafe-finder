package com.example.cafe_finder.api;

import com.example.cafe_finder.service.CafeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
public class CafeController {

    private final CafeService cafeService;

    public CafeController(CafeService cafeService){
        this.cafeService = cafeService;
    }

    @GetMapping("/api/cafes")
    public List<CafeService.Cafe> cafes(
            @org.springframework.web.bind.annotation.RequestParam double swLat,
            @org.springframework.web.bind.annotation.RequestParam double swLng,
            @org.springframework.web.bind.annotation.RequestParam double neLat,
            @org.springframework.web.bind.annotation.RequestParam double neLng,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "50") int limit
    ) {
        return cafeService.findCafesInBbox(swLat, swLng, neLat, neLng, limit);
    }
}
