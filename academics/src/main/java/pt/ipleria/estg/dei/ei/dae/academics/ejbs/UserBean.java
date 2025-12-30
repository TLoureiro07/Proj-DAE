package pt.ipleria.estg.dei.ei.dae.academics.ejbs;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

        User user = new User(
                username,
                Hasher.hash(password),
                name,
                email,
                role
        );

        em.persist(user);
        return user;
    }

    public void changeRole(String username, String role) {
        User user = find(username);
        if (user == null) return;
        user.setRole(role);
        em.merge(user);
    }

    public void setActive(String username, boolean active) {
        User user = find(username);
        if (user == null) return;
        user.setActive(active);
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
        return user.getSubscribedTags();
    }
}
