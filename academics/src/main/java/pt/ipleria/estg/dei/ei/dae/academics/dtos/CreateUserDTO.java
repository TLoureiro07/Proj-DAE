package pt.ipleria.estg.dei.ei.dae.academics.dtos;

import java.io.Serializable;

public class CreateUserDTO implements Serializable {
    public String username;
    public String password;
    public String name;
    public String email;
    public String role;

    public CreateUserDTO() {}
}
