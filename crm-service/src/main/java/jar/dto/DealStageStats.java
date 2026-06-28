package jar.dto;

import java.math.BigDecimal;

import jar.enums.DealStage;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DealStageStats {
    private final DealStage stage;
    private final BigDecimal totalAmount;
    private final long dealCount;
}
