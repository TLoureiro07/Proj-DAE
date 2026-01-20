package pt.ipleria.estg.dei.ei.dae.academics.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("Collaborator")
public class Collaborator extends User {
    public Collaborator() {
        super();
    }

    public Collaborator(String username, String password, String name, String email) {
        super(username, password, name, email);
    }
}
