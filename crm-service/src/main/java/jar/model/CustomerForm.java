package jar.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CustomerForm {

    private Long id;
    private String userId;
    private String name;
    private String email;

    public CustomerForm(String userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
    }

}
