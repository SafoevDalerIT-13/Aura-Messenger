package ru.messenger.user_service.api.mapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.messenger.user_service.api.dto.UserRequestDto;
import ru.messenger.user_service.api.dto.UserResponseDto;
import ru.messenger.user_service.domain.entity.UserEntity;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // Entity в ResponseDto
    UserResponseDto toResponseDto(UserEntity entity);

    // RequestDto в Entity (при создании)
    @Mapping(target = "id", ignore = true)
    UserEntity toEntity(UserRequestDto requestDto);
}