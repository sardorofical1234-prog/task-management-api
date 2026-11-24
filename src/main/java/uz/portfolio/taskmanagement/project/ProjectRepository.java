package uz.portfolio.taskmanagement.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("""
            select distinct project
            from Project project
            left join ProjectMember membership on membership.project = project
            where project.owner.id = :userId
               or membership.user.id = :userId
            order by project.createdAt desc
            """)
    List<Project> findVisibleToUser(@Param("userId") Long userId);
}
