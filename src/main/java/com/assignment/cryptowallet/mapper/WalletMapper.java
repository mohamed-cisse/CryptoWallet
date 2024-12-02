package com.assignment.cryptowallet.mapper;

import com.assignment.cryptowallet.dto.AssetDto;
import com.assignment.cryptowallet.dto.WalletDto;
import com.assignment.cryptowallet.model.Asset;
import com.assignment.cryptowallet.model.Wallet;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WalletMapper {

    Wallet toEntity(WalletDto dto);

    WalletDto toDto(Wallet entity);
}
