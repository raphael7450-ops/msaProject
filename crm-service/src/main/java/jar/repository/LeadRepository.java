package jar.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import jar.entity.LeadEntity;

@Repository
public interface LeadRepository extends CrudRepository<LeadEntity, Long> {
}
