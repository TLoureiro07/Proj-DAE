package pt.ipleria.estg.dei.ei.dae.academics.ejbs;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import pt.ipleria.estg.dei.ei.dae.academics.entities.User;

@Startup
@Singleton
public class ConfigBean {

    @EJB
    private UserBean userBean;

    @PostConstruct
    public void populateDB() {
        System.out.println("Hello Java EE!");
        
        // Criar utilizadores iniciais para testes
        // Password para todos: "pass"
        if (userBean.find("admin") == null) {
            userBean.create("admin", "pass", "Administrator", "admin@example.com", "Administrator");
            System.out.println("Created user: admin");
        }
        
        if (userBean.find("responsible") == null) {
            userBean.create("responsible", "pass", "Responsible User", "responsible@example.com", "Responsible");
            System.out.println("Created user: responsible");
        }
        
        if (userBean.find("collaborator") == null) {
            userBean.create("collaborator", "pass", "Collaborator User", "collaborator@example.com", "Collaborator");
            System.out.println("Created user: collaborator");
        }
    }
}
