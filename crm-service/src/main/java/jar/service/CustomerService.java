package jar.service;

import java.util.Comparator;
import java.util.Collections;
import java.util.List;

import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import jar.client.UserServiceClient;
import jar.client.dto.UserDto;
import jar.model.Customer;
import jar.model.CustomerListView;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final UserServiceClient userServiceClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    public CustomerListView getAllCustomers() {
        return circuitBreakerFactory.create("user-service-customers")
                .run(this::loadCustomers, throwable -> new CustomerListView(Collections.emptyList(), true));
    }

    public Customer getCustomer(Long id) {
        return circuitBreakerFactory.create("user-service-customer")
                .run(() -> toCustomer(userServiceClient.getUser(id)), throwable -> {
                    throw new ResponseStatusException(
                            HttpStatus.SERVICE_UNAVAILABLE,
                            "사용자 서비스가 일시적으로 응답하지 않습니다.",
                            throwable);
                });
    }

    private CustomerListView loadCustomers() {
        List<Customer> customers = userServiceClient.getUsers().stream()
                .sorted(Comparator.comparing(UserDto::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toCustomer)
                .toList();
        return new CustomerListView(customers, false);
    }

    private Customer toCustomer(UserDto user) {
        return new Customer(
                user.getId(),
                user.getName(),
                user.getUserId(),
                user.getEmail(),
                "등록",
                "-"
        );
    }

}
