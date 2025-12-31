package pt.ipleria.estg.dei.ei.dae.academics.ejbs;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Publication;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Rating;
import pt.ipleria.estg.dei.ei.dae.academics.entities.User;

import java.util.List;

@Stateless
public class RatingBean {

    @PersistenceContext
    private EntityManager em;

    @EJB
    private UserBean userBean;

    @EJB
    private PublicationBean publicationBean;

    @EJB
    private UserActivityBean userActivityBean;

    public Rating createOrUpdate(Long publicationId, String authorUsername, Integer value) {
        if (value < 1 || value > 5) return null;

        Publication publication = publicationBean.find(publicationId);
        if (publication == null) return null;

        User author = userBean.find(authorUsername);
        if (author == null) return null;

        // Verificar se já existe rating deste utilizador para esta publicação
        Rating existingRating = findByUserAndPublication(publicationId, authorUsername);
        
        if (existingRating != null) {
            // Atualizar rating existente
            existingRating.setValue(value);
            existingRating.setRatingDate(java.time.LocalDateTime.now());
            em.merge(existingRating);
            
            // Atualizar média
            updateRatingAvg(publicationId);
            
            // Registar atividade
            userActivityBean.recordActivity(authorUsername, publicationId, "RATING_UPDATE", 
                "Atualizou rating da publicação: " + publication.getTitle());
            
            return existingRating;
        } else {
            // Criar novo rating
            Rating rating = new Rating(value, author, publication);
            em.persist(rating);
            em.flush();
            
            // Atualizar média
            updateRatingAvg(publicationId);
            
            // Registar atividade
            userActivityBean.recordActivity(authorUsername, publicationId, "RATING", 
                "Avaliou a publicação: " + publication.getTitle());
            
            return rating;
        }
    }

    public Rating find(Long id) {
        return em.find(Rating.class, id);
    }

    public Rating findWithRelations(Long id) {
        List<Rating> ratings = em.createQuery(
            "SELECT r FROM Rating r LEFT JOIN FETCH r.author LEFT JOIN FETCH r.publication WHERE r.id = :id",
            Rating.class)
            .setParameter("id", id)
            .getResultList();
        return ratings.isEmpty() ? null : ratings.get(0);
    }

    public Rating findByUserAndPublication(Long publicationId, String username) {
        List<Rating> ratings = em.createQuery(
            "SELECT r FROM Rating r WHERE r.publication.id = :publicationId AND r.author.username = :username",
            Rating.class)
            .setParameter("publicationId", publicationId)
            .setParameter("username", username)
            .getResultList();
        
        return ratings.isEmpty() ? null : ratings.get(0);
    }

    public List<Rating> findByPublication(Long publicationId) {
        return em.createQuery(
            "SELECT r FROM Rating r LEFT JOIN FETCH r.author LEFT JOIN FETCH r.publication WHERE r.publication.id = :publicationId ORDER BY r.ratingDate DESC",
            Rating.class)
            .setParameter("publicationId", publicationId)
            .getResultList();
    }

    public void delete(Long ratingId) {
        Rating rating = find(ratingId);
        if (rating != null) {
            Long publicationId = rating.getPublication().getId();
            em.remove(rating);
            updateRatingAvg(publicationId);
        }
    }

    private void updateRatingAvg(Long publicationId) {
        Publication publication = publicationBean.find(publicationId);
        if (publication == null) return;

        List<Rating> ratings = findByPublication(publicationId);
        if (ratings.isEmpty()) {
            publication.setRatingAvg(null);
        } else {
            double sum = ratings.stream().mapToInt(Rating::getValue).sum();
            double avg = sum / ratings.size();
            publication.setRatingAvg(avg);
        }
        em.merge(publication);
    }
}

