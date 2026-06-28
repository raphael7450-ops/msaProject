package jar.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jar.UserEntity;
import jar.UserRepository;
import jar.security.TokenProvider;

@Service
public class LoginService {
	
	private final TokenProvider tokenProvider;
	private final UserRepository userRepository;
	private final BCryptPasswordEncoder passwordEncoder;
	
	public LoginService(
			TokenProvider tokenProvider,
			UserRepository userRepository,
			BCryptPasswordEncoder passwordEncoder) {
		this.tokenProvider = tokenProvider;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}
	
	public String login(String userId, String password) {
		UserEntity user = userRepository.findByUserId(userId);

		if (user == null || password == null || !passwordEncoder.matches(password, user.getPassword())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid userId or password");
		}

		return tokenProvider.createToken(user.getUserId());
	}

}
