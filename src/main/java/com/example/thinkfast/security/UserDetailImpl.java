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

    @Override
    public boolean isAccountNonExpired() {
        return false;
    }

    public UserDetailImpl(String username, String password, Role roles) {
        this.username = username;
        this.password = password;
        this.roles = roles;
    }

    @Override
    public boolean isAccountNonLocked() {
        return false;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    public static UserDetails responderBuild(User user){
        UserDetailImpl userDetail = new UserDetailImpl(user.getUsername(), user.getPassword(), Role.RESPONDER);
        return userDetail;
    }
}
