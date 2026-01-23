package pt.ipleria.estg.dei.ei.dae.academics.ejbs;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Administrator;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Collaborator;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Comment;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Publication;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Rating;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Responsible;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Tag;
import pt.ipleria.estg.dei.ei.dae.academics.entities.User;
import pt.ipleria.estg.dei.ei.dae.academics.entities.UserActivity;
import pt.ipleria.estg.dei.ei.dae.academics.ejbs.TagBean;
import pt.ipleria.estg.dei.ei.dae.academics.security.Hasher;

import java.util.List;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

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

    public boolean subscribeToTag(String username, Long tagId) {
        User user = find(username);
        Tag tag = tagBean.find(tagId);
        if (user != null && tag != null) {
            if (!user.getSubscribedTags().contains(tag)) {
            user.addSubscribedTag(tag);
            em.merge(user);
                return true;
        }
        }
        return false;
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
        Hibernate.initialize(user.getSubscribedTags());
        return user.getSubscribedTags();
    }

    public User findByEmail(String email) {
        try {
            return em.createQuery(
                            "SELECT u FROM User u WHERE u.email = :email",
                            User.class
                    ).setParameter("email", email)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public void delete(String username) {
        User user = find(username);
        if (user == null) return;

        // Verificar se o utilizador tem publicações
        List<Publication> userPublications = em.createQuery(
            "SELECT p FROM Publication p WHERE p.owner.username = :username",
            Publication.class)
            .setParameter("username", username)
            .getResultList();

        if (!userPublications.isEmpty()) {
            throw new IllegalStateException("Não é possível remover o utilizador porque tem publicações associadas.");
        }

        // Remover todas as subscrições de tags antes de remover o utilizador
        List<Tag> subscribedTags = user.getSubscribedTags();
        for (Tag tag : List.copyOf(subscribedTags)) { // Usar cópia para evitar ConcurrentModificationException
            user.removeSubscribedTag(tag);
        }
        em.flush(); // Sincronizar remoção das relações ManyToMany

        // Remover comentários e ratings do utilizador
        em.createQuery("DELETE FROM Comment c WHERE c.author.username = :username")
                .setParameter("username", username)
                .executeUpdate();
        em.createQuery("DELETE FROM Rating r WHERE r.author.username = :username")
                .setParameter("username", username)
                .executeUpdate();
        em.createQuery("DELETE FROM UserActivity ua WHERE ua.user.username = :username")
                .setParameter("username", username)
                .executeUpdate();
        em.flush(); // Sincronizar remoção das relações OneToMany

        em.remove(user);
    }

    public int importFromCSV(InputStream inputStream) throws Exception {

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        );

        String line;
        int count = 0;
        boolean firstLine = true;

        while ((line = reader.readLine()) != null) {

            if (firstLine) {
                firstLine = false;
                continue;
            }

            if (line.trim().isEmpty()) continue;

            String[] parts = line.split(";", -1);

            if (parts.length < 5) continue;

            String username = parts[0].trim();
            String password = parts[1].trim();
            String name     = parts[2].trim();
            String email    = parts[3].trim();
            String role = parts[4].trim().replace("\r", "");

            if (find(username) != null) continue;

            User created = create(username, password, name, email, role);

            if (created != null) {
                count++;
            }
        }

        return count;
    }

    public void delete(String username) {
        User user = find(username);
        if (user == null) return;

        // Verificar se o utilizador tem publicações
        List<Publication> userPublications = em.createQuery(
            "SELECT p FROM Publication p WHERE p.owner.username = :username",
            Publication.class)
            .setParameter("username", username)
            .getResultList();

        if (!userPublications.isEmpty()) {
            throw new IllegalStateException("Não é possível remover o utilizador porque tem publicações associadas.");
        }

        // Remover todas as subscrições de tags antes de remover o utilizador
        List<Tag> subscribedTags = user.getSubscribedTags();
        for (Tag tag : List.copyOf(subscribedTags)) { // Usar cópia para evitar ConcurrentModificationException
            user.removeSubscribedTag(tag);
        }
        em.flush(); // Sincronizar remoção das relações ManyToMany

        // Remover comentários e ratings do utilizador
        em.createQuery("DELETE FROM Comment c WHERE c.author.username = :username")
                .setParameter("username", username)
                .executeUpdate();
        em.createQuery("DELETE FROM Rating r WHERE r.author.username = :username")
                .setParameter("username", username)
                .executeUpdate();
        em.createQuery("DELETE FROM UserActivity ua WHERE ua.user.username = :username")
                .setParameter("username", username)
                .executeUpdate();
        em.flush(); // Sincronizar remoção das relações OneToMany

        em.remove(user);
    }
}
