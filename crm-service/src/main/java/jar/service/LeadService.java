package jar.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import jar.dto.LeadRequest;
import jar.entity.LeadEntity;
import jar.enums.LeadStatus;
import jar.repository.LeadRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LeadService {

    private final LeadRepository leadRepository;

    public List<LeadEntity> getAllLeads() {
        List<LeadEntity> leads = new ArrayList<>();
        leadRepository.findAll().forEach(leads::add);
        return leads;
    }

    public LeadEntity getLead(Long id) {
        return leadRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "리드를 찾을 수 없습니다."));
    }

    public LeadEntity createLead(LeadRequest request) {
        validateLeadRequest(request);

        LeadEntity lead = new LeadEntity();
        lead.setName(request.getName().trim());
        lead.setCompany(trimToNull(request.getCompany()));
        lead.setEmail(request.getEmail().trim());
        lead.setPhone(trimToNull(request.getPhone()));
        lead.setStatus(LeadStatus.NEW);
        return leadRepository.save(lead);
    }

    public LeadEntity updateStatus(Long id, LeadStatus status) {
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "상태 값은 필수입니다.");
        }

        LeadEntity lead = getLead(id);
        lead.setStatus(status);
        return leadRepository.save(lead);
    }

    private void validateLeadRequest(LeadRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "요청 데이터가 없습니다.");
        }
        if (isBlank(request.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이름은 필수입니다.");
        }
        if (isBlank(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일은 필수입니다.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
