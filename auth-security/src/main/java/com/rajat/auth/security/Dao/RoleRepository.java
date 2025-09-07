package com.rajat.auth.security.Dao;

import com.rajat.auth.security.models.Role;
import com.rajat.auth.security.models.User;
import com.rajat.auth.security.models.UserRole;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.Optional;

public interface RoleRepository extends CassandraRepository<Role, String> {

    Optional<Role> findByName(UserRole name);
}
