package com.procurement.monitor.entity;

import java.io.Serializable;
import java.util.Objects;

public class UserRoleId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long roleId;

    public UserRoleId() {}

    public UserRoleId(Long userId, Long roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof UserRoleId id)) {
            return false;
        }

        return Objects.equals(userId, id.userId)
            && Objects.equals(roleId, id.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, roleId);
    }
}
