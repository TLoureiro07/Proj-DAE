package pt.ipleria.estg.dei.ei.dae.academics.dtos;


import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import pt.ipleria.estg.dei.ei.dae.academics.entities.Student;

public class StudentDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String username;
    private String password;
    private String name;
    private String email;

    public StudentDTO() {}

    public StudentDTO(String username, String password, String name, String email) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.email = email;
    }

    // getters & setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // helper converters
    public static StudentDTO from(Student student) {
        return new StudentDTO(
                student.getUsername(),
                student.getPassword(),
                student.getName(),
                student.getEmail()
        );
    }

    public static List<StudentDTO> from(List<Student> students) {
        return students.stream().map(StudentDTO::from).collect(Collectors.toList());
    }
}

