package pt.ipleria.estg.dei.ei.dae.academics.dtos;

import pt.ipleria.estg.dei.ei.dae.academics.entities.User;

public class UserDTO {
    public String username;
    public String name;
    public String email;
    public String role;

    public static UserDTO from(User u) {
        UserDTO dto = new UserDTO();
        dto.username = u.getUsername();
        dto.name = u.getName();
        dto.email = u.getEmail();
        dto.role = u.getRole();
        return dto;
    }
}
