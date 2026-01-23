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
import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Stateless
public class PublicationBean {
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
        return p;
    }

    public Publication find(Long id) {
        return em.find(Publication.class, id);
    }

    // Método para encontrar publicação com relações lazy inicializadas (para DTOs)
    public Publication findWithRelations(Long id) {
        Publication p = em.find(Publication.class, id);
        if (p != null) {
            // Inicializar relações lazy antes de fechar a sessão
            Hibernate.initialize(p.getTags());
            Hibernate.initialize(p.getOwner());
            Hibernate.initialize(p.getAuthors());

            for (Tag tag : p.getTags()) {
                Hibernate.initialize(tag.getSubscribedUsers());
            }
        }
        return p;
    }

    // Método para encontrar publicações com relações lazy inicializadas
    public List<Publication> findVisibleWithRelations(String search, String scientificArea, String tagName, String sortBy, String order) {
        List<Publication> publications = findVisible(search, scientificArea, tagName, sortBy, order);
        // Inicializar relações lazy para cada publicação
        for (Publication p : publications) {
            Hibernate.initialize(p.getTags());
            Hibernate.initialize(p.getOwner());
            Hibernate.initialize(p.getAuthors());
        }
        return publications;
    }

    // Método para encontrar publicações por owner com relações lazy inicializadas
    public List<Publication> findByOwnerWithRelations(String ownerUsername) {
        List<Publication> publications = findByOwner(ownerUsername);
        // Inicializar relações lazy para cada publicação
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
        p.setSummary(summary);
        p.setLastEdited(LocalDateTime.now());
        em.merge(p);
        recordHistory(p, editedBy, List.of("summary"));
        return p;
    }

    public Publication updateVisibility(Long id, String visibility, String editedByUsername) {
        Publication p = find(id);
        if (p == null) return null;
        User editedBy = userBean.find(editedByUsername);
        if (editedBy == null) return null;
        p.setVisibility(visibility);
        p.setLastEdited(LocalDateTime.now());
        em.merge(p);
        recordHistory(p, editedBy, List.of("visibility"));
        return p;
    }

    public Publication updateTitle(Long id, String title, String editedByUsername) {
        Publication p = find(id);
        if (p == null) return null;
        User editedBy = userBean.find(editedByUsername);
        if (editedBy == null) return null;
        p.setTitle(title);
        p.setLastEdited(LocalDateTime.now());
        em.merge(p);
        recordHistory(p, editedBy, List.of("title"));
        return p;
    }

    public Publication updateScientificArea(Long id, String scientificArea, String editedByUsername) {
        Publication p = find(id);
        if (p == null) return null;
        User editedBy = userBean.find(editedByUsername);
        if (editedBy == null) return null;
        p.setScientificArea(scientificArea);
        p.setLastEdited(LocalDateTime.now());
        em.merge(p);
        recordHistory(p, editedBy, List.of("scientificArea"));
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
        StringBuilder queryBuilder = new StringBuilder("SELECT DISTINCT p FROM Publication p WHERE p.visibility <> 'hidden'");
        
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
            } else if ("comments".equalsIgnoreCase(sortBy)) {
                orderBy = "(SELECT COUNT(c) FROM Comment c WHERE c.publication = p) " + ("desc".equalsIgnoreCase(order) ? "DESC" : "ASC");
            } else if ("ratings".equalsIgnoreCase(sortBy)) {
                orderBy = "(SELECT COUNT(r) FROM Rating r WHERE r.publication = p) " + ("desc".equalsIgnoreCase(order) ? "DESC" : "ASC");
            } else if ("date".equalsIgnoreCase(sortBy)) {
                orderBy = "p.uploadDate " + ("desc".equalsIgnoreCase(order) ? "DESC" : "ASC");
            }
        }
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
        p.setFileData(data);
        p.setFileName(fileName);
        p.setLastEdited(LocalDateTime.now());
        em.merge(p);
        recordHistory(p, editedBy, List.of("file"));
        return p;
    }

    // Upload: cria publicação diretamente com o ficheiro (padrão Ficha 9)
    public Publication upload(String ownerUsername, String fileName, InputStream stream) throws IOException {
        User owner = userBean.find(ownerUsername);
        if (owner == null) return null;

        // Criar nova publicação
        Publication p = new Publication();
        p.setOwner(owner);
        p.setUploadDate(LocalDate.now());
        p.setLastEdited(LocalDateTime.now());
        p.setVisibility("internal"); // default
        // Título pode ser extraído do nome do ficheiro ou gerado depois
        if (fileName != null && !fileName.isEmpty()) {
            // Remover extensão para usar como título inicial
            String title = fileName;
            int lastDot = fileName.lastIndexOf('.');
            if (lastDot > 0) {
                title = fileName.substring(0, lastDot);
            }
            p.setTitle(title);
        }

        // Salvar ficheiro no filesystem
        String ownerUsernameForPath = owner.getUsername();
        Path uploadBase = Paths.get(System.getProperty("java.io.tmpdir"), "academics_uploads");
        Path targetDirectoryPath = uploadBase.resolve(ownerUsernameForPath);
        if (!Files.exists(targetDirectoryPath)) {
            Files.createDirectories(targetDirectoryPath);
        }

        Path targetFilePath = targetDirectoryPath.resolve("file_" + UUID.randomUUID());
        Files.copy(stream, targetFilePath, StandardCopyOption.REPLACE_EXISTING);

        p.setFileName(fileName);
        p.setFilePath(targetFilePath.toString());

        em.persist(p);
        recordHistory(p, owner, List.of("upload"));
        return p;
    }

    // Método para atualizar ficheiro de publicação existente (se necessário)
    public Publication updateFile(Long id, InputStream stream, String fileName, String editedByUsername) throws IOException {
        Publication p = find(id);
        if (p == null) return null;
        User editedBy = userBean.find(editedByUsername);
        if (editedBy == null) editedBy = p.getOwner();

        String ownerUsername = p.getOwner() != null ? p.getOwner().getUsername() : "unknown";

        Path uploadBase = Paths.get(System.getProperty("java.io.tmpdir"), "academics_uploads");
        Path targetDirectoryPath = uploadBase.resolve(ownerUsername);
        if (!Files.exists(targetDirectoryPath)) {
            Files.createDirectories(targetDirectoryPath);
        }

        Path targetFilePath = targetDirectoryPath.resolve("file_" + UUID.randomUUID());
        Files.copy(stream, targetFilePath, StandardCopyOption.REPLACE_EXISTING);

        // Remover ficheiro antigo se existir
        if (p.getFilePath() != null) {
            try {
                Path oldPath = Paths.get(p.getFilePath());
                if (Files.exists(oldPath)) {
                    Files.delete(oldPath);
                }
            } catch (Exception ignored) {}
        }

        p.setFileName(fileName);
        p.setFilePath(targetFilePath.toString());
        p.setLastEdited(LocalDateTime.now());
        em.merge(p);
        recordHistory(p, editedBy, List.of("file"));
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
        return em.createQuery("SELECT h FROM PublicationHistory h WHERE h.publication = :pub ORDER BY h.editDate DESC", PublicationHistory.class)
                .setParameter("pub", p)
                .getResultList();
    }

    public Publication addTag(Long publicationId, Long tagId) {
        Publication p = find(publicationId);
        Tag tag = tagBean.find(tagId);
        if (p != null && tag != null) {
            p.addTag(tag);
            em.merge(p);
        }
        return p;
    }

    public Publication removeTag(Long publicationId, Long tagId) {
        Publication p = find(publicationId);
        Tag tag = tagBean.find(tagId);
        if (p != null && tag != null) {
            p.removeTag(tag);
            em.merge(p);
        }
        return p;
    }

    public Publication updateAiSummary(Long id, String aiSummary, String editedByUsername) {
        Publication p = find(id);
        if (p == null) return null;

        User editedBy = userBean.find(editedByUsername);
        if (editedBy == null) return null;

        p.setAiSummary(aiSummary);
        p.setLastEdited(LocalDateTime.now());
        em.merge(p);

        // Track the change in history
        recordHistory(p, editedBy, List.of("aiSummary"));

        return p;
    }
}