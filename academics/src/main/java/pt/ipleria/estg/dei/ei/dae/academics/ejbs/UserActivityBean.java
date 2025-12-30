package pt.ipleria.estg.dei.ei.dae.academics.ejbs;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import pt.ipleria.estg.dei.ei.dae.academics.entities.User;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Publication;
import pt.ipleria.estg.dei.ei.dae.academics.entities.UserActivity;

import java.util.List;

@Stateless
public class UserActivityBean {

    @PersistenceContext
    private EntityManager em;

    @EJB
    private UserBean userBean;

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

    // Método sobrecarregado para aceitar String username e Long publicationId
    public void recordActivity(String username, Long publicationId, String activityType, String description) {
        User user = userBean.find(username);
        if (user == null) return; // Should not happen if authentication is working

        Publication publication = null;
        if (publicationId != null) {
            publication = em.find(Publication.class, publicationId);
        }

        UserActivity activity = new UserActivity(user, publication, activityType, description);
        em.persist(activity);
    }

    // Método original mantido para compatibilidade
    public void recordActivity(User user, Publication publication, 
                              String activityType, String description) {
        UserActivity activity = new UserActivity(user, publication, activityType, description);
        em.persist(activity);
    }
}

