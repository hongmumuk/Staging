package hongmumuk.hongmumuk.controller;

import hongmumuk.hongmumuk.dto.PageDto;
import hongmumuk.hongmumuk.dto.ReviewDto;
import hongmumuk.hongmumuk.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("")
    public ResponseEntity<?> allReviews(@RequestParam int restaurantId, @RequestBody PageDto pageDto) {
        return reviewService.allReviews(restaurantId, pageDto);
    }
}
