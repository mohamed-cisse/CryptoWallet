package com.assignment.cryptowallet.repository;

import com.assignment.cryptowallet.model.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssetRepository extends JpaRepository<Asset, Long> {}
