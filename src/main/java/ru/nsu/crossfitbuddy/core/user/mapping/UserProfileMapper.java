package ru.nsu.crossfitbuddy.core.user.mapping;

import org.mapstruct.Mapper;
import ru.nsu.crossfitbuddy.core.user.api.UserProfileResponse;
import ru.nsu.crossfitbuddy.core.user.domain.UserProfile;
import ru.nsu.crossfitbuddy.shared.mapping.CentralMapperConfig;

@Mapper(config = CentralMapperConfig.class)
public interface UserProfileMapper {
    UserProfileResponse toResponse(UserProfile profile);
}
