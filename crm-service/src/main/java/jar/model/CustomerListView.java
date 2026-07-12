package jar.model;

import java.util.List;

public record CustomerListView(List<Customer> customers, boolean userServiceUnavailable) {
}
