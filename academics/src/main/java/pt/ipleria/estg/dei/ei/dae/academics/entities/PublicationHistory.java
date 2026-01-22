package pt.ipleria.estg.dei.ei.dae.academics.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "publication_history")
public class PublicationHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long editId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "publication_id", nullable = false)
    private Publication publication;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "edited_by_username")
    private User editedBy;

    private LocalDateTime editDate;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "publication_history_changes", joinColumns = @JoinColumn(name = "edit_id"))
    @Column(name = "change")
    private List<String> changes;

    // getters / setters
    public Long getEditId() { return editId; }
    public void setEditId(Long editId) { this.editId = editId; }

    public Publication getPublication() { return publication; }
    public void setPublication(Publication publication) { this.publication = publication; }

    public User getEditedBy() { return editedBy; }
    public void setEditedBy(User editedBy) { this.editedBy = editedBy; }

    public LocalDateTime getEditDate() { return editDate; }
    public void setEditDate(LocalDateTime editDate) { this.editDate = editDate; }

    public List<String> getChanges() { return changes; }
    public void setChanges(List<String> changes) { this.changes = changes; }
}