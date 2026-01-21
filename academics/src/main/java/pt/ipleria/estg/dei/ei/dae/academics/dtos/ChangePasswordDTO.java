package pt.ipleria.estg.dei.ei.dae.academics.dtos;

import java.io.Serializable;

public class ChangePasswordDTO implements Serializable {
    public String old_password;
    public String new_password;
    public String confirm_password;

    public ChangePasswordDTO() {}
}
