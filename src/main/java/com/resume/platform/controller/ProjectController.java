package com.resume.platform.controller;

import com.resume.platform.common.BusinessException;
import com.resume.platform.common.ErrorCode;
import com.resume.platform.common.Result;
import com.resume.platform.entity.Project;
import com.resume.platform.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

/**
 * 项目文件控制器
 * 安全规约重点：上传/下载/更新/删除前必须校验资源归属(userId)，防止越权访问他人文件
 *
 * @author system
 */
@Slf4j
@RestController
@RequestMapping("/api/project")
@CrossOrigin
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    /**
     * 数据正常状态
     */
    private static final int STATUS_NORMAL = 1;

    /**
     * 查询指定用户的项目文件列表
     * 安全说明：公开查询不做越权（和简历一样用户可公开查看），仅打印日志
     */
    @GetMapping("/user/{userId}")
    public Result<List<Project>> listProjects(@PathVariable Long userId) {
        log.debug("查询项目文件列表: userId={}", userId);
        List<Project> projects = projectService.getProjectsByUserId(userId);
        return Result.success(projects);
    }

    /**
     * 新增项目（非文件上传形式）
     */
    @PostMapping
    public Result<Project> createProject(@RequestBody Project project) {
        Long currentUserId = getCurrentUserId();
        log.info("新增项目: 登录userId={}, 请求userId={}", currentUserId, project.getUserId());
        if (!isAdminUser() && !currentUserId.equals(project.getUserId())) {
            log.warn("越权新增项目已拦截");
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
        Project created = projectService.createProject(project);
        return Result.success(created);
    }

    /**
     * 更新项目的名称/描述
     * 越权校验：admin任意；普通用户只能更新自己的项目
     */
    @PutMapping("/{id}")
    public Result<Project> updateProject(@PathVariable Long id, @RequestBody Project project) {
        Long currentUserId = getCurrentUserId();
        Project existing = getProjectAndCheckOwner(id, currentUserId);
        if (project.getName() != null) {
            existing.setName(project.getName());
        }
        if (project.getDescription() != null) {
            existing.setDescription(project.getDescription());
        }
        Project updated = projectService.updateProject(existing);
        log.info("项目更新成功: projectId={}, userId={}", id, currentUserId);
        return Result.success(updated);
    }

    /**
     * 删除项目文件（连同服务器物理文件）
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteProject(@PathVariable Long id) {
        Long currentUserId = getCurrentUserId();
        Project project = getProjectAndCheckOwner(id, currentUserId);
        if (project.getFileUrl() != null && !project.getFileUrl().isEmpty()) {
            deleteUploadedFile(project.getFileUrl());
        }
        boolean deleted = projectService.deleteProject(id);
        log.info("项目删除成功: projectId={}, userId={}", id, currentUserId);
        return Result.success(deleted);
    }

    /**
     * 上传项目文件
     */
    @PostMapping("/upload")
    public Result<Project> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") Long userId,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description) {
        Long currentUserId = getCurrentUserId();
        log.info("上传文件: 登录userId={}, 请求userId={}, fileName={}, size={}",
                currentUserId, userId, file.getOriginalFilename(), file.getSize());
        if (!isAdminUser() && !currentUserId.equals(userId)) {
            log.warn("越权上传项目文件已拦截");
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "文件内容为空");
        }
        try {
            File uploadPath = new File(uploadDir);
            // 优先转成绝对路径，避免相对路径写入启动目录（宝塔下通常无写权限）
            if (!uploadPath.isAbsolute()) {
                File absolute = new File(System.getProperty("user.dir"), uploadDir);
                log.warn("上传目录配置为相对路径: {}, 已转为绝对路径: {}", uploadPath, absolute.getAbsolutePath());
                uploadPath = absolute;
            }
            if (!uploadPath.exists()) {
                boolean mkdirOk = uploadPath.mkdirs();
                if (!mkdirOk) {
                    log.error("创建上传目录失败: path={}, exists={}, canWrite={}, parentWritable={}",
                            uploadPath.getAbsolutePath(),
                            uploadPath.exists(),
                            uploadPath.canWrite(),
                            uploadPath.getParentFile() == null ? "n/a" : uploadPath.getParentFile().canWrite());
                    throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED,
                            "上传目录创建失败，请联系管理员配置 file.upload-dir 为绝对路径");
                }
                log.info("上传目录已自动创建: {}", uploadPath.getAbsolutePath());
            }
            if (!uploadPath.canWrite()) {
                log.error("上传目录无写权限: path={}, canWrite={}", uploadPath.getAbsolutePath(), uploadPath.canWrite());
                throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "上传目录无写权限");
            }
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String newFilename = UUID.randomUUID().toString() + (extension.isEmpty() ? "" : "." + extension);

            Path filePath = Paths.get(uploadPath.getAbsolutePath(), newFilename);
            long bytesCopied = Files.copy(file.getInputStream(), filePath);
            if (bytesCopied <= 0) {
                log.error("文件写入字节数为0: path={}", filePath);
                try { Files.deleteIfExists(filePath); } catch (IOException ignored) {}
                throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "文件内容未写入");
            }

            String fileUrl = "/uploads/" + newFilename;
            String fileType = determineFileType(extension);
            long fileSize = file.getSize();

            Project project = new Project();
            project.setUserId(userId);
            project.setName(name != null && !name.isEmpty() ? name : (originalFilename != null ? originalFilename : "未命名文件"));
            project.setDescription(description);
            project.setFileUrl(fileUrl);
            project.setFileType(fileType);
            project.setFileSize(fileSize);
            project.setStatus(STATUS_NORMAL);
            log.info("文件写入磁盘成功，准备写DB: filePath={}, fileName={}, size={}, userId={}",
                    filePath, newFilename, fileSize, userId);
            Project created = projectService.createProject(project);
            log.info("文件上传成功: projectId={}, fileUrl={}", created.getId(), fileUrl);
            return Result.success(created);
        } catch (IOException e) {
            log.error("文件上传IO异常: uploadDir={}, reason={}", uploadDir, e.toString(), e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "IO异常: " + e.getMessage());
        } catch (Exception e) {
            // 兜底：写DB、越权校验等其他链路的异常，防止裸 50007 无法排查
            log.error("文件上传异常(非IO): uploadDir={}, type={}, reason={}",
                    uploadDir, e.getClass().getSimpleName(), e.toString(), e);
            String hint = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "上传失败: " + hint);
        }
    }

    /**
     * 下载文件
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        Long currentUserId;
        try {
            currentUserId = getCurrentUserId();
        } catch (BusinessException e) {
            log.warn("下载文件未登录: projectId={}", id);
            return ResponseEntity.status(401).build();
        }
        Project project = getProjectAndCheckOwner(id, currentUserId);
        if (project.getFileUrl() == null) {
            return ResponseEntity.notFound().build();
        }
        String filename = project.getName() + getFileExtensionFromUrl(project.getFileUrl());
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        String uploadPath = new File(uploadDir).getAbsolutePath();
        String filenameOnDisk = project.getFileUrl().replace("/uploads/", "");
        Path fileLocation = Paths.get(uploadPath, filenameOnDisk);
        if (!Files.exists(fileLocation)) {
            log.warn("下载的文件物理不存在: projectId={}, path={}", id, fileLocation);
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(fileLocation);
        log.info("文件下载: projectId={}, userId={}, filename={}", id, currentUserId, filename);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(getMediaType(project.getFileType())))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .contentLength(project.getFileSize())
                .body(resource);
    }

    /**
     * 预览文件（图片/PDF/文本等浏览器可直接内联显示的类型）
     * 注意：SecurityConfig已放行此接口permitAll，token支持从query参数获取以便浏览器新标签页预览
     */
    @GetMapping("/preview/{id}")
    public ResponseEntity<Resource> previewFile(@PathVariable Long id) {
        Project project = projectService.getById(id);
        if (project == null || project.getFileUrl() == null) {
            return ResponseEntity.notFound().build();
        }
        String uploadPath = new File(uploadDir).getAbsolutePath();
        String filenameOnDisk = project.getFileUrl().replace("/uploads/", "");
        Path fileLocation = Paths.get(uploadPath, filenameOnDisk);
        if (!Files.exists(fileLocation)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(fileLocation);
        log.debug("文件预览: projectId={}, fileType={}", id, project.getFileType());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(getMediaType(project.getFileType())))
                .body(resource);
    }

    // ================== 私有工具方法 ==================

    /**
     * 根据项目ID获取资源并校验归属
     * 规则：admin可操作任意；普通用户只能操作自己的项目
     */
    private Project getProjectAndCheckOwner(Long id, Long currentUserId) {
        Project project = projectService.getById(id);
        if (project == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        if (!isAdminUser() && !currentUserId.equals(project.getUserId())) {
            log.warn("越权访问项目已拦截: projectId={}, ownerUserId={}, accessUserId={}",
                    id, project.getUserId(), currentUserId);
            throw new BusinessException(ErrorCode.PERMISSION_DENIED);
        }
        return project;
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getDetails() == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Object details = authentication.getDetails();
        if (details instanceof Long) {
            return (Long) details;
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }

    /**
     * 是否admin角色
     */
    private boolean isAdminUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(granted -> "ROLE_admin".equals(granted.getAuthority()));
    }

    /**
     * 物理删除服务器上的上传文件
     */
    private void deleteUploadedFile(String fileUrl) {
        try {
            String uploadPath = new File(uploadDir).getAbsolutePath();
            String filenameOnDisk = fileUrl.replace("/uploads/", "");
            Path fileLocation = Paths.get(uploadPath, filenameOnDisk);
            Files.deleteIfExists(fileLocation);
        } catch (IOException e) {
            log.error("删除文件失败: fileUrl={}, error={}", fileUrl, e.getMessage());
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    private String getFileExtensionFromUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return "";
        }
        int lastDotIndex = fileUrl.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return fileUrl.substring(lastDotIndex);
    }

    private String determineFileType(String extension) {
        if (extension == null) {
            return "other";
        }
        switch (extension.toLowerCase()) {
            case "doc":
            case "docx":
                return "doc";
            case "pdf":
                return "pdf";
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
            case "bmp":
            case "svg":
                return "img";
            case "zip":
            case "rar":
            case "7z":
            case "tar":
            case "gz":
                return "archive";
            case "xls":
            case "xlsx":
                return "excel";
            case "ppt":
            case "pptx":
                return "ppt";
            case "txt":
            case "md":
            case "csv":
                return "text";
            default:
                return "other";
        }
    }

    private String getMediaType(String fileType) {
        switch (fileType != null ? fileType : "other") {
            case "img":
                return "image/*";
            case "pdf":
                return "application/pdf";
            case "doc":
                return "application/msword";
            case "excel":
                return "application/vnd.ms-excel";
            case "ppt":
                return "application/vnd.ms-powerpoint";
            case "archive":
                return "application/zip";
            case "text":
                return "text/plain; charset=UTF-8";
            default:
                return "application/octet-stream";
        }
    }
}
