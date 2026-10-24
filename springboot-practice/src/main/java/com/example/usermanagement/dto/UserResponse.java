
package com.example.usermanagement.dto;

import com.example.usermanagement.entity.Role;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private Role role;
    private boolean enabled;

    public UserResponse(
            Long id,
            String name,
            String email,
            Role role,
            boolean enabled
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.enabled = enabled;
    }
}
