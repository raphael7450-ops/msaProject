package jar.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DashboardStats {

    private final int totalCustomers;
    private final int newLeads;
    private final int activeDeals;
    private final int closedThisMonth;

}
