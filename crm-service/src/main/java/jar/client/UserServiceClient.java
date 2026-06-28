package jar.client;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import jar.client.dto.UserDto;
import jar.client.dto.UserRequestDto;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/users")
    List<UserDto> getUsers();

    @GetMapping("/users/{id}")
    UserDto getUser(@PathVariable("id") Long id);

    @PostMapping("/users")
    UserDto createUser(@RequestBody UserRequestDto request);

    @PutMapping("/users/{id}")
    UserDto updateUser(@PathVariable("id") Long id, @RequestBody UserRequestDto request);

    @DeleteMapping("/users/{id}")
    void deleteUser(@PathVariable("id") Long id);

}
