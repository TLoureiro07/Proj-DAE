package pt.ipleria.estg.dei.ei.dae.academics.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import pt.ipleria.estg.dei.ei.dae.academics.entities.PublicationHistory;

public class PublicationHistoryDTO implements Serializable {
    public Long editId;
    public String editedBy;
    public String editDate;
    public List<String> changes;

    public PublicationHistoryDTO() {}

    public static PublicationHistoryDTO from(PublicationHistory h) {
        PublicationHistoryDTO d = new PublicationHistoryDTO();
        d.editId = h.getEditId();
        d.editedBy = h.getEditedBy() != null ? h.getEditedBy().getUsername() : null;
        d.editDate = h.getEditDate() != null 
            ? h.getEditDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            : null;
        d.changes = h.getChanges();
        return d;
    }

    public static List<PublicationHistoryDTO> from(List<PublicationHistory> history) {
        return history.stream()
            .map(PublicationHistoryDTO::from)
            .collect(Collectors.toList());
    }
}
