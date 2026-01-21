package pt.ipleria.estg.dei.ei.dae.academics.dtos;

import org.hibernate.Hibernate;
import pt.ipleria.estg.dei.ei.dae.academics.entities.User;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

public class UserDTO implements Serializable {
    public String username;
    public String name;
    public String email;
    public String role;
    public boolean active;

    public UserDTO() {}

    public UserDTO(String username, String name, String email, String role, boolean active) {
        this.username = username;
        this.name = name;
        this.email = email;
        this.role = role;
        this.active = active;
    }

    public static UserDTO from(User u) {
        return new UserDTO(
            u.getUsername(),
            u.getName(),
            u.getEmail(),
            Hibernate.getClass(u).getSimpleName(),
            u.isActive()
        );
    }

    public static List<UserDTO> from(List<User> users) {
        return users.stream().map(UserDTO::from).collect(Collectors.toList());
    }
}
