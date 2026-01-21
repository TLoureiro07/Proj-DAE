package pt.ipleria.estg.dei.ei.dae.academics.dtos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

public class CreateRatingDTO implements Serializable {
    @NotNull
    @Min(1)
    @Max(5)
    public Integer value;

    public CreateRatingDTO() {}

    public CreateRatingDTO(Integer value) {
        this.value = value;
    }
}



