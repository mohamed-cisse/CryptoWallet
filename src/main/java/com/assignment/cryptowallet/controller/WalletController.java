package com.assignment.cryptowallet.controller;

import com.assignment.cryptowallet.dto.ResponseDto;
import com.assignment.cryptowallet.dto.WalletDto;
import com.assignment.cryptowallet.model.Wallet;
import com.assignment.cryptowallet.repository.WalletRepository;
import com.assignment.cryptowallet.service.CryptoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private CryptoService cryptoService;

    @PostMapping
    public ResponseEntity<ResponseDto> createWallet(@RequestBody WalletDto walletDto) {
        return ResponseEntity.ok(cryptoService.registerWallet(walletDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Wallet> getWallet(@PathVariable Long id) {
        Wallet wallet = walletRepository.findById(id).orElse(null);
        if (wallet == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(wallet);
    }
}
