package hongmumuk.hongmumuk.entity;

import hongmumuk.hongmumuk.dto.AdminDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.bind.DefaultValue;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // 진짜 레스토랑 이름
    private String restaurantName = "res_1";
    // iOS키 값
    private String name;
    private String address;
    private Integer likes;
    private Double longitude;
    private Double latitude;
    @Enumerated(EnumType.STRING)
    private Category category;
    private Double front;
    private Double back;
    private String naverLink;
    private String kakaoLink;
    private String fileName;
    private String imageUrl;


    public void setLikes(int i) {
        this.likes = i;
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setLikes(Integer likes) {
        this.likes = likes;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setFront(Double front) {
        this.front = front;
    }

    public void setBack(Double back) {
        this.back = back;
    }

    public void setNaverLink(String naverLink) {
        this.naverLink = naverLink;
    }

    public void setKakaoLink(String kakaoLink) {
        this.kakaoLink = kakaoLink;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    // Dto -> Entity 변환
    public static Restaurant toEntity(AdminDto.addNewOneDto addNewOneDto){
        return Restaurant.builder()
                .name(addNewOneDto.getName())
                .address(addNewOneDto.getAddress())
                .longitude(addNewOneDto.getLongitude())
                .latitude(addNewOneDto.getLatitude())
                .category(addNewOneDto.getCategory())
                .front(addNewOneDto.getFront())
                .back(addNewOneDto.getBack())
                .naverLink(addNewOneDto.getNaverLink())
                .kakaoLink(addNewOneDto.getKakaoLink())
                .build();
    }
}
