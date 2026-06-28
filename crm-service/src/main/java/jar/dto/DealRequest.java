package jar.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DealRequest {
    private String title;
    private BigDecimal amount;
    private Long leadId;
    private LocalDate closingDate;
}
