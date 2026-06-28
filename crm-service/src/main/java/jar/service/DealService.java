package jar.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jar.dto.DealRequest;
import jar.dto.DealStageStats;
import jar.entity.DealEntity;
import jar.entity.LeadEntity;
import jar.enums.DealStage;
import jar.enums.LeadStatus;
import jar.repository.DealRepository;
import jar.repository.DealRepository.StageAmountSummary;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DealService {

    private final DealRepository dealRepository;
    private final LeadService leadService;

    public List<DealEntity> getAllDeals() {
        List<DealEntity> deals = new ArrayList<>();
        dealRepository.findAll().forEach(deals::add);
        return deals;
    }

    public DealEntity getDeal(Long id) {
        return dealRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "딜을 찾을 수 없습니다."));
    }

    public DealEntity createDeal(DealRequest request) {
        validateDealRequest(request);

        LeadEntity lead = leadService.getLead(request.getLeadId());
        if (lead.getStatus() != LeadStatus.QUALIFIED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "QUALIFIED 상태의 리드만 딜로 전환할 수 있습니다. 현재 상태: " + lead.getStatus());
        }

        DealEntity deal = new DealEntity();
        deal.setTitle(request.getTitle().trim());
        deal.setAmount(request.getAmount());
        deal.setLeadId(request.getLeadId());
        deal.setStage(DealStage.PROSPECTING);
        deal.setClosingDate(request.getClosingDate());
        return dealRepository.save(deal);
    }

    public DealEntity updateStage(Long id, DealStage stage) {
        if (stage == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "단계 값은 필수입니다.");
        }

        DealEntity deal = getDeal(id);
        deal.setStage(stage);
        return dealRepository.save(deal);
    }

    public List<DealStageStats> getStageStats() {
        Map<DealStage, StageAmountSummary> summaryByStage = dealRepository.summarizeAmountByStage().stream()
                .collect(Collectors.toMap(StageAmountSummary::getStage, summary -> summary));

        return Arrays.stream(DealStage.values())
                .map(stage -> {
                    StageAmountSummary summary = summaryByStage.get(stage);
                    if (summary == null) {
                        return new DealStageStats(stage, BigDecimal.ZERO, 0L);
                    }
                    BigDecimal totalAmount = summary.getTotalAmount() != null
                            ? summary.getTotalAmount()
                            : BigDecimal.ZERO;
                    long dealCount = summary.getDealCount() != null ? summary.getDealCount() : 0L;
                    return new DealStageStats(stage, totalAmount, dealCount);
                })
                .toList();
    }

    private void validateDealRequest(DealRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 데이터가 없습니다.");
        }
        if (isBlank(request.getTitle())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "딜 제목은 필수입니다.");
        }
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "금액은 0보다 커야 합니다.");
        }
        if (request.getLeadId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "리드 ID는 필수입니다.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
