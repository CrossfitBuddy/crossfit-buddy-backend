package ru.nsu.crossfitbuddy.core.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.nsu.crossfitbuddy.core.dto.response.UserProfileResponse;
import ru.nsu.crossfitbuddy.core.entity.UserProfile;

@Mapper(config = CentralMapperConfig.class)
public interface UserProfileMapper {
    @Mapping(target = "email", source = "user.email")
    UserProfileResponse toResponse(UserProfile profile);
}

