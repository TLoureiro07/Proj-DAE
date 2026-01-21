package pt.ipleria.estg.dei.ei.dae.academics.dtos;

import pt.ipleria.estg.dei.ei.dae.academics.entities.Tag;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

public class TagDTO implements Serializable {
    public Long id;
    public String name;

    public TagDTO() {}

    public TagDTO(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public static TagDTO from(Tag tag) {
        return new TagDTO(tag.getId(), tag.getName());
    }

    public static List<TagDTO> from(List<Tag> tags) {
        return tags.stream().map(TagDTO::from).collect(Collectors.toList());
    }
}

