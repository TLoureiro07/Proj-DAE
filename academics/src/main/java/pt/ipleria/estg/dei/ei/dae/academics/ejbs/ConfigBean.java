package pt.ipleria.estg.dei.ei.dae.academics.ejbs;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

@Startup
@Singleton
public class ConfigBean {

    @PostConstruct
    public void populateDB() {
        System.out.println("Hello Java EE!");
        // TODO: Adicionar população inicial de dados do projeto (Users, Tags, etc.)
    }
}
