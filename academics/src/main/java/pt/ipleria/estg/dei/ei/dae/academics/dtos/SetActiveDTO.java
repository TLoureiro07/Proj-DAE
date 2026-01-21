package pt.ipleria.estg.dei.ei.dae.academics.dtos;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

public class SetActiveDTO implements Serializable {
    @NotNull
    public Boolean active;

    public SetActiveDTO() {}
}
