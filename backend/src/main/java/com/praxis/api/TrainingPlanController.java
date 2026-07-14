package com.praxis.api;

import com.praxis.config.AppProperties;
import com.praxis.dto.TrainingPlanDto;
import com.praxis.service.TrainingPlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/training-plan")
public class TrainingPlanController {

    private final TrainingPlanService trainingPlanService;
    private final AppProperties appProperties;

    public TrainingPlanController(TrainingPlanService trainingPlanService, AppProperties appProperties) {
        this.trainingPlanService = trainingPlanService;
        this.appProperties = appProperties;
    }

    @GetMapping
    public ResponseEntity<TrainingPlanDto> getLatest() {
        return trainingPlanService.getLatest(appProperties.chessCom().username())
                .map(TrainingPlanDto::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping("/generate")
    public ResponseEntity<TrainingPlanDto> generate() {
        TrainingPlanDto dto = TrainingPlanDto.from(
                trainingPlanService.generate(appProperties.chessCom().username()));
        return ResponseEntity.ok(dto);
    }
}
