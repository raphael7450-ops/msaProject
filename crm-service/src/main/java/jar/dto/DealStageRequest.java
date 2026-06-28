package jar.dto;

import jar.enums.DealStage;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DealStageRequest {
    private DealStage stage;
}
