package pt.ipleria.estg.dei.ei.dae.academics.ejbs;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Administrator;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Collaborator;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Responsible;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Tag;
import pt.ipleria.estg.dei.ei.dae.academics.entities.User;
import pt.ipleria.estg.dei.ei.dae.academics.ejbs.TagBean;
import pt.ipleria.estg.dei.ei.dae.academics.security.Hasher;

import java.util.List;

@Stateless
public class UserBean {

    @PersistenceContext
    private EntityManager em;

    @EJB
    private TagBean tagBean;

    public User find(String username) {
        return em.find(User.class, username);
    }

    public List<User> findAll() {
        return em.createQuery("SELECT u FROM User u", User.class)
                .getResultList();
    }

    public User create(String username, String password, String name, String email, String role) {
        if (find(username) != null) return null;

        User user;
        switch (role) {
            case "Administrator":
                user = new Administrator(username, Hasher.hash(password), name, email);
                break;
            case "Responsible":
                user = new Responsible(username, Hasher.hash(password), name, email);
                break;
            case "Collaborator":
                user = new Collaborator(username, Hasher.hash(password), name, email);
                break;
            default:
                return null;
        }

        em.persist(user);
        return user;
    }

    public void changeRole(String username, String newRole) {
        User user = find(username);
        if (user == null) return;

        // Se já tem o role correto, não fazer nada
        String currentRole = org.hibernate.Hibernate.getClass(user).getSimpleName();
        if (currentRole.equals(newRole)) return;

        // Criar nova instância com o role correto
        User newUser;
        switch (newRole) {
            case "Administrator":
                newUser = new Administrator(user.getUsername(), user.getPassword(),
                        user.getName(), user.getEmail());
                break;
            case "Responsible":
                newUser = new Responsible(user.getUsername(), user.getPassword(),
                        user.getName(), user.getEmail());
                break;
            case "Collaborator":
                newUser = new Collaborator(user.getUsername(), user.getPassword(),
                        user.getName(), user.getEmail());
                break;
            default:
                return;
        }

        // Copiar estado adicional
        newUser.setActive(user.isActive());
        // subscribedTags será mantido pela relação ManyToMany

        // Remover antigo e persistir novo
        em.remove(user);
        em.flush(); // Garantir que remove antes de criar
        em.persist(newUser);
    }

    public void setActive(String username, boolean active) {
        User user = find(username);
        if (user == null) return;
        user.setActive(active);
        em.merge(user);
    }

    public void update(User user) {
        em.merge(user);
    }

    public boolean canLogin(String username, String password) {
        User user = find(username);
        if (user == null) return false;
        if (!user.isActive()) return false;
        return Hasher.verify(password, user.getPassword());
    }

    public void changePassword(String username, String newPassword) {
        User user = find(username);
        if (user == null) return;
        user.setPassword(Hasher.hash(newPassword));
        em.merge(user);
    }

    public void subscribeToTag(String username, Long tagId) {
        User user = find(username);
        Tag tag = tagBean.find(tagId);
        if (user != null && tag != null) {
            user.addSubscribedTag(tag);
            em.merge(user);
        }
    }

    public void unsubscribeFromTag(String username, Long tagId) {
        User user = find(username);
        Tag tag = tagBean.find(tagId);
        if (user != null && tag != null) {
            user.removeSubscribedTag(tag);
            em.merge(user);
        }
    }

    public List<Tag> getSubscribedTags(String username) {
        User user = find(username);
        if (user == null) return List.of();
        // Inicializar relação lazy antes de retornar
        Hibernate.initialize(user.getSubscribedTags());
        return user.getSubscribedTags();
    }

    public boolean delete(String username) {
        User user = find(username);
        if (user == null) return false;
        
        // Verificar se o utilizador tem publicações, comentários, ratings ou atividades
        // Se tiver, não podemos eliminar devido a constraints de foreign key
        long publicationsCount = em.createQuery(
            "SELECT COUNT(p) FROM Publication p WHERE p.owner.username = :username",
            Long.class)
            .setParameter("username", username)
            .getSingleResult();
        
        if (publicationsCount > 0) {
            return false; // Não pode eliminar utilizador com publicações
        }
        
        // Remover todas as subscrições de tags antes de remover o utilizador
        // (a relação ManyToMany será gerida automaticamente pelo JPA)
        user.getSubscribedTags().clear();
        
        // Eliminar comentários e ratings do utilizador (se existirem)
        em.createQuery("DELETE FROM Comment c WHERE c.author.username = :username")
            .setParameter("username", username)
            .executeUpdate();
        
        em.createQuery("DELETE FROM Rating r WHERE r.author.username = :username")
            .setParameter("username", username)
            .executeUpdate();
        
        // Eliminar atividades do utilizador
        em.createQuery("DELETE FROM UserActivity ua WHERE ua.user.username = :username")
            .setParameter("username", username)
            .executeUpdate();
        
        em.remove(user);
        return true;
    }
}
