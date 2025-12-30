package pt.ipleria.estg.dei.ei.dae.academics.ejbs;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import pt.ipleria.estg.dei.ei.dae.academics.entities.User;
import pt.ipleria.estg.dei.ei.dae.academics.entities.UserActivity;

import java.util.List;

@Stateless
public class UserActivityBean {

    @PersistenceContext
    private EntityManager em;

    public List<UserActivity> findByUser(String username) {
        User user = em.find(User.class, username);
        if (user == null) return List.of();
        return em.createQuery(
            "SELECT a FROM UserActivity a WHERE a.user = :user ORDER BY a.activityDate DESC",
            UserActivity.class
        )
        .setParameter("user", user)
        .getResultList();
    }

    public void recordActivity(User user, pt.ipleria.estg.dei.ei.dae.academics.entities.Publication publication, 
                              String activityType, String description) {
        UserActivity activity = new UserActivity(user, publication, activityType, description);
        em.persist(activity);
    }
}

