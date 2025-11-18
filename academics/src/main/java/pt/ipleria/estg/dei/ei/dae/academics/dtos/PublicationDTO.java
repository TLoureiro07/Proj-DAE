package pt.ipleria.estg.dei.ei.dae.academics.dtos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Publication;

public class PublicationDTO {
    public Long id;
    public String title;
    public List<String> authors;
    public String scientificArea;
    public List<String> tags;
    public String visibility;
    public String owner;
    public LocalDate uploadDate;
    public String fileName;
    public String summary;
    public Double ratingAvg;
    public LocalDateTime lastEdited;

    public static PublicationDTO from(Publication p) {
        PublicationDTO d = new PublicationDTO();
        d.id = p.getId();
        d.title = p.getTitle();
        d.authors = p.getAuthors();
        d.scientificArea = p.getScientificArea();
        d.tags = p.getTags();
        d.visibility = p.getVisibility();
        d.owner = p.getOwner();
        d.uploadDate = p.getUploadDate();
        d.fileName = p.getFileName();
        d.summary = p.getSummary();
        d.ratingAvg = p.getRatingAvg();
        d.lastEdited = p.getLastEdited();
        return d;
    }
}