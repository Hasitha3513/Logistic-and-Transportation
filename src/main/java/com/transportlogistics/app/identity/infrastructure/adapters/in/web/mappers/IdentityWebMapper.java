package com.transportlogistics.app.identity.infrastructure.adapters.in.web.mappers;

import com.transportlogistics.app.identity.domain.model.AuthTokens;
import com.transportlogistics.app.identity.domain.model.Role;
import com.transportlogistics.app.identity.domain.model.User;
import com.transportlogistics.app.identity.infrastructure.adapters.in.web.dto.response.AuthResponse;
import com.transportlogistics.app.identity.infrastructure.adapters.in.web.dto.response.RoleResponse;
import com.transportlogistics.app.identity.infrastructure.adapters.in.web.dto.response.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface IdentityWebMapper {

    AuthResponse toResponse(AuthTokens tokens);

    @Mapping(target = "roles", expression = "java(user.roleNames())")
    @Mapping(target = "permissions", expression = "java(user.permissions())")
    @Mapping(target = "roleIds", expression = "java(user.roleIds())")
    UserResponse toResponse(User user);

    List<UserResponse> toUserResponseList(List<User> users);

    RoleResponse toResponse(Role role);

    List<RoleResponse> toRoleResponseList(List<Role> roles);
}
