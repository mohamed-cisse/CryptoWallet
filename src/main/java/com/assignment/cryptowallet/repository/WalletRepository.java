package com.assignment.cryptowallet.repository;

import com.assignment.cryptowallet.model.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, Long> {}
