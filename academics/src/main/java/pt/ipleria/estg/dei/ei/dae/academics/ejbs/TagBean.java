package pt.ipleria.estg.dei.ei.dae.academics.ejbs;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Tag;

import java.util.List;

@Stateless
public class TagBean {

    @PersistenceContext
    private EntityManager em;

    public Tag create(String name) {
        if (name == null || name.trim().isEmpty()) return null;

        // Verificar se já existe
        Tag existing = findByName(name.trim());
        if (existing != null) return null;

        Tag tag = new Tag(name.trim());
        em.persist(tag);
        return tag;
    }

    public Tag find(Long id) {
        return em.find(Tag.class, id);
    }

    public Tag findByName(String name) {
        List<Tag> tags = em.createQuery(
            "SELECT t FROM Tag t WHERE LOWER(t.name) = LOWER(:name)",
            Tag.class)
            .setParameter("name", name)
            .getResultList();
        
        return tags.isEmpty() ? null : tags.get(0);
    }

    public List<Tag> findAll() {
        return em.createQuery("SELECT t FROM Tag t ORDER BY t.name", Tag.class)
            .getResultList();
    }

    public void delete(Long id) {
        Tag tag = find(id);
        if (tag != null) {
            em.remove(tag);
        }
    }
}

