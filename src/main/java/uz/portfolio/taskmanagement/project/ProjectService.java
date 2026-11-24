package uz.portfolio.taskmanagement.project;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.portfolio.taskmanagement.auth.security.CurrentUserService;
import uz.portfolio.taskmanagement.project.dto.AddProjectMemberRequest;
import uz.portfolio.taskmanagement.project.dto.CreateProjectRequest;
import uz.portfolio.taskmanagement.project.dto.ProjectMemberResponse;
import uz.portfolio.taskmanagement.project.dto.ProjectResponse;
import uz.portfolio.taskmanagement.project.dto.UpdateProjectRequest;
import uz.portfolio.taskmanagement.project.exception.ProjectMemberAlreadyExistsException;
import uz.portfolio.taskmanagement.project.exception.ProjectMemberNotFoundException;
import uz.portfolio.taskmanagement.project.exception.ProjectNotFoundException;
import uz.portfolio.taskmanagement.project.exception.ProjectOwnerRemovalException;
import uz.portfolio.taskmanagement.user.User;
import uz.portfolio.taskmanagement.user.UserRepository;
import uz.portfolio.taskmanagement.user.exception.UserNotFoundException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public ProjectService(
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService
    ) {
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    public List<ProjectResponse> findAll() {
        List<Project> projects = currentUserService.isAdmin()
                ? projectRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                : projectRepository.findVisibleToUser(currentUserService.getCurrentUser().getId());

        return projects.stream().map(ProjectResponse::from).toList();
    }

    @Transactional
    public ProjectResponse create(CreateProjectRequest request) {
        Project project = new Project(
                request.name().trim(),
                request.description(),
                currentUserService.getCurrentUser()
        );
        return ProjectResponse.from(projectRepository.save(project));
    }

    public ProjectResponse findById(Long projectId) {
        return ProjectResponse.from(getAccessibleProject(projectId));
    }

    @Transactional
    public ProjectResponse update(Long projectId, UpdateProjectRequest request) {
        Project project = getManageableProject(projectId);
        project.update(request.name().trim(), request.description());
        return ProjectResponse.from(projectRepository.save(project));
    }

    @Transactional
    public void delete(Long projectId) {
        projectRepository.delete(getManageableProject(projectId));
    }

    @Transactional
    public ProjectMemberResponse addMember(Long projectId, AddProjectMemberRequest request) {
        Project project = getManageableProject(projectId);
        User member = userRepository.findById(request.userId())
                .orElseThrow(() -> new UserNotFoundException(request.userId()));

        if (project.getOwner().getId().equals(member.getId())
                || projectMemberRepository.existsByProjectIdAndUserId(projectId, member.getId())) {
            throw new ProjectMemberAlreadyExistsException(member.getId(), projectId);
        }

        return ProjectMemberResponse.from(
                projectMemberRepository.saveAndFlush(new ProjectMember(project, member))
        );
    }

    @Transactional
    public void removeMember(Long projectId, Long userId) {
        Project project = getManageableProject(projectId);
        if (project.getOwner().getId().equals(userId)) {
            throw new ProjectOwnerRemovalException();
        }

        ProjectMember membership = projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ProjectMemberNotFoundException(userId, projectId));
        projectMemberRepository.delete(membership);
    }

    public List<ProjectMemberResponse> findMembers(Long projectId) {
        getAccessibleProject(projectId);
        return projectMemberRepository.findByProjectIdOrderByJoinedAtAsc(projectId)
                .stream()
                .map(ProjectMemberResponse::from)
                .toList();
    }

    public Project getAccessibleProject(Long projectId) {
        Project project = getProjectOrThrow(projectId);
        if (currentUserService.isAdmin()) {
            return project;
        }

        User currentUser = currentUserService.getCurrentUser();
        if (isOwnerOrMember(project, currentUser)) {
            return project;
        }

        throw new ProjectNotFoundException(projectId);
    }

    public Project getManageableProject(Long projectId) {
        Project project = getProjectOrThrow(projectId);
        if (currentUserService.isAdmin()
                || project.getOwner().getId().equals(currentUserService.getCurrentUser().getId())) {
            return project;
        }
        throw new ProjectNotFoundException(projectId);
    }

    public boolean isOwnerOrMember(Project project, User user) {
        return project.getOwner().getId().equals(user.getId())
                || projectMemberRepository.existsByProjectIdAndUserId(project.getId(), user.getId());
    }

    private Project getProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
    }
}
