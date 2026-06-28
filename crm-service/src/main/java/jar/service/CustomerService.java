package jar.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import jar.client.UserServiceClient;
import jar.client.dto.UserDto;
import jar.client.dto.UserRequestDto;
import jar.model.Customer;
import jar.model.CustomerForm;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final UserServiceClient userServiceClient;

    public List<Customer> getAllCustomers() {
        return userServiceClient.getUsers().stream()
                .sorted(Comparator.comparing(UserDto::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toCustomer)
                .toList();
    }

    public CustomerForm getCustomerForm(Long id) {
        UserDto user = userServiceClient.getUser(id);
        CustomerForm form = new CustomerForm();
        form.setId(user.getId());
        form.setUserId(user.getUserId());
        form.setName(user.getName());
        form.setEmail(user.getEmail());
        return form;
    }

    public void createCustomer(CustomerForm form) {
        userServiceClient.createUser(toRequest(form));
    }

    public void updateCustomer(Long id, CustomerForm form) {
        userServiceClient.updateUser(id, toRequest(form));
    }

    public void deleteCustomer(Long id) {
        userServiceClient.deleteUser(id);
    }

    private UserRequestDto toRequest(CustomerForm form) {
        return new UserRequestDto(form.getUserId(), form.getName(), form.getEmail());
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
