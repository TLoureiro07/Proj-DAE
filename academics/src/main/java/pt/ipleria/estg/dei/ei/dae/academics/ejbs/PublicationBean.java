package pt.ipleria.estg.dei.ei.dae.academics.ejbs;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Publication;
import pt.ipleria.estg.dei.ei.dae.academics.entities.PublicationHistory;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Tag;
import pt.ipleria.estg.dei.ei.dae.academics.entities.User;
import pt.ipleria.estg.dei.ei.dae.academics.ejbs.TagBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Rating;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;


@Stateless
public class PublicationBean {
    private static final String UPLOAD_DIR = "/tmp/uploads";

    @PersistenceContext
    private EntityManager em;

    @EJB
    private UserBean userBean;

    @EJB
    private TagBean tagBean;

    public Publication create(String ownerUsername, Publication p) {
        User owner = userBean.find(ownerUsername);
        if (owner == null) return null;
        p.setOwner(owner);
        p.setUploadDate(LocalDate.now());
        p.setLastEdited(LocalDateTime.now());
        em.persist(p);
        
        if (p.getTags() != null && !p.getTags().isEmpty()) {
            for (Tag tag : p.getTags()) {
                userBean.subscribeToTag(ownerUsername, tag.getId());
            }
        }
        
        return p;
    }

    public Publication find(Long id) {
        return em.find(Publication.class, id);
    }

    public Publication findWithRelations(Long id) {
        Publication p = em.find(Publication.class, id);
        if (p != null) {
            Hibernate.initialize(p.getTags());
            Hibernate.initialize(p.getOwner());
            Hibernate.initialize(p.getAuthors());

            for (Tag tag : p.getTags()) {
                Hibernate.initialize(tag.getSubscribedUsers());
            }
        }
        return p;
    }

    public List<Publication> findVisibleWithRelations(String search, String scientificArea, String tagName, String sortBy, String order) {
        List<Publication> publications = findVisible(search, scientificArea, tagName, sortBy, order);
        for (Publication p : publications) {
            Hibernate.initialize(p.getTags());
            Hibernate.initialize(p.getOwner());
            Hibernate.initialize(p.getAuthors());
        }
        return publications;
    }

    public List<Publication> findByOwnerWithRelations(String ownerUsername) {
        List<Publication> publications = findByOwner(ownerUsername);
        for (Publication p : publications) {
            Hibernate.initialize(p.getTags());
            Hibernate.initialize(p.getOwner());
            Hibernate.initialize(p.getAuthors());
        }
        return publications;
    }

    public Publication updateSummary(Long id, String summary, String editedByUsername) {
        Publication p = find(id);
        if (p == null) return null;
        User editedBy = userBean.find(editedByUsername);
        if (editedBy == null) return null;
        String oldValue = p.getSummary() != null ? p.getSummary() : "(vazio)";
        String newValue = summary != null ? summary : "(vazio)";
        p.setSummary(summary);
        p.setLastEdited(LocalDateTime.now());
        em.merge(p);
        recordHistory(p, editedBy, List.of("Resumo: '" + oldValue + "' -> '" + newValue + "'"));
        return p;
    }

    public Publication updateVisibility(Long id, String visibility, String editedByUsername) {
        Publication p = find(id);
        if (p == null) return null;
        User editedBy = userBean.find(editedByUsername);
        if (editedBy == null) return null;
        String oldValue = p.getVisibility() != null ? p.getVisibility() : "(vazio)";
        String newValue = visibility != null ? visibility : "(vazio)";
        p.setVisibility(visibility);
        p.setLastEdited(LocalDateTime.now());
        em.merge(p);
        recordHistory(p, editedBy, List.of("Visibilidade: '" + oldValue + "' -> '" + newValue + "'"));
        return p;
    }

    public Publication updateTitle(Long id, String title, String editedByUsername) {
        Publication p = find(id);
        if (p == null) return null;
        User editedBy = userBean.find(editedByUsername);
        if (editedBy == null) return null;
        String oldValue = p.getTitle() != null ? p.getTitle() : "(vazio)";
        String newValue = title != null ? title : "(vazio)";
        p.setTitle(title);
        p.setLastEdited(LocalDateTime.now());
        em.merge(p);
        recordHistory(p, editedBy, List.of("Título: '" + oldValue + "' -> '" + newValue + "'"));
        return p;
    }

