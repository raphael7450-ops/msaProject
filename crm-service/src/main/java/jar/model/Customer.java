package jar.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Customer {

    private final Long id;
    private final String name;
    private final String company;
    private final String email;
    private final String status;
    private final String lastContact;

}
