package pt.ipleria.estg.dei.ei.dae.academics.dtos;

import java.io.Serializable;

public class LoginDTO implements Serializable {
    public String username;
    public String password;

    public LoginDTO() {}

    public LoginDTO(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
