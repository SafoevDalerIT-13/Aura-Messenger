package ru.messenger.user_service.contact_service;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ContactMapper {

    ContactResponseDto toResponse(ContactEntity entity);

    ContactEntity toEntity(ContactRequestDto request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget ContactEntity entity, ContactRequestDto request);
}