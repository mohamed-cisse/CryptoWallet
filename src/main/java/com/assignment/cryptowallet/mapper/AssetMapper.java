package com.assignment.cryptowallet.mapper;

import com.assignment.cryptowallet.dto.AssetDto;
import com.assignment.cryptowallet.model.Asset;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface AssetMapper {

    Asset toEntity(AssetDto dto);

    AssetDto toDto(Asset entity);

}
