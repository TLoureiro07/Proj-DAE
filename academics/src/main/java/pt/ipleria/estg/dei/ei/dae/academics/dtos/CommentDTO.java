package pt.ipleria.estg.dei.ei.dae.academics.dtos;

import pt.ipleria.estg.dei.ei.dae.academics.entities.Comment;
import pt.ipleria.estg.dei.ei.dae.academics.entities.User;

import java.time.LocalDateTime;

public class CommentDTO {
    public Long id;
    public String text;
    public String authorUsername;
    public String authorName;
    public Long publicationId;
    public LocalDateTime commentDate;
    public boolean hidden;

    public CommentDTO() {}

    public CommentDTO(Long id, String text, String authorUsername, String authorName, 
                      Long publicationId, LocalDateTime commentDate, boolean hidden) {
        this.id = id;
        this.text = text;
        this.authorUsername = authorUsername;
        this.authorName = authorName;
        this.publicationId = publicationId;
        this.commentDate = commentDate;
        this.hidden = hidden;
    }

    public static CommentDTO from(Comment comment) {
        User author = comment.getAuthor();
        return new CommentDTO(
            comment.getId(),
            comment.getText(),
            author != null ? author.getUsername() : null,
            author != null ? author.getName() : null,
            comment.getPublication() != null ? comment.getPublication().getId() : null,
            comment.getCommentDate(),
            comment.isHidden()
        );
    }
}

