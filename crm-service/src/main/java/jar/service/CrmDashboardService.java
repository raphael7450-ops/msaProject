package jar.service;

import java.util.List;

import org.springframework.stereotype.Service;

import jar.client.UserServiceClient;
import jar.client.dto.UserDto;
import jar.model.DashboardStats;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CrmDashboardService {

    private final UserServiceClient userServiceClient;

    public DashboardStats getStats() {
        List<UserDto> users = userServiceClient.getUsers();
        int totalCustomers = users.size();
        return new DashboardStats(totalCustomers, 0, 0, 0);
    }

}
