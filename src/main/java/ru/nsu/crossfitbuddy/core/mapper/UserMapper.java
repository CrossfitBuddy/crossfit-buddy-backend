package ru.nsu.crossfitbuddy.core.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.nsu.crossfitbuddy.core.dto.request.UserRequest;
import ru.nsu.crossfitbuddy.core.dto.response.UserResponse;
import ru.nsu.crossfitbuddy.core.entity.AppUser;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(AppUser appUser);

    @Mapping(target = "id", ignore = true)
    AppUser toEntity(UserRequest request);
}
