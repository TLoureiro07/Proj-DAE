package pt.ipleria.estg.dei.ei.dae.academics.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tags")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String name;

    @ManyToMany(mappedBy = "tags")
    private List<Publication> publications = new ArrayList<>();

    @ManyToMany
    @JoinTable(
            name = "user_tag_subscription",
            joinColumns = @JoinColumn(name = "tag_id"),
            inverseJoinColumns = @JoinColumn(name = "user_username")
    )
    private List<User> subscribedUsers = new ArrayList<>();

    public Tag() {}

    public Tag(String name) {
        this.name = name;
    }

    // getters & setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Publication> getPublications() {
        return publications;
    }

    public void setPublications(List<Publication> publications) {
        this.publications = publications;
    }

    public void addPublication(Publication publication) {
        if (!publications.contains(publication)) {
            publications.add(publication);
        }
    }

    public void removePublication(Publication publication) {
        publications.remove(publication);
    }

    public List<User> getSubscribedUsers() {
        return subscribedUsers;
    }

    public void setSubscribedUsers(List<User> subscribedUsers) {
        this.subscribedUsers = subscribedUsers;
    }

    public void addSubscribedUser(User user) {
        if (!subscribedUsers.contains(user)) {
            subscribedUsers.add(user);
        }
    }

    public void removeSubscribedUser(User user) {
        subscribedUsers.remove(user);
    }
}