    public Publication updateScientificArea(Long id, String scientificArea, String editedByUsername) {
        Publication p = find(id);
        if (p == null) return null;
        User editedBy = userBean.find(editedByUsername);
        if (editedBy == null) return null;
        String oldValue = p.getScientificArea() != null ? p.getScientificArea() : "(vazio)";
        String newValue = scientificArea != null ? scientificArea : "(vazio)";
        p.setScientificArea(scientificArea);
        p.setLastEdited(LocalDateTime.now());
        em.merge(p);
        recordHistory(p, editedBy, List.of("Área Científica: '" + oldValue + "' -> '" + newValue + "'"));
        return p;
    }

    public Publication updateAuthors(Long id, List<String> authors, String editedByUsername) {
        Publication p = find(id);
        if (p == null) return null;
        User editedBy = userBean.find(editedByUsername);
        if (editedBy == null) return null;
        String oldValue = p.getAuthors() != null && !p.getAuthors().isEmpty() 
            ? String.join(", ", p.getAuthors()) : "(vazio)";
        String newValue = authors != null && !authors.isEmpty() 
            ? String.join(", ", authors) : "(vazio)";
        p.setAuthors(authors != null ? authors : new ArrayList<>());
        p.setLastEdited(LocalDateTime.now());
        em.merge(p);
        recordHistory(p, editedBy, List.of("Autores: '" + oldValue + "' -> '" + newValue + "'"));
        return p;
    }

    public List<Publication> findByOwner(String ownerUsername) {
        User owner = userBean.find(ownerUsername);
        if (owner == null) return List.of();
        return em.createQuery("SELECT p FROM Publication p WHERE p.owner = :owner", Publication.class)
                .setParameter("owner", owner)
                .getResultList();
    }

    public List<Publication> findVisible(String search, String scientificArea, String tagName, String sortBy, String order) {
        // Construir query dinâmica baseada nos filtros
        StringBuilder queryBuilder = new StringBuilder(
                "SELECT p FROM Publication p LEFT JOIN p.comments c " +
                        "WHERE p.visibility <> 'hidden'"
        );
        
        if (scientificArea != null && !scientificArea.trim().isEmpty()) {
            queryBuilder.append(" AND LOWER(p.scientificArea) LIKE LOWER(:scientificArea)");
        }
        
        if (tagName != null && !tagName.trim().isEmpty()) {
            queryBuilder.append(" AND EXISTS (SELECT t FROM p.tags t WHERE LOWER(t.name) LIKE LOWER(:tagName))");
        }
        
        if (search != null && !search.trim().isEmpty()) {
            queryBuilder.append(" AND (LOWER(p.title) LIKE LOWER(:search) OR LOWER(p.summary) LIKE LOWER(:search))");
        }
        
        // Ordenação
        String orderBy = "p.uploadDate DESC";
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            if ("rating".equalsIgnoreCase(sortBy)) {
                orderBy = "p.ratingAvg " + ("desc".equalsIgnoreCase(order) ? "DESC" : "ASC");
            } else if ("comments".equalsIgnoreCase(sortBy)
                    || "commentCount".equalsIgnoreCase(sortBy)) {

                orderBy =
                        "SUM(CASE WHEN c.hidden = false THEN 1 ELSE 0 END) " +
                                ("desc".equalsIgnoreCase(order) ? "DESC" : "ASC");
            }else if ("ratings".equalsIgnoreCase(sortBy)) {
                orderBy = "(SELECT COUNT(r) FROM Rating r WHERE r.publication = p) " + ("desc".equalsIgnoreCase(order) ? "DESC" : "ASC");
            } else if ("date".equalsIgnoreCase(sortBy)) {
                orderBy = "p.uploadDate " + ("desc".equalsIgnoreCase(order) ? "DESC" : "ASC");
            }
        }
        queryBuilder.append(" GROUP BY p");
        queryBuilder.append(" ORDER BY ").append(orderBy);



        jakarta.persistence.TypedQuery<Publication> query = em.createQuery(queryBuilder.toString(), Publication.class);
        
