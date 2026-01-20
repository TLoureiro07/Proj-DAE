package pt.ipleria.estg.dei.ei.dae.academics.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("Responsible")
public class Responsible extends User {
    public Responsible() {
        super();
    }

    public Responsible(String username, String password, String name, String email) {
        super(username, password, name, email);
    }
}
