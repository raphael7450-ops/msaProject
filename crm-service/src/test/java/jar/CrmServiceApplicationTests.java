package jar;

import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jar.client.UserServiceClient;
import jar.client.dto.UserDto;

@SpringBootTest
class CrmServiceApplicationTests {

	@MockitoBean
	private UserServiceClient userServiceClient;

	@Test
	void contextLoads() {
		UserDto user = new UserDto();
		user.setId(1L);
		user.setUserId("test_user");
		user.setName("테스트");
		user.setEmail("test@example.com");
		when(userServiceClient.getUsers()).thenReturn(List.of(user));
	}

}
