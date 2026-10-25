package com.shakib.smartorder.auth.dto;

import com.shakib.smartorder.auth.entity.User;
import com.shakib.smartorder.auth.enums.Role;

public record UserResponse(Long id, String name, String email, Role role) {

    public static UserResponse from(User user) 
    {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
