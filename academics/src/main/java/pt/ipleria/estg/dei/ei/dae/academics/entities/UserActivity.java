package pt.ipleria.estg.dei.ei.dae.academics.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_activities")
public class UserActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_username", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publication_id")
    private Publication publication;

    @Column(nullable = false)
    private String activityType; // "UPLOAD", "EDIT", "COMMENT", "RATING", "TAG_SUBSCRIPTION", etc.

    private String description; // Descrição opcional da atividade

    @Column(nullable = false)
    private LocalDateTime activityDate;

    public UserActivity() {}

    public UserActivity(User user, Publication publication, String activityType, String description) {
        this.user = user;
        this.publication = publication;
        this.activityType = activityType;
        this.description = description;
        this.activityDate = LocalDateTime.now();
    }

    // getters & setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Publication getPublication() {
        return publication;
    }

    public void setPublication(Publication publication) {
        this.publication = publication;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getActivityDate() {
        return activityDate;
    }

    public void setActivityDate(LocalDateTime activityDate) {
        this.activityDate = activityDate;
    }
}