        if (scientificArea != null && !scientificArea.trim().isEmpty()) {
            query.setParameter("scientificArea", "%" + scientificArea.trim() + "%");
        }
        
        if (tagName != null && !tagName.trim().isEmpty()) {
            query.setParameter("tagName", "%" + tagName.trim() + "%");
        }
        
        if (search != null && !search.trim().isEmpty()) {
            query.setParameter("search", "%" + search.trim() + "%");
        }
        
        return query.getResultList();
    }

    // Método antigo - mantido para compatibilidade, mas não recomendado
    // Usar upload() ou updateFile() em vez disso
    public Publication saveFile(Long id, byte[] data, String fileName, String editedByUsername) {
        Publication p = find(id);
        if (p == null) return null;
        User editedBy = userBean.find(editedByUsername);
        if (editedBy == null) editedBy = p.getOwner();
        String oldValue = p.getFileName() != null ? p.getFileName() : "(sem ficheiro)";
        String newValue = fileName != null ? fileName : "(sem ficheiro)";
        p.setFileData(data);
        p.setFileName(fileName);
        p.setLastEdited(LocalDateTime.now());
        em.merge(p);
        recordHistory(p, editedBy, List.of("Ficheiro: '" + oldValue + "' -> '" + newValue + "'"));
        return p;
    }

    public Publication upload(String ownerUsername, String fileName, InputStream stream) throws IOException {
        User owner = userBean.find(ownerUsername);
        if (owner == null) return null;

        Publication p = new Publication();
        p.setOwner(owner);
        p.setUploadDate(LocalDate.now());
        p.setLastEdited(LocalDateTime.now());
        p.setVisibility("internal");
        if (fileName != null && !fileName.isEmpty()) {
            String title = fileName;
            int lastDot = fileName.lastIndexOf('.');
            if (lastDot > 0) {
                title = fileName.substring(0, lastDot);
            }
            p.setTitle(title);
        }

        String ownerUsernameForPath = owner.getUsername();
        Path targetDirectoryPath = Paths.get(UPLOAD_DIR, ownerUsernameForPath);
        if (!Files.exists(targetDirectoryPath)) {
            Files.createDirectories(targetDirectoryPath);
        }

        Path targetFilePath = targetDirectoryPath.resolve("file_" + UUID.randomUUID());
        Files.copy(stream, targetFilePath, StandardCopyOption.REPLACE_EXISTING);

        p.setFileName(fileName);
        p.setFilePath(targetFilePath.toString());

        em.persist(p);
        return p;
    }

    public Publication updateFile(Long id, InputStream stream, String fileName, String editedByUsername) throws IOException {
        Publication p = find(id);
        if (p == null) return null;
        User editedBy = userBean.find(editedByUsername);
        if (editedBy == null) editedBy = p.getOwner();

        String ownerUsername = p.getOwner() != null ? p.getOwner().getUsername() : "unknown";

        Path targetDirectoryPath = Paths.get(UPLOAD_DIR, ownerUsername);
        if (!Files.exists(targetDirectoryPath)) {
            Files.createDirectories(targetDirectoryPath);
        }

        Path targetFilePath = targetDirectoryPath.resolve("file_" + UUID.randomUUID());
        Files.copy(stream, targetFilePath, StandardCopyOption.REPLACE_EXISTING);

        if (p.getFilePath() != null) {
            try {
                Path oldPath = Paths.get(p.getFilePath());
                if (Files.exists(oldPath)) {
                    Files.delete(oldPath);
                }
            } catch (Exception ignored) {}
        }

        String oldValue = p.getFileName() != null ? p.getFileName() : "(sem ficheiro)";
        String newValue = fileName != null ? fileName : "(sem ficheiro)";
        p.setFileName(fileName);
        p.setFilePath(targetFilePath.toString());
        p.setLastEdited(LocalDateTime.now());
        em.merge(p);
        recordHistory(p, editedBy, List.of("Ficheiro: '" + oldValue + "' -> '" + newValue + "'"));
        return p;
    }

    private void recordHistory(Publication publication, User editedBy, List<String> changes) {
        PublicationHistory h = new PublicationHistory();
        h.setPublication(publication);
        h.setEditedBy(editedBy);
        h.setEditDate(LocalDateTime.now());
        h.setChanges(changes);
        em.persist(h);
    }

    public List<PublicationHistory> getHistory(Long publicationId) {
        Publication p = find(publicationId);
        if (p == null) return List.of();
        List<PublicationHistory> history = em.createQuery("SELECT h FROM PublicationHistory h LEFT JOIN FETCH h.editedBy WHERE h.publication = :pub ORDER BY h.editDate DESC", PublicationHistory.class)
                .setParameter("pub", p)
                .getResultList();
        for (PublicationHistory h : history) {
            Hibernate.initialize(h.getChanges());
        }
        return history;
    }

    public Publication addTag(Long publicationId, Long tagId, String editedByUsername) {
        Publication p = find(publicationId);
        Tag tag = tagBean.find(tagId);
        if (p != null && tag != null) {
            p.addTag(tag);
            p.setLastEdited(LocalDateTime.now());
            em.merge(p);
            
            if (p.getOwner() != null) {
                userBean.subscribeToTag(p.getOwner().getUsername(), tagId);
            }
            
            if (editedByUsername != null) {
                User editedBy = userBean.find(editedByUsername);
                if (editedBy != null) {
                    recordHistory(p, editedBy, List.of("Tag adicionada: '" + tag.getName() + "'"));
                }
            }
        }
        return p;
    }
    
    public boolean wasOwnerSubscribedToTag(Long publicationId, Long tagId) {
        Publication p = find(publicationId);
        if (p != null && p.getOwner() != null) {
            User user = userBean.find(p.getOwner().getUsername());
            Tag tag = tagBean.find(tagId);
            if (user != null && tag != null) {
                Hibernate.initialize(user.getSubscribedTags());
                return !user.getSubscribedTags().contains(tag);
            }
        }
        return false;
    }

    public Publication removeTag(Long publicationId, Long tagId, String editedByUsername) {
        Publication p = find(publicationId);
        Tag tag = tagBean.find(tagId);
        if (p != null && tag != null) {
            p.removeTag(tag);
            p.setLastEdited(LocalDateTime.now());
            em.merge(p);
            
            if (editedByUsername != null) {
                User editedBy = userBean.find(editedByUsername);
                if (editedBy != null) {
                    recordHistory(p, editedBy, List.of("Tag removida: '" + tag.getName() + "'"));
                }
            }
        }
        return p;
    }

    public int importFromCSV(InputStream inputStream) throws Exception {

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        );

        String line;
        boolean firstLine = true;
        int count = 0;

        while ((line = reader.readLine()) != null) {

            if (firstLine) {
                firstLine = false;
                continue;
            }

            if (line.trim().isEmpty()) continue;

            String[] parts = line.split(";");

            if (parts.length < 7) continue;

            String ownerUsername   = parts[0].trim();
            String title           = parts[1].trim();
            String summary         = parts[2].trim();
            String scientificArea  = parts[3].trim();
            String visibility      = parts[4].trim();
            String authorsRaw      = parts[5].trim();
            String tagsRaw         = parts[6].trim();

            User owner = userBean.find(ownerUsername);
            if (owner == null) continue;

            Publication p = new Publication();
            p.setOwner(owner);
            p.setTitle(title);
            p.setSummary(summary.isEmpty() ? null : summary);
            p.setScientificArea(scientificArea.isEmpty() ? null : scientificArea);
            p.setVisibility(visibility.isEmpty() ? "internal" : visibility);
            p.setUploadDate(LocalDate.now());
            p.setLastEdited(LocalDateTime.now());

            if (!authorsRaw.isEmpty()) {
                List<String> authors = List.of(authorsRaw.split("\\|"));
                p.setAuthors(new ArrayList<>(authors));
            }

            em.persist(p);
            em.flush();

            if (!tagsRaw.isEmpty()) {
                String[] tagNames = tagsRaw.split("\\|");
                for (String tagName : tagNames) {
                    Tag tag = tagBean.findByName(tagName.trim());
                    if (tag != null) {
                        p.addTag(tag);
                        userBean.subscribeToTag(ownerUsername, tag.getId());
                    }
                }
            }

            count++;
        }

        return count;
    }


}