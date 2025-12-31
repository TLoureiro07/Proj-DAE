package pt.ipleria.estg.dei.ei.dae.academics.ejbs;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Comment;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Publication;
import pt.ipleria.estg.dei.ei.dae.academics.entities.User;

import java.util.List;

@Stateless
public class CommentBean {

    @PersistenceContext
    private EntityManager em;

    @EJB
    private UserBean userBean;

    @EJB
    private PublicationBean publicationBean;

    @EJB
    private UserActivityBean userActivityBean;

    public Comment create(Long publicationId, String authorUsername, String text) {
        Publication publication = publicationBean.find(publicationId);
        if (publication == null) return null;

        User author = userBean.find(authorUsername);
        if (author == null) return null;

        Comment comment = new Comment(text, author, publication);
        em.persist(comment);
        em.flush();

        // Registar atividade
        userActivityBean.recordActivity(authorUsername, publicationId, "COMMENT", 
            "Comentou na publicação: " + publication.getTitle());

        return comment;
    }

    public Comment find(Long id) {
        return em.find(Comment.class, id);
    }

    public Comment findWithRelations(Long id) {
        List<Comment> comments = em.createQuery(
            "SELECT c FROM Comment c LEFT JOIN FETCH c.author LEFT JOIN FETCH c.publication WHERE c.id = :id",
            Comment.class)
            .setParameter("id", id)
            .getResultList();
        return comments.isEmpty() ? null : comments.get(0);
    }

    public List<Comment> findByPublication(Long publicationId, boolean includeHidden) {
        if (includeHidden) {
            return em.createQuery(
                "SELECT c FROM Comment c LEFT JOIN FETCH c.author LEFT JOIN FETCH c.publication WHERE c.publication.id = :publicationId ORDER BY c.commentDate DESC",
                Comment.class)
                .setParameter("publicationId", publicationId)
                .getResultList();
        } else {
            return em.createQuery(
                "SELECT c FROM Comment c LEFT JOIN FETCH c.author LEFT JOIN FETCH c.publication WHERE c.publication.id = :publicationId AND c.hidden = false ORDER BY c.commentDate DESC",
                Comment.class)
                .setParameter("publicationId", publicationId)
                .getResultList();
        }
    }

    public void setHidden(Long commentId, boolean hidden) {
        Comment comment = find(commentId);
        if (comment != null) {
            comment.setHidden(hidden);
            em.merge(comment);
        }
    }

    public void delete(Long commentId) {
        Comment comment = find(commentId);
        if (comment != null) {
            em.remove(comment);
        }
    }
}

