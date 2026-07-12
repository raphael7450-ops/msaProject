package jar.service;

import org.springframework.stereotype.Service;

import jar.model.DashboardStats;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CrmDashboardService {

    private final CustomerService customerService;

    public DashboardStats getStats() {
        int totalCustomers = customerService.getAllCustomers().customers().size();
        return new DashboardStats(totalCustomers, 0, 0, 0);
    }

}
