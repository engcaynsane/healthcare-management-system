package com.hms.auth.repository;

import com.hms.auth.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("select u from User u where (:search is null or lower(u.username) like lower(concat('%', :search, '%')) " +
            "or lower(u.fullName) like lower(concat('%', :search, '%')) " +
            "or lower(u.email) like lower(concat('%', :search, '%'))) " +
            "and (:roleCode is null or :roleCode in (select r.code from u.roles r)) " +
            "and (:branchId is null or u.branchId = :branchId)")
    Page<User> search(@Param("search") String search,
                      @Param("roleCode") String roleCode,
                      @Param("branchId") Long branchId,
                      Pageable pageable);
}