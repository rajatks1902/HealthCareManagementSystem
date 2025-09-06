package com.rajat.auth.security.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.data.cassandra.core.mapping.Column;

import java.util.Set;
import java.util.HashSet;

/**
 * Represents a user in the system (Cassandra version).
 */
@Table("users") // Maps this class to Cassandra 'users' table
public class User {

  @PrimaryKey
  private String id;

  @NotBlank
  @Size(max = 20)
  @Column("username")
  private String username;

  @NotBlank
  @Size(max = 50)
  @Email
  @Column("email")
  private String email;

  @NotBlank
  @Size(max = 120)
  @Column("password")
  private String password;

  @Column("roles")
  private Set<String> roles = new HashSet<>();

  public User() {}

  public User(String username, String email, String password) {
    this.username = username;
    this.email = email;
    this.password = password;
  }

  // Getters and setters
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public Set<String> getRoles() {
    return roles;
  }

  public void setRoles(Set<String> roles) {
    this.roles = roles;
  }
}
