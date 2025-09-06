package com.rajat.auth.security.models;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

/**
 * Represents a role in the system.
 */
@Table("roles") // Maps this class to the 'roles' table in Cassandra
public class Role {

  @PrimaryKey
  private String id;

  private UserRole name;

  /**
   * Default constructor.
   */
  public Role() {}

  /**
   * Constructor with role name.
   *
   * @param name The role name.
   */
  public Role(UserRole name) {
    this.name = name;
  }

  // Getter and Setter for id
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  // Getter and Setter for name
  public UserRole getName() {
    return name;
  }

  public void setName(UserRole name) {
    this.name = name;
  }
}
