package hongmumuk.hongmumuk.controller;

import hongmumuk.hongmumuk.common.JwtUtil;
import hongmumuk.hongmumuk.dto.PageDto;
import hongmumuk.hongmumuk.dto.ReviewDto;
import hongmumuk.hongmumuk.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("")
    public ResponseEntity<?> allReviews(@RequestParam int restaurantId, @RequestParam int page, @RequestParam String sort) {
        return reviewService.allReviews(restaurantId, page, sort);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createReview(@RequestBody ReviewDto.newReviewDto newReviewDto){
        String email = JwtUtil.getCurrentUserEmail();
        return reviewService.createReview(email, newReviewDto);
    }

    @DeleteMapping("")
    public ResponseEntity<?> deleteReview(@RequestBody ReviewDto.deleteReviewDto deleteReviewDto){
        return reviewService.deleteReview(deleteReviewDto);
    }

    @GetMapping("/available/{rid}")
    public ResponseEntity<?> checkAvailable(@PathVariable Long rid){
        String email = JwtUtil.getCurrentUserEmail();
        return reviewService.checkAvailable(email, rid);
    }
}
