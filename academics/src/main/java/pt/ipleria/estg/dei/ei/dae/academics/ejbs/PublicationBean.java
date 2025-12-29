package pt.ipleria.estg.dei.ei.dae.academics.ejbs;

import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Publication;
import pt.ipleria.estg.dei.ei.dae.academics.entities.PublicationHistory;

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
  /*  @PersistenceContext
    private EntityManager em;

    public Publication create(String owner, Publication p) {
        p.setOwner(owner);
        p.setUploadDate(LocalDate.now());
        p.setLastEdited(LocalDateTime.now());
        em.persist(p);
        return p;
    }

    public Publication find(Long id) {
        return em.find(Publication.class, id);
    }

    public Publication updateSummary(Long id, String summary, String editedBy) {
        Publication p = find(id);
        if (p == null) return null;
        p.setSummary(summary);
        p.setLastEdited(LocalDateTime.now());
        em.merge(p);
        recordHistory(id, editedBy, List.of("summary"));
        return p;
    }

    public Publication changeVisibility(Long id, String visibility, String editedBy) {
        Publication p = find(id);
        if (p == null) return null;
        p.setVisibility(visibility);
        p.setLastEdited(LocalDateTime.now());
        em.merge(p);
        recordHistory(id, editedBy, List.of("visibility"));
        return p;
    }

    public List<Publication> findByOwner(String owner) {
        return em.createQuery("SELECT p FROM Publication p WHERE p.owner = :owner", Publication.class)
                .setParameter("owner", owner)
                .getResultList();
    }

    public List<Publication> findVisible(String search, String sortBy, String order) {
        // mínimo: retorna todas com visibility != "hidden"
        return em.createQuery("SELECT p FROM Publication p WHERE p.visibility <> 'hidden' ORDER BY p.uploadDate DESC", Publication.class)
                .getResultList();
    }

    public Publication saveFile(Long id, byte[] data, String fileName, String editedBy) {
        Publication p = find(id);
        if (p == null) return null;
        p.setFileData(data);
        p.setFileName(fileName);
        p.setLastEdited(LocalDateTime.now());
        em.merge(p);
        recordHistory(id, editedBy, List.of("file"));
        return p;
    }

    // New: save file to filesystem and store filepath in entity
    public Publication saveFile(Long id, InputStream stream, String fileName, String editedBy) throws IOException {
        Publication p = find(id);
        if (p == null) return null;

        String owner = p.getOwner();
        if (owner == null) owner = "unknown";

        Path uploadBase = Paths.get(System.getProperty("java.io.tmpdir"), "academics_uploads");
        Path targetDirectoryPath = uploadBase.resolve(owner);
        if (!Files.exists(targetDirectoryPath)) {
            Files.createDirectories(targetDirectoryPath);
        }

        Path targetFilePath = targetDirectoryPath.resolve("file_" + UUID.randomUUID());
        Files.copy(stream, targetFilePath, StandardCopyOption.REPLACE_EXISTING);

        p.setFileData(null); // free DB storage if previously used
        p.setFileName(fileName);
        p.setFilePath(targetFilePath.toString());
        p.setLastEdited(LocalDateTime.now());
        em.merge(p);
        recordHistory(id, editedBy, List.of("file"));
        return p;
    }

    private void recordHistory(Long publicationId, String editedBy, List<String> changes) {
        PublicationHistory h = new PublicationHistory();
        h.setPublicationId(publicationId);
        h.setEditedBy(editedBy);
        h.setEditDate(LocalDateTime.now());
        h.setChanges(changes);
        em.persist(h);
    }

    public List<PublicationHistory> getHistory(Long publicationId) {
        return em.createQuery("SELECT h FROM PublicationHistory h WHERE h.publicationId = :pid ORDER BY h.editDate DESC", PublicationHistory.class)
                .setParameter("pid", publicationId)
                .getResultList();
    }*/
}