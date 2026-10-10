package test.core.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import test.core.dto.UserRequest;
import test.core.dto.UserResponse;
import test.core.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);

    @Mapping(target = "id", ignore = true)
    User toEntity(UserRequest request);
}
