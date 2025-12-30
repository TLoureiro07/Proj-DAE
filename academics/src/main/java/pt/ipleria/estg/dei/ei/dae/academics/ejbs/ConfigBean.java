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
        
        // Criar ou atualizar utilizadores iniciais para testes
        // Password para todos: "pass"
        User admin = userBean.find("admin");
        if (admin == null) {
            admin = userBean.create("admin", "pass", "Administrator", "admin@example.com", "Administrator");
            System.out.println("Created user: admin");
        } else {
            // Garantir que está ativo e tem a password correta
            userBean.setActive("admin", true);
            userBean.changePassword("admin", "pass");
            System.out.println("Updated user: admin");
        }
        
        User responsible = userBean.find("responsible");
        if (responsible == null) {
            userBean.create("responsible", "pass", "Responsible User", "responsible@example.com", "Responsible");
            System.out.println("Created user: responsible");
        } else {
            // Garantir que está ativo e tem a password correta
            userBean.setActive("responsible", true);
            userBean.changePassword("responsible", "pass");
            System.out.println("Updated user: responsible");
        }
        
        User collaborator = userBean.find("collaborator");
        if (collaborator == null) {
            collaborator = userBean.create("collaborator", "pass", "Collaborator User", "collaborator@example.com", "Collaborator");
            System.out.println("Created user: collaborator");
        } else {
            // Garantir que está ativo e tem a password correta
            userBean.setActive("collaborator", true);
            userBean.changePassword("collaborator", "pass");
            System.out.println("Updated user: collaborator");
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

        // Criar publicação de teste
        if (collaborator != null && publicationBean.findByOwner("collaborator").size() == 0) {
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
        }
    }
}
