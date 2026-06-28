package jar;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository // 이 어노테이션이 있으면 스프링이 관리하는 빈(Bean)이 됩니다.
public interface UserRepository extends CrudRepository<UserEntity, Long> {

    UserEntity findByUserId(String userId);
}