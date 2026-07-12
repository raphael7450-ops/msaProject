package jar.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import jar.client.dto.UserDto;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/users")
    List<UserDto> getUsers();

    @GetMapping("/users/{id}")
    UserDto getUser(@PathVariable("id") Long id);

}
