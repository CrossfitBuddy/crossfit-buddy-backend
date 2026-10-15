package ru.nsu.crossfitbuddy.core.mapper;

import org.mapstruct.Mapper;
import ru.nsu.crossfitbuddy.core.dto.response.UserProfileResponse;
import ru.nsu.crossfitbuddy.core.entity.UserProfile;

@Mapper(config = CentralMapperConfig.class)
public interface UserProfileMapper {
    UserProfileResponse toResponse(UserProfile profile);
}
