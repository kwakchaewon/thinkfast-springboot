package com.example.thinkfast.security;

import com.example.thinkfast.domain.auth.Role;
import com.example.thinkfast.domain.auth.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class UserDetailImpl implements UserDetails {
    private String username;
    private String password;
    private String email;
    private Role roles;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    public UserDetailImpl(String username, String password, Role roles) {
        this.username = username;
        this.password = password;
        this.roles = roles;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public static UserDetailImpl responderBuild(User user){
        return new UserDetailImpl(user.getUsername(), user.getPassword(), Role.RESPONDER);
    }
}
