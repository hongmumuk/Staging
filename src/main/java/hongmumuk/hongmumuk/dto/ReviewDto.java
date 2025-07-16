package hongmumuk.hongmumuk.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ReviewDto {
    private String name;
    private Integer star;
    private String content;
    private LocalDate createdDate;
    private Long rank;
    private List<String> imageUrls;

    public ReviewDto(String name, int star, String content, LocalDate createdDate, long rank, List<String> imageUrls) {
        this.name = name;
        this.star = star;
        this.content = content;
        this.createdDate = createdDate;
        this.rank = rank;
        this.imageUrls = imageUrls;
    }

}
