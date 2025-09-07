package com.rajat.auth.security.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.data.cassandra.core.mapping.CassandraType;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;
import org.springframework.data.cassandra.core.mapping.Column;

import java.util.Set;
import java.util.HashSet;

/**
 * Represents a user in the system (Cassandra version).
 */
@Table("users") // Maps this class to Cassandra 'users' table
@Data
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
//  @CassandraType(type = CassandraType.Name.SET, typeArguments = CassandraType.Name.TEXT)
  private Set<String> roles = new HashSet<>();

  public User() {}

  public User(String username, String email, String password) {
    this.username = username;
    this.email = email;
    this.password = password;
  }
}
