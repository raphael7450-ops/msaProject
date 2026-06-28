package jar.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import jar.entity.DealEntity;
import jar.enums.DealStage;

@Repository
public interface DealRepository extends CrudRepository<DealEntity, Long> {

    List<DealEntity> findByLeadId(Long leadId);

    List<DealEntity> findByStage(DealStage stage);

    @Query("SELECT d.stage AS stage, SUM(d.amount) AS totalAmount, COUNT(d) AS dealCount "
            + "FROM DealEntity d GROUP BY d.stage")
    List<StageAmountSummary> summarizeAmountByStage();

    interface StageAmountSummary {
        DealStage getStage();

        java.math.BigDecimal getTotalAmount();

        Long getDealCount();
    }
}
