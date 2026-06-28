package jar.dto;

import jar.enums.LeadStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeadStatusRequest {
    private LeadStatus status;
}
