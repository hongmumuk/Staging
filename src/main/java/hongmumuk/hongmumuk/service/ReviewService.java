package hongmumuk.hongmumuk.service;

import hongmumuk.hongmumuk.common.response.Apiresponse;
import hongmumuk.hongmumuk.common.response.status.SuccessStatus;
import hongmumuk.hongmumuk.dto.PageDto;
import hongmumuk.hongmumuk.dto.ReviewDto;
import hongmumuk.hongmumuk.dto.ReviewPageDto;
import hongmumuk.hongmumuk.entity.Review;
import hongmumuk.hongmumuk.entity.ReviewImage;
import hongmumuk.hongmumuk.entity.User;
import hongmumuk.hongmumuk.repository.ReviewImageRepository;
import hongmumuk.hongmumuk.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;

    @Transactional
    public ResponseEntity<?> allReviews(long restaurantId, PageDto pageDto) {
        long reviewCount = reviewRepository.countByRestaurantId(restaurantId);

        int page = pageDto.getPage();
        String sort = pageDto.getSort();

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
}
