package jar.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jar.service.LoginService;

@RestController
@RequestMapping("/login")
// @RequiredArgsConstructor // 👈 일단 이 어노테이션은 지우거나 주석 처리하세요.
public class LoginController {
    
    // final 필드는 생성자에서 반드시 초기화되어야 합니다.
    private final LoginService loginService;
    
    // 직접 생성자를 만듭니다. (롬복 대신 수동 주입)
    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }
    
    @PostMapping("/process")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {
        String token = loginService.login(request.getUserId(), request.getPassword());
        return ResponseEntity.ok(token);
    }
}

//간단한 요청 DTO (같은 파일 하단이나 별도 파일에 작성)
class LoginRequest {
 private String userId;
 private String password;
 
 public String getUserId() {return userId;}
 public String getPassword() {return password;}
 
 public void setUserId(String userId) { this.userId = userId; }
 public void setPassword(String password) { this.password = password; }

}
