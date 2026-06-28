package jar.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jar.dto.LeadRequest;
import jar.dto.LeadStatusRequest;
import jar.entity.LeadEntity;
import jar.service.LeadService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    @GetMapping
    public List<LeadEntity> getAllLeads() {
        return leadService.getAllLeads();
    }

    @GetMapping("/{id}")
    public LeadEntity getLead(@PathVariable Long id) {
        return leadService.getLead(id);
    }

    @PostMapping
    public ResponseEntity<LeadEntity> createLead(@RequestBody LeadRequest request) {
        LeadEntity saved = leadService.createLead(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PatchMapping("/{id}/status")
    public LeadEntity updateStatus(@PathVariable Long id, @RequestBody LeadStatusRequest request) {
        return leadService.updateStatus(id, request.getStatus());
    }
}
