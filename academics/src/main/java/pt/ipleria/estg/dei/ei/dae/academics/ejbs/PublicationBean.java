package pt.ipleria.estg.dei.ei.dae.academics.ejbs;

import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Publication;
import pt.ipleria.estg.dei.ei.dae.academics.entities.PublicationHistory;
import pt.ipleria.estg.dei.ei.dae.academics.entities.User;

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

    public List<Publication> findByOwner(String ownerUsername) {
        User owner = userBean.find(ownerUsername);
        if (owner == null) return List.of();
        return em.createQuery("SELECT p FROM Publication p WHERE p.owner = :owner", Publication.class)
                .setParameter("owner", owner)
                .getResultList();
    }

    public List<Publication> findVisible(String search, String sortBy, String order) {
        // mínimo: retorna todas com visibility != "hidden"
        return em.createQuery("SELECT p FROM Publication p WHERE p.visibility <> 'hidden' ORDER BY p.uploadDate DESC", Publication.class)
                .getResultList();
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
}