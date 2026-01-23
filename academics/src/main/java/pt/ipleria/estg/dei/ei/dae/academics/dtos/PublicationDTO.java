package pt.ipleria.estg.dei.ei.dae.academics.dtos;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Publication;

public class PublicationDTO implements Serializable {
    public Long id;
    public String title;
    public List<String> authors;
    public String scientificArea;
    public List<TagDTO> tags;
    public String visibility;
    public String owner;
    public String uploadDate;
    public String fileName;
    public String summary;
    public String aiSummary;
    public Double ratingAvg;
    public String lastEdited;

    public PublicationDTO() {}

    public static PublicationDTO from(Publication p) {
        PublicationDTO d = new PublicationDTO();
        d.id = p.getId();
        d.title = p.getTitle();
        d.authors = p.getAuthors();
        d.scientificArea = p.getScientificArea();
        d.tags = p.getTags() != null ? 
            p.getTags().stream().map(TagDTO::from).collect(Collectors.toList()) : 
            List.of();
        d.visibility = p.getVisibility();
        d.owner = p.getOwner() != null ? p.getOwner().getUsername() : null;
        d.uploadDate = p.getUploadDate() != null ? p.getUploadDate().toString() : null;
        d.fileName = p.getFileName();
        d.summary = p.getSummary();
        d.aiSummary = p.getAiSummary();
        d.ratingAvg = p.getRatingAvg();
        d.lastEdited = p.getLastEdited() != null ? p.getLastEdited().toString() : null;
        return d;
    }

    public static List<PublicationDTO> from(List<Publication> publications) {
        return publications.stream().map(PublicationDTO::from).collect(Collectors.toList());
    }
}