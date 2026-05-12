package com.yjailbir.core.controller.rest;

import com.yjailbir.core.dto.PaymentDtoForFrontend;
import com.yjailbir.core.dto.TransactionDtoForFrontend;
import com.yjailbir.core.dto.TransactionDtoFromBank;
import com.yjailbir.core.service.TransactionChainService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


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

    @GetMapping("/payments")
    public ResponseEntity<List<PaymentDtoForFrontend>> getPayments(){
        return ResponseEntity.ok().body(transactionChainService.getAllPayments());
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionDtoForFrontend>> getTransactions(){
        return ResponseEntity.ok().body(transactionChainService.getAllTransactions());
    }


    @GetMapping("/payment")
    public ResponseEntity<PaymentDtoForFrontend> getPaymentById(@RequestParam UUID id){
        return ResponseEntity.ok().body(transactionChainService.getPaymentById(id));
    }

    @GetMapping("/transaction")
    public ResponseEntity<TransactionDtoForFrontend> getTransactionById(@RequestParam UUID id){
        return ResponseEntity.ok().body(transactionChainService.getTransactionById(id));
    }
}
