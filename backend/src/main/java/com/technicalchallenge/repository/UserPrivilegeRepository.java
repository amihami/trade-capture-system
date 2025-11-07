package com.technicalchallenge.repository;

import com.technicalchallenge.model.UserPrivilege;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPrivilegeRepository extends JpaRepository<UserPrivilege, Long> {

    @Query(value = """
            select case when count(*) > 0 then true else false end
            from user_privilege up
            join privilege p on p.id = up.privilege_id
            where up.user_id = :userId
              and lower(p.name) = lower(:privName)
            """, nativeQuery = true)
    boolean existsByUserIdAndPrivilegeNameIgnoreCase(@Param("userId") Long userId,
                                                     @Param("privName") String privilegeName);
}