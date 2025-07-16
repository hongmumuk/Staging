package hongmumuk.hongmumuk.service;

import hongmumuk.hongmumuk.common.response.Apiresponse;
import hongmumuk.hongmumuk.common.response.status.ErrorStatus;
import hongmumuk.hongmumuk.common.response.status.SuccessStatus;
import hongmumuk.hongmumuk.dto.PageDto;
import hongmumuk.hongmumuk.dto.ReviewDto;
import hongmumuk.hongmumuk.dto.ReviewPageDto;
import hongmumuk.hongmumuk.entity.*;
import hongmumuk.hongmumuk.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final TodayReviewRepository todayReviewRepository;

    @Transactional
    public ResponseEntity<?> allReviews(long restaurantId, int page, String sort) {
        long reviewCount = reviewRepository.countByRestaurantId(restaurantId);

        Pageable pageable;
        if ("high".equals(sort)) {
            pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "star"));
        } else if ("low".equals(sort)) {
            pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.ASC, "star"));
        } else {
            pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "createdDate"));
        }

        Page<Review> reviewPage = reviewRepository.findByRestaurantId(restaurantId, pageable);
        List<Review> reviews = reviewPage.getContent();

        if (reviews.isEmpty()) {
            ReviewPageDto emptyResult = new ReviewPageDto(reviewCount, Collections.emptyList());
            return ResponseEntity.ok(Apiresponse.isSuccess(SuccessStatus.OK, emptyResult));
        }

        // 유저 ID 리스트 추출
        List<Long> userIds = reviews.stream()
                .map(r -> r.getUser().getId())
                .distinct()
                .collect(Collectors.toList());

        // 사용자별 리뷰 수 직접 집계 (성능 고려 필요)
        Map<Long, Long> userReviewCounts = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<Review> allReviewsByUsers = reviewRepository.findByUserIdIn(userIds);
            for (Long userId : userIds) {
                long count = allReviewsByUsers.stream()
                        .filter(r -> r.getUser().getId().equals(userId))
                        .count();
                userReviewCounts.put(userId, count);
            }
        }

        // 리뷰 ID 리스트
        List<Long> reviewIds = reviews.stream()
                .map(Review::getId)
                .collect(Collectors.toList());

        // 리뷰별 이미지 URL 매핑
        Map<Long, List<String>> imageUrlsByReviewId = new HashMap<>();
        if (!reviewIds.isEmpty()) {
            List<ReviewImage> images = reviewImageRepository.findAllByReviewIdIn(reviewIds);
            for (ReviewImage ri : images) {
                imageUrlsByReviewId
                        .computeIfAbsent(ri.getReview().getId(), k -> new ArrayList<>())
                        .add(ri.getImageUrl());
            }
        }

        // DTO 변환
        List<ReviewDto> reviewDtos = reviews.stream().map(r -> {
            User user = r.getUser();
            long rank = userReviewCounts.getOrDefault(user.getId(), 0L);
            List<String> imgUrls = imageUrlsByReviewId.getOrDefault(r.getId(), Collections.emptyList());

            return new ReviewDto(
                    r.getId(),
                    user.getNickName(),
                    r.getStar(),
                    r.getContent(),
                    r.getCreatedDate(),
                    rank,
                    imgUrls
            );
        }).collect(Collectors.toList());

        ReviewPageDto result = new ReviewPageDto(reviewCount, reviewDtos);
        return ResponseEntity.ok(Apiresponse.isSuccess(SuccessStatus.OK, result));
    }

    @Transactional
    public ResponseEntity<?> createReview(String email, ReviewDto.newReviewDto newReviewDto){

        Optional<User> userOptional = userRepository.findByEmail(email);

        if(userOptional.isEmpty()){
            return ResponseEntity.ok(Apiresponse.isFailed(ErrorStatus.UNAUTHORIZED_ERROR));
        }

        Optional<Restaurant> restaurantOptional = restaurantRepository.findById(newReviewDto.getRid());

        if(restaurantOptional.isEmpty()){
            return ResponseEntity.ok(Apiresponse.isFailed(ErrorStatus.RESTAURANT_NOT_FOUND));
        }

        User user = userOptional.get();
        Restaurant restaurant = restaurantOptional.get();

        Review review = new Review();

        review.setUser(user);
        review.setStar(newReviewDto.getStar());
        review.setRestaurant(restaurant);
        review.setContent(newReviewDto.getContent());
        review.setCreatedDate(LocalDate.now());

        reviewRepository.save(review);

        for (String imageUrl : newReviewDto.getImageUrls()) {
            ReviewImage reviewImage = new ReviewImage();

            reviewImage.setReview(review);
            reviewImage.setImageUrl(imageUrl);

            reviewImageRepository.save(reviewImage);
        }

        TodayReview todayReview = new TodayReview();

        todayReview.setUser(user);
        todayReview.setRestaurant(restaurant);

        todayReviewRepository.save(todayReview);

    return ResponseEntity.ok(Apiresponse.isSuccess(SuccessStatus.CREATED));
    }

    @Transactional
    public ResponseEntity<?> deleteReview(ReviewDto.deleteReviewDto deleteReviewDto){
        Optional<Review> reviewOptional = reviewRepository.findById(deleteReviewDto.getReviewId());

        if(reviewOptional.isEmpty()){
            return ResponseEntity.ok(Apiresponse.isFailed(ErrorStatus.REVIEW_NOT_AVAILABLE));
        }

        Review review = reviewOptional.get();

        User user = review.getUser();
        Restaurant restaurant = review.getRestaurant();

        Optional<TodayReview> todayReviewOptional = todayReviewRepository.findByUserAndRestaurant(user, restaurant);

        todayReviewOptional.ifPresent(todayReviewRepository::delete); // 리뷰 작성 여부 초기화.

        List<ReviewImage> reviewImages = reviewImageRepository.findAllByReviewId(review.getId());

        reviewImageRepository.deleteAll(reviewImages);
        reviewRepository.delete(review);

        return ResponseEntity.ok(Apiresponse.isSuccess(SuccessStatus.OK));
    }

    @Transactional
    public ResponseEntity<?> checkAvailable(String email, Long rid){

        Optional<User> userOptional = userRepository.findByEmail(email);
        Optional<Restaurant> restaurantOptional = restaurantRepository.findById(rid);

        if(userOptional.isEmpty()){
            return ResponseEntity.ok(Apiresponse.isFailed(ErrorStatus.UNKNOWN_USER_ERROR));
        }

        if(restaurantOptional.isEmpty()){
            return ResponseEntity.ok(Apiresponse.isFailed(ErrorStatus.RESTAURANT_NOT_FOUND));
        }

        User user = userOptional.get();
        Restaurant restaurant = restaurantOptional.get();

        Optional<TodayReview> todayReviewOptional = todayReviewRepository.findByUserAndRestaurant(user, restaurant);

        if(todayReviewOptional.isEmpty()){
            return ResponseEntity.ok(Apiresponse.isSuccess(SuccessStatus.AVAILABLE));
        }

        return ResponseEntity.ok(Apiresponse.isFailed(ErrorStatus.ALREADY_REVIEWED));
    }
}
