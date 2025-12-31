package pt.ipleria.estg.dei.ei.dae.academics.ejbs;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Publication;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Tag;
import pt.ipleria.estg.dei.ei.dae.academics.entities.User;

import java.util.Arrays;

@Startup
@Singleton
public class ConfigBean {

    @EJB
    private UserBean userBean;

    @EJB
    private PublicationBean publicationBean;

    @EJB
    private TagBean tagBean;

    @PostConstruct
    public void populateDB() {
        System.out.println("Hello Java EE!");
        
        // Criar utilizadores iniciais para testes apenas se não existirem
        // Password para todos: "pass"
        User admin = null;
        if (userBean.find("admin") == null) {
            admin = userBean.create("admin", "pass", "Administrator", "admin@example.com", "Administrator");
            System.out.println("Created user: admin");
        } else {
            admin = userBean.find("admin");
            System.out.println("User admin already exists");
        }
        
        if (userBean.find("responsible") == null) {
            userBean.create("responsible", "pass", "Responsible User", "responsible@example.com", "Responsible");
            System.out.println("Created user: responsible");
        } else {
            System.out.println("User responsible already exists");
        }
        
        User collaborator = null;
        if (userBean.find("collaborator") == null) {
            collaborator = userBean.create("collaborator", "pass", "Collaborator User", "collaborator@example.com", "Collaborator");
            System.out.println("Created user: collaborator");
        } else {
            collaborator = userBean.find("collaborator");
            System.out.println("User collaborator already exists");
        }

        // Criar tags de teste
        Tag tagProjetoX = tagBean.findByName("Projeto X");
        if (tagProjetoX == null) {
            tagProjetoX = tagBean.create("Projeto X");
            System.out.println("Created tag: Projeto X");
        }

        Tag tagProjetoY = tagBean.findByName("Projeto Y");
        if (tagProjetoY == null) {
            tagProjetoY = tagBean.create("Projeto Y");
            System.out.println("Created tag: Projeto Y");
        }

        // Criar publicação de teste (comentado para evitar problemas durante @PostConstruct)
        // As publicações podem ser criadas via API depois do deployment
        /*
        if (collaborator != null && publicationBean.findByOwner("collaborator").size() == 0) {
            try {
                Publication testPub = new Publication();
                testPub.setTitle("Deep Learning Applications in Medical Imaging");
                testPub.setAuthors(Arrays.asList("João Silva", "Maria Santos", "Pedro Costa"));
                testPub.setScientificArea("Ciência de Dados");
                testPub.setSummary("Este artigo explora as aplicações de deep learning no processamento de imagens médicas, apresentando técnicas avançadas de redes neurais convolucionais para diagnóstico assistido por computador.");
                testPub.setVisibility("public");
                
                Publication created = publicationBean.create("collaborator", testPub);
                if (created != null && tagProjetoX != null) {
                    publicationBean.addTag(created.getId(), tagProjetoX.getId());
                    System.out.println("Created test publication: " + created.getTitle());
                }
            } catch (Exception e) {
                System.out.println("Warning: Could not create test publication: " + e.getMessage());
            }
        }
        */
    }
}
