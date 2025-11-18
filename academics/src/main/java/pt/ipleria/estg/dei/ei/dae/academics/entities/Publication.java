package pt.ipleria.estg.dei.ei.dae.academics.entities;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "publications")
public class Publication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @ElementCollection
    private List<String> authors;

    private String scientificArea;

    @ElementCollection
    private List<String> tags;

    private String visibility; // "public", "internal", "hidden"

    private LocalDate uploadDate;

    private String owner; // username of creator

    @Lob
    private byte[] fileData;

    private String fileName;

    private String filePath;

    private String summary;

    private Double ratingAvg;

    private LocalDateTime lastEdited;

    @Version
    private int version;

    // getters / setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<String> getAuthors() { return authors; }
    public void setAuthors(List<String> authors) { this.authors = authors; }

    public String getScientificArea() { return scientificArea; }
    public void setScientificArea(String scientificArea) { this.scientificArea = scientificArea; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public LocalDate getUploadDate() { return uploadDate; }
    public void setUploadDate(LocalDate uploadDate) { this.uploadDate = uploadDate; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public Double getRatingAvg() { return ratingAvg; }
    public void setRatingAvg(Double ratingAvg) { this.ratingAvg = ratingAvg; }

    public LocalDateTime getLastEdited() { return lastEdited; }
    public void setLastEdited(LocalDateTime lastEdited) { this.lastEdited = lastEdited; }

    public int getVersion() { return version; }
    // no setter for version (managed by JPA)
}