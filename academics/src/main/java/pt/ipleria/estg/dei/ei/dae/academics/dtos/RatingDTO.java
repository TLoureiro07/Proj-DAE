package pt.ipleria.estg.dei.ei.dae.academics.dtos;

import pt.ipleria.estg.dei.ei.dae.academics.entities.Rating;
import pt.ipleria.estg.dei.ei.dae.academics.entities.User;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

public class RatingDTO implements Serializable {
    public Long id;
    public Integer value;
    public String authorUsername;
    public String authorName;
    public Long publicationId;
    public String ratingDate;

    public RatingDTO() {}

    public RatingDTO(Long id, Integer value, String authorUsername, String authorName, 
                    Long publicationId, String ratingDate) {
        this.id = id;
        this.value = value;
        this.authorUsername = authorUsername;
        this.authorName = authorName;
        this.publicationId = publicationId;
        this.ratingDate = ratingDate;
    }

    public static RatingDTO from(Rating rating) {
        User author = rating.getAuthor();
        return new RatingDTO(
            rating.getId(),
            rating.getValue(),
            author != null ? author.getUsername() : null,
            author != null ? author.getName() : null,
            rating.getPublication() != null ? rating.getPublication().getId() : null,
            rating.getRatingDate() != null ? rating.getRatingDate().toString() : null
        );
    }

    public static List<RatingDTO> from(List<Rating> ratings) {
        return ratings.stream().map(RatingDTO::from).collect(Collectors.toList());
    }
}

