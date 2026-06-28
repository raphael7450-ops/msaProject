package jar;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jar.dto.UserRequest;

@RestController
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/test")
    public String test() {
        return "~!!@~!@!@#seojiho";
    }

    @GetMapping("/test_save")
    public String testSave() {
        UserEntity user = new UserEntity();
        user.setUserId("chagwon_test");
        user.setName("서지호");
        user.setEmail("seojiho@gmail.com");
        userRepository.save(user);
        return "Welcome!! user 저장 완료";
    }

    @GetMapping("/users")
    public List<UserEntity> getAllUsers() {
        List<UserEntity> users = new ArrayList<>();
        userRepository.findAll().forEach(users::add);
        return users;
    }

    @GetMapping("/users/{id}")
    public UserEntity getUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    @PostMapping("/users")
    public ResponseEntity<UserEntity> createUser(@RequestBody UserRequest request) {
        validateRequest(request, null);

        UserEntity existing = userRepository.findByUserId(request.getUserId().trim());
        if (existing != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 사용자 ID입니다.");
        }

        UserEntity user = new UserEntity();
        user.setUserId(request.getUserId().trim());
        user.setName(request.getName().trim());
        user.setEmail(request.getEmail().trim());
        UserEntity saved = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/users/{id}")
    public UserEntity updateUser(@PathVariable Long id, @RequestBody UserRequest request) {
        validateRequest(request, id);

        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        UserEntity duplicate = userRepository.findByUserId(request.getUserId().trim());
        if (duplicate != null && !duplicate.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 사용자 ID입니다.");
        }

        user.setUserId(request.getUserId().trim());
        user.setName(request.getName().trim());
        user.setEmail(request.getEmail().trim());
        return userRepository.save(user);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void validateRequest(UserRequest request, Long excludeId) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 데이터가 없습니다.");
        }
        if (isBlank(request.getUserId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사용자 ID는 필수입니다.");
        }
        if (isBlank(request.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이름은 필수입니다.");
        }
        if (isBlank(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일은 필수입니다.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

}
