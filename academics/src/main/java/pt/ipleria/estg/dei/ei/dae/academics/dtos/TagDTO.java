package pt.ipleria.estg.dei.ei.dae.academics.dtos;

import pt.ipleria.estg.dei.ei.dae.academics.entities.Tag;

public class TagDTO {
    public Long id;
    public String name;

    public static TagDTO from(Tag tag) {
        TagDTO dto = new TagDTO();
        dto.id = tag.getId();
        dto.name = tag.getName();
        return dto;
    }
}

