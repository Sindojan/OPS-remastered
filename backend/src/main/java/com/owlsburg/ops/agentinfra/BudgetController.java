package com.owlsburg.ops.agentinfra;

import com.owlsburg.ops.agentinfra.dto.BudgetOverviewResponse;
import com.owlsburg.ops.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/budget")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<BudgetOverviewResponse>> overview() {
        BudgetOverviewResponse overview = budgetService.getOverview();
        return ResponseEntity.ok(ApiResponse.ok(overview));
    }
}
