package pt.ipleria.estg.dei.ei.dae.academics.ejbs;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

@Startup
@Singleton
public class ConfigBean {

    @EJB
    private StudentBean studentBean;

    @PostConstruct
    public void populateDB() {
        System.out.println("Hello Java EE!");
        studentBean.create("john123", "pass123", "John Doe", "john@example.com");
        studentBean.create("maria456", "pass456", "Maria Silva", "maria@example.com");
    }
}
