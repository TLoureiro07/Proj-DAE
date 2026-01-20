package pt.ipleria.estg.dei.ei.dae.academics.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("Administrator")
public class Administrator extends User {
    public Administrator() {
        super();
    }

    public Administrator(String username, String password, String name, String email) {
        super(username, password, name, email);
    }
}
