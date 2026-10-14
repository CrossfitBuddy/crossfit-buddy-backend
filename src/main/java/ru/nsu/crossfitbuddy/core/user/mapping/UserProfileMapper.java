package ru.nsu.crossfitbuddy.core.user.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import ru.nsu.crossfitbuddy.core.user.api.UserProfileResponse;
import ru.nsu.crossfitbuddy.core.user.domain.UserProfile;
import ru.nsu.crossfitbuddy.shared.mapping.CentralMapperConfig;

@Mapper(config = CentralMapperConfig.class)
public interface UserProfileMapper {
    @Mapping(target = "email", source = "user.email")
    UserProfileResponse toResponse(UserProfile profile);
}
