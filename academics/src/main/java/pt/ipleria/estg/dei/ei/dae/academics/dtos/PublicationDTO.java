package pt.ipleria.estg.dei.ei.dae.academics.dtos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Publication;
import pt.ipleria.estg.dei.ei.dae.academics.entities.Tag;

public class PublicationDTO {
    public Long id;
    public String title;
    public List<String> authors;
    public String scientificArea;
    public List<TagDTO> tags;
    public String visibility;
    public String owner;
    public LocalDate uploadDate;
    public String fileName;
    public String summary;
    public Double ratingAvg;
    public LocalDateTime lastEdited;

    public static PublicationDTO from(Publication p) {
        if (p == null) return null;
        
        PublicationDTO d = new PublicationDTO();
        d.id = p.getId();
        d.title = p.getTitle();
        d.authors = p.getAuthors() != null ? p.getAuthors() : List.of();
        d.scientificArea = p.getScientificArea();
        
        // Tratar tags com cuidado para evitar lazy loading exception
        try {
            if (p.getTags() != null) {
                d.tags = p.getTags().stream().map(TagDTO::from).collect(Collectors.toList());
            } else {
                d.tags = List.of();
            }
        } catch (Exception e) {
            System.err.println("Erro ao converter tags: " + e.getMessage());
            d.tags = List.of();
        }
        
        d.visibility = p.getVisibility();
        
        // Tratar owner com cuidado
        try {
            d.owner = p.getOwner() != null ? p.getOwner().getUsername() : null;
        } catch (Exception e) {
            System.err.println("Erro ao obter owner: " + e.getMessage());
            d.owner = null;
        }
        
        d.uploadDate = p.getUploadDate();
        d.fileName = p.getFileName();
        d.summary = p.getSummary();
        d.ratingAvg = p.getRatingAvg();
        d.lastEdited = p.getLastEdited();
        return d;
    }
}