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

import jar.dto.DealRequest;
import jar.dto.DealStageRequest;
import jar.dto.DealStageStats;
import jar.entity.DealEntity;
import jar.service.DealService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/deals")
@RequiredArgsConstructor
public class DealController {

    private final DealService dealService;

    @GetMapping
    public List<DealEntity> getAllDeals() {
        return dealService.getAllDeals();
    }

    @GetMapping("/{id}")
    public DealEntity getDeal(@PathVariable Long id) {
        return dealService.getDeal(id);
    }

    @PostMapping
    public ResponseEntity<DealEntity> createDeal(@RequestBody DealRequest request) {
        DealEntity saved = dealService.createDeal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PatchMapping("/{id}/stage")
    public DealEntity updateStage(@PathVariable Long id, @RequestBody DealStageRequest request) {
        return dealService.updateStage(id, request.getStage());
    }

    @GetMapping("/dashboard/stats")
    public List<DealStageStats> getStageStats() {
        return dealService.getStageStats();
    }
}
