package com.yjailbir.core.controller.rest;

import com.yjailbir.core.dto.TransactionDtoFromBank;
import com.yjailbir.core.service.TransactionChainService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TransactionsRestController {
    @Autowired
    private final TransactionChainService transactionChainService;

    @PostMapping("/add")
    public void add (@RequestBody List<TransactionDtoFromBank> list) {
        for (TransactionDtoFromBank dto : list) {
            transactionChainService.saveAndValidate(dto);
        }
    }

    @GetMapping("/payment")
    public ResponseEntity<>
}
