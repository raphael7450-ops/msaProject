package jar.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeadRequest {
    private String name;
    private String company;
    private String email;
    private String phone;
}
