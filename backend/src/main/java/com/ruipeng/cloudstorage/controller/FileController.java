package com.ruipeng.cloudstorage.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruipeng.cloudstorage.config.file.FileStorageConfig;
import com.ruipeng.cloudstorage.entity.*;
import com.ruipeng.cloudstorage.entity.File;
import com.ruipeng.cloudstorage.entity.FilePermission;
import com.ruipeng.cloudstorage.mappers.FileMapper;
import com.ruipeng.cloudstorage.mappers.FilePermissionMapper;
import com.ruipeng.cloudstorage.mappers.FileVersionMapper;
import com.ruipeng.cloudstorage.service.*;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.docx4j.fonts.IdentityPlusMapper;
import org.docx4j.fonts.Mapper;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.converter.WordToHtmlConverter;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.docx4j.Docx4J;
import org.docx4j.convert.in.xhtml.XHTMLImporterImpl;
import org.docx4j.convert.out.HTMLSettings;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.zwobble.mammoth.internal.conversion.DocumentToHtml.convertToHtml;

@RestController
public class FileController {
    private final FilePermissionMapper filePermission;
    private final User user;
    private final FileVersionMapper fileVersionMapper;
    private final FileS3Service fileService;
    private final FileMapper fileMapper;
    private File file;
    private FileStorageConfig storage;
    private final FolderService folderService;
    private final S3StorageService s3StorageService;



    public FileController(FileS3Service fileService, File file ,
                          FileStorageConfig storage, FilePermissionMapper filePermission,
                          User user, FileVersionMapper fileVersionMapper,
                          FolderService folderService, FileMapper fileMapper,
                          S3StorageService s3StorageService) {
        this.fileService = fileService;
        this.file = file;
        this.storage = storage;
        this.filePermission = filePermission;
        this.user = user;
        this.fileVersionMapper = fileVersionMapper;
        this.folderService = folderService;
        this.fileMapper = fileMapper;
        this.s3StorageService = s3StorageService;
    }
    @GetMapping("/getRootFiles")
    public List<File> getRootFiles(@RequestParam Long ownerId) throws SQLException {
        Long rootFolderId = folderService.getRootFolderId(ownerId);
        return fileService.getFiles( ownerId,rootFolderId);
    }
    @GetMapping("/convertDocxToHtml")
    public ResponseEntity<?> convertDocxToHtml(
            @RequestParam("ownerId") long ownerId,
            @RequestParam("fileId") long fileId) {
        try {
            // 验证文件存在性和权限
            File existingFile = fileMapper.getFileById(fileId);
            if (existingFile == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File does not exist");
            }

            FilePermission permission = filePermission.findByFileIdAndUserId(fileId, ownerId);
            if (permission == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No permission to access this file");
            }

            // 获取最新版本的存储路径
            FileVersion latestVersion = fileVersionMapper.getLatestVersion(fileId);
            String storagePath = latestVersion.getStoragePath();

            // 从S3下载DOCX文件
            byte[] docxBytes = s3StorageService.downloadFile(storagePath);

            // 使用Mammoth库转换DOCX为HTML
            String htmlContent = convertDocxToHtmlUsingMammoth(docxBytes);

            // 创建响应
            Map<String, Object> response = new HashMap<>();
            response.put("htmlContent", htmlContent);
            response.put("fileName", existingFile.getName());
            response.put("mimeType", existingFile.getMimeType());
            response.put("fileId", fileId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to convert DOCX to HTML: " + e.getMessage());
        }
    }

    private String convertDocxToHtmlContent(byte[] docxInputStream) throws Exception {
        // 使用docx4j库转换DOCX为HTML
        WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(convertBytesToDocxFile(docxInputStream));

        // 设置字体映射器来处理缺失字体问题
        IdentityPlusMapper fontMapper = new IdentityPlusMapper();
        wordMLPackage.setFontMapper(fontMapper);

        // 配置HTML输出选项
        HTMLSettings htmlSettings = Docx4J.createHTMLSettings();
        htmlSettings.setWmlPackage(wordMLPackage);
        htmlSettings.setImageDirPath(null); // 不保存图片
        htmlSettings.setImageTargetUri(null);

        // 禁用CSS处理以便更好地与Quill兼容
            htmlSettings.setUserCSS(String.valueOf(false));

        // 设置输出方法
        htmlSettings.setOpcPackage(wordMLPackage);

        // 简化HTML输出并提高与Quill的兼容性
        ByteArrayOutputStream htmlOutputStream = new ByteArrayOutputStream();

        // 使用FLAG_EXPORT_PREFER_NONXSLT可能会减少问题
        Docx4J.toHTML(htmlSettings, htmlOutputStream, Docx4J.FLAG_EXPORT_PREFER_NONXSL);


        // 获取HTML内容
        String htmlContent = htmlOutputStream.toString("UTF-8");

        // 清理HTML，只保留body内容以便与Quill兼容
        htmlContent = extractBodyContent(htmlContent);

        // 执行额外的HTML清理
        htmlContent = cleanHtmlForQuill(htmlContent);

        return htmlContent;
    }

    // 提取body内容，确保只获取有用的部分
    private String extractBodyContent(String htmlContent) {
        // 先尝试提取<body>标签内的内容
        Pattern bodyPattern = Pattern.compile("<body[^>]*>(.*?)</body>", Pattern.DOTALL);
        Matcher bodyMatcher = bodyPattern.matcher(htmlContent);
        if (bodyMatcher.find()) {
            return bodyMatcher.group(1);
        }

        // 如果没有body标签，返回原始内容
        return htmlContent;
    }

    // 清理HTML内容使其适合Quill编辑器
    private String cleanHtmlForQuill(String htmlContent) {
        // 移除docx4j生成的不需要的样式和脚本
        htmlContent = htmlContent.replaceAll("<style[^>]*>.*?</style>", "");
        htmlContent = htmlContent.replaceAll("<script[^>]*>.*?</script>", "");

        // 移除Word特有的XML命名空间属性
        htmlContent = htmlContent.replaceAll(" xmlns:w=\"[^\"]*\"", "");
        htmlContent = htmlContent.replaceAll(" xmlns:o=\"[^\"]*\"", "");

        // 简化样式属性，移除复杂的样式定义
        htmlContent = htmlContent.replaceAll(" style=\"[^\"]*\"", "");
        htmlContent = htmlContent.replaceAll(" class=\"[^\"]*\"", "");

        // 处理HTML实体
        htmlContent = htmlContent.replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&apos;", "'")
                .replace("&quot;", "\"");

        // 处理特殊Unicode字符
        htmlContent = htmlContent.replace("\u2018", "'")
                .replace("\u2019", "'")
                .replace("\u201C", "\"")
                .replace("\u201D", "\"")
                .replace("\u2013", "-")
                .replace("\u2014", "--");

        // 确保标签正确闭合
        htmlContent = htmlContent.replaceAll("<br\\s*>", "<br/>");
        htmlContent = htmlContent.replaceAll("<hr\\s*>", "<hr/>");
        htmlContent = htmlContent.replaceAll("<img([^>]*)>", "<img$1/>");

        // 移除空段落和多余的换行
        htmlContent = htmlContent.replaceAll("<p>\\s*</p>", "");
        htmlContent = htmlContent.replaceAll("\\n\\s*\\n", "\n");

        // 移除Word特有的注释标记
        htmlContent = htmlContent.replaceAll("<!--\\[if.*?\\]>.*?<!\\[endif\\]-->", "");

        return htmlContent;
    }
    public java.io.File convertBytesToDocxFile(byte[] docxBytes) throws IOException {
        // Create a temporary file with the .docx extension
       java.io.File docxFile = java.io.File.createTempFile("document_", ".docx");
        docxFile.deleteOnExit(); // This will delete the file when the JVM exits

        // Write the bytes to the file
        try (FileOutputStream fos = new FileOutputStream(docxFile)) {
            fos.write(docxBytes);
            fos.flush();
        }

        return docxFile;
    }


    @PostMapping("/convertHtmlToDocx")
    public ResponseEntity<?> convertHtmlToDocx(@RequestBody Map<String, String> requestBody) {
        try {
            String htmlContent = requestBody.get("htmlContent");
            String fileName = requestBody.get("fileName");
            long ownerId = Long.parseLong(requestBody.get("ownerId"));
            long fileId = Long.parseLong(requestBody.get("fileId"));

            if (htmlContent == null || fileName == null) {
                return ResponseEntity.badRequest().body("HTML content and file name are required");
            }

            // 1. Validate file exists and check permissions
            File existingFile = fileMapper.getFileById(fileId);
            if (existingFile == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File does not exist");
            }

            FilePermission permission = filePermission.findByFileIdAndUserId(fileId, ownerId);
            if (permission == null || permission.getPermission() != PermissionType.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No permission to update this file");
            }

            // 2. Create Word document with better formatting preservation
            WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.createPackage();
            MainDocumentPart mainDocumentPart = wordMLPackage.getMainDocumentPart();

            //Improved font mapping for better text appearance
           Mapper fontMapper = new IdentityPlusMapper();
            wordMLPackage.setFontMapper(fontMapper);

            // Clean and format HTML
            String cleanedHtml = "<html><head><meta charset=\"UTF-8\"/></head><body>" + htmlContent.replace("&nbsp;", "&#160;")
                    .replace("&amp;", "&#38;")
                    .replace("&lt;", "&#60;")
                    .replace("&gt;", "&#62;")
                    .replace("<br>", "<br/>") + "</body></html>";

            // Configure XHTML importer with improved image handling
            XHTMLImporterImpl xhtmlImporter = new XHTMLImporterImpl(wordMLPackage);
            xhtmlImporter.setHyperlinkStyle("Hyperlink");

            // Convert HTML to DOCX content with improved formatting preservation
            mainDocumentPart.getContent().addAll(xhtmlImporter.convert(cleanedHtml, null));

            // Save document to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Docx4J.save(wordMLPackage, outputStream, Docx4J.FLAG_SAVE_ZIP_FILE);
            byte[] docxBytes = outputStream.toByteArray();

            // 3. 获取最新版本号并递增
            int currentVersionNumber = fileVersionMapper.getLatestVersionNumber(fileId);
            int newVersionNumber = currentVersionNumber + 1;

            // 4. 获取最近版本的存储路径
            FileVersion latestVersion = fileVersionMapper.getLatestVersion(fileId);
            String previousPath = latestVersion.getStoragePath();

            // 5. 生成新的S3密钥，替换版本号部分
            String extension = ".docx";
            String s3Key;
            if (previousPath.contains("_v")) {
                s3Key = previousPath.replaceAll("_v\\d+\\.", "_v" + newVersionNumber + ".");
            } else {
                // 如果路径中没有版本模式，则创建一个新的
                String basePath = previousPath.substring(0, previousPath.lastIndexOf('.'));
                s3Key = basePath + "_v" + newVersionNumber + extension;
            }

            MultipartFile multipartFile = new MockMultipartFile(
                    fileName,                // 文件名
                    file.getName(),        // 原始文件名
                    file.getMimeType(),             // 内容类型，如 "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    new FileInputStream(convertBytesToDocxFile(docxBytes))  // 文件内容
            );
            // 6. 上传文件到S3
            s3StorageService.uploadFile( multipartFile, s3Key);

            // 7. 插入新的文件版本记录
            FileVersion version = new FileVersion();
            version.setFileId(fileId);
            version.setVersionNumber(newVersionNumber);
            version.setStoragePath(s3Key);
            version.setSize((long) docxBytes.length);
            version.setCreatedBy(ownerId);
            version.setCreatedAt(Instant.now());
            fileVersionMapper.insertVersion(version);

            // 8. 更新文件元数据
            existingFile.setMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            existingFile.setSize((long) docxBytes.length);
            existingFile.setUpdatedAt(Instant.now());
            if (!fileName.endsWith(".docx")) {
                existingFile.setName(fileName + ".docx");
            } else {
                existingFile.setName(fileName);
            }
            fileMapper.updateFile(existingFile);

            // 9. 创建带有文件详细信息的响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Word document created successfully");
            response.put("fileName", existingFile.getName());
            response.put("mimeType", existingFile.getMimeType());
            response.put("fileId", fileId);
            response.put("versionId", version.getId());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to convert HTML to DOCX: " + e.getMessage());
        }
    }

    @GetMapping("/convertDocxToHtmlMammoth")
    public ResponseEntity<?> convertDocxToHtmlMammoth(
            @RequestParam("ownerId") long ownerId,
            @RequestParam("fileId") long fileId) {
        try {
            // 验证文件存在性和权限
            File existingFile = fileMapper.getFileById(fileId);
            if (existingFile == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File does not exist");
            }

            FilePermission permission = filePermission.findByFileIdAndUserId(fileId, ownerId);
            if (permission == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No permission to access this file");
            }

            // 获取最新版本的存储路径
            FileVersion latestVersion = fileVersionMapper.getLatestVersion(fileId);
            String storagePath = latestVersion.getStoragePath();

            // 从S3下载DOCX文件
            byte[] docxBytes = s3StorageService.downloadFile(storagePath);

            // 使用Mammoth库转换DOCX为HTML
            String htmlContent = convertDocxToHtmlUsingMammoth(docxBytes);

            // 创建响应
            Map<String, Object> response = new HashMap<>();
            response.put("htmlContent", htmlContent);
            response.put("fileName", existingFile.getName());
            response.put("mimeType", existingFile.getMimeType());
            response.put("fileId", fileId);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to convert DOCX to HTML: " + e.getMessage());
        }
    }
    // 在FileController.java中添加这个替代方法
    private String convertDocxToHtmlUsingMammoth(byte[] docxBytes) throws Exception {
        // 创建临时文件
        java.io.File docxFile = convertBytesToDocxFile(docxBytes);

        // 使用Mammoth库进行转换，遵循正确的样式映射语法
        org.zwobble.mammoth.DocumentConverter converter = new org.zwobble.mammoth.DocumentConverter()
                // 段落样式映射
                .addStyleMap("p[style-name='Heading 1'] => h1:fresh")
                .addStyleMap("p[style-name='Heading 2'] => h2:fresh")
                .addStyleMap("p[style-name='Heading 3'] => h3:fresh")
                .addStyleMap("p[style-name='Heading 4'] => h4:fresh")
                .addStyleMap("p[style-name='Heading 5'] => h5:fresh")
                .addStyleMap("p[style-name='Heading 6'] => h6:fresh")
                // 行内样式映射
                .addStyleMap("r[style-name='Strong'] => strong")
                .addStyleMap("r[style-name='Emphasis'] => em")
                .addStyleMap("r[style-name='Underline'] => u");

        // 执行转换
        org.zwobble.mammoth.Result<String> result = converter.convertToHtml(docxFile);
        String htmlContent = result.getValue();

        // 打印警告
        for (String warning : result.getWarnings()) {
            System.out.println("Mammoth warning: " + warning);
        }

        return htmlContent;
    }

    @PostMapping("/uploadNewFile")
    public ResponseEntity<?> uploadNewFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("ownerId") long ownerId,
            @RequestParam("fileId") long fileId) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Please select a file to upload");
            }

            // Call the file service to handle the upload
            File updatedFile = fileService.uploadNewVersion(file, ownerId, fileId);

            // Create a response with success details and file info
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "File uploaded successfully");
            response.put("fileId", updatedFile.getId());
            response.put("fileName", updatedFile.getName());
            response.put("mimeType", updatedFile.getMimeType());
            response.put("size", updatedFile.getSize());

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to upload file: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "An unexpected error occurred: " + e.getMessage()));
        }
    }


    @GetMapping("/getAllFiles")
    public ResponseEntity<List<File>> getAllFiles(@RequestParam long folderId,@RequestParam long ownerId) {
        if (folderId==-1){
            folderId=0;
        }
        List<File> files = fileService.getFiles(ownerId,folderId);

        return ResponseEntity.ok().body(files);
    }



    @GetMapping("/getFileByUserIdAndFileId")
    public ResponseEntity<?> getFileByUserIdAndFileId(@RequestParam long ownerId, @RequestParam long fileId) {
        try {
            // Get the latest version of the file
            FileVersion version = fileVersionMapper.getLatestVersion(fileId);
            if (version == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("File version not found for fileId: " + fileId);
            }

            // Get file metadata
            File file = fileMapper.getFileByUserIdAndFileId(ownerId, fileId);
            if (file == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("File not found for ownerId: " + ownerId + ", fileId: " + fileId);
            }

            // Check if file exists in storage
            String s3Key = version.getStoragePath();
            if (!s3StorageService.doesFileExist(s3Key)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("File not found in storage: " + s3Key);
            }

            // Download file content from S3
            byte[] fileContent = s3StorageService.downloadFile(s3Key);
            if (fileContent == null || fileContent.length == 0) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Empty file content for s3Key: " + s3Key);
            }

            // Determine content type and filename
            String contentType = file.getMimeType() != null ? file.getMimeType() : "application/octet-stream";
            String fileName = file.getName() != null ? file.getName() : s3Key.substring(s3Key.lastIndexOf('/') + 1);

            // Handle Word documents (.doc, .docx) - ensure correct MIME type
            if (s3Key.endsWith(".docx") && !contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
                contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            } else if (s3Key.endsWith(".doc") && !contentType.equals("application/msword")) {
                contentType = "application/msword";
            }

            // Create response body for JSON responses
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("fileName", fileName);
            responseBody.put("mimeType", contentType);
            responseBody.put("versionId", version.getId());
            responseBody.put("size", fileContent.length);

            // Handle different file types
            if (contentType.startsWith("text/") || contentType.equals("application/json")) {
                // Text files and JSON: Return content as string
                String textContent = new String(fileContent, StandardCharsets.UTF_8);
                if (contentType.equals("application/json")) {
                    try {
                        // Validate JSON
                        new ObjectMapper().readTree(textContent);
                    } catch (JsonProcessingException e) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body("Invalid JSON content: " + e.getMessage());
                    }
                }
                responseBody.put("content", textContent);
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(responseBody);
            } else if (isWordDocument(contentType, s3Key)) {
                try {
                    // Convert Word documents to HTML for editing while preserving formatting
                    String htmlContent;
                    // Check file format by content, not just extension
                    try (ByteArrayInputStream bais = new ByteArrayInputStream(fileContent);
                         PushbackInputStream pushbackInputStream = new PushbackInputStream(bais, 8)) {
                        if (isOle2Format(pushbackInputStream)) {
                            // Process .doc files
                            try (POIFSFileSystem fs = new POIFSFileSystem(pushbackInputStream);
                                 HWPFDocument document = new HWPFDocument(fs)) {
                                WordToHtmlConverter converter = new WordToHtmlConverter(
                                        DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument());
                                converter.processDocument(document);
                                StringWriter writer = new StringWriter();
                                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                                transformer.transform(
                                        new DOMSource(converter.getDocument()),
                                        new StreamResult(writer));
                                htmlContent = writer.toString();
                                htmlContent = extractBodyContent(htmlContent);
                            }
                        } else {
                            // Process .docx files
                            WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(pushbackInputStream);

                            // Configure HTML output options to preserve formatting
                            HTMLSettings htmlSettings = Docx4J.createHTMLSettings();
                            htmlSettings.setWmlPackage(wordMLPackage);
                            htmlSettings.setImageDirPath("images/");
                            htmlSettings.setImageTargetUri("images/");
                            htmlSettings.setUserCSS(String.valueOf(false)); // For Quill compatibility

                            // Convert to HTML
                            ByteArrayOutputStream htmlOutputStream = new ByteArrayOutputStream();
                            Docx4J.toHTML(htmlSettings, htmlOutputStream, Docx4J.FLAG_EXPORT_PREFER_XSL);
                            htmlContent = htmlOutputStream.toString("UTF-8");
                            htmlContent = extractBodyContent(htmlContent);
                        }
                    }

                    // Update response with HTML content for editing
                    responseBody.put("content", htmlContent);
                    responseBody.put("originalMimeType", contentType); // Keep track of original mime type

                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(responseBody);
                } catch (Exception e) {
                    e.printStackTrace();
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Failed to convert Word document to HTML: " + e.getMessage());
                }
            } else {
                // Binary files (images, PDFs, etc.): Return as download
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                        .body(fileContent);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error reading file from S3: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    // Helper method to check if the file is in OLE2 format (.doc)
    private boolean isOle2Format(PushbackInputStream inputStream) throws IOException {
        byte[] header = new byte[8];
        int read = inputStream.read(header);
        if (read >= 8) {
            inputStream.unread(header);
            // Check for OLE2 magic number (0xD0CF11E0)
            return header[0] == (byte) 0xD0 && header[1] == (byte) 0xCF &&
                    header[2] == (byte) 0x11 && header[3] == (byte) 0xE0;
        }
        inputStream.unread(header, 0, read);
        return false;
    }

    // Helper method to determine if the file is a Word document
    private boolean isWordDocument(String contentType, String s3Key) {
        return contentType.equals("application/msword") ||
                contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                s3Key.endsWith(".doc") || s3Key.endsWith(".docx");
    }


    @PostMapping("/upload")
    public ResponseEntity<File> uploadFile(@RequestParam("file") MultipartFile uploadFile,@RequestParam("userId")long ownerId,@RequestParam("folderId")long folderId) throws IOException {
        if (folderId==-1){
            folderId=0;
        }
        File uploadedFile = fileService.uploadFile(uploadFile, ownerId, folderId);

        return ResponseEntity.ok().body(uploadedFile);
    }
    @DeleteMapping("/deleteFile")
    public ResponseEntity<?> deleteFile(@RequestParam long fileId,@RequestParam long userId) throws IOException {

        int i = fileService.deleteFile(fileId, userId);
        if (i>0){
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.badRequest().build();
    }

    @DeleteMapping("/softDeleteFile")
    public ResponseEntity<?> softDeleteFile(@RequestParam long fileId,@RequestParam long userId) throws IOException {
        int i = fileService.softDeleteFile(fileId, userId);
        if (i>0){
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/getAllDeletedFiles")
    public ResponseEntity<List<File>> getAllDeletedFiles(@RequestParam long ownerId,@RequestParam long folderId) {

        if (folderId==-1){
            folderId=0;
        }
        List<File> files = fileService.getAllDeletedFiles(ownerId,folderId);

        return ResponseEntity.ok().body(files);
    }

    @PostMapping("/restoreFile")
    public ResponseEntity<?>restoreFile(@RequestParam long fileId,@RequestParam long ownerId) {
        int i = fileService.restoreFile(fileId, ownerId);

        if (i>0){
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }




     @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam("fileId") Long fileId) {
        try {
            DownloadFileInfo downloadInfo = fileService.downloadFile(fileId);

            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(downloadInfo.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + URLEncoder.encode(downloadInfo.getFileName(), "UTF-8") + "\"")
                .body(downloadInfo.getResource());
        } catch (Exception e) {
            throw new RuntimeException("fail downloading file " + e.getMessage());
        }
    }

    @PostMapping("/convert-doc")
    public ResponseEntity<?> convertDoc(@RequestParam("file") MultipartFile file) {
        try {
            //  Apache POI
            XWPFDocument document;
            if (file.getOriginalFilename().endsWith(".docx")) {
                document = new XWPFDocument(file.getInputStream());
            } else {
                HWPFDocument doc = new HWPFDocument(file.getInputStream());
                return ResponseEntity.ok(doc.getDocumentText());
            }


            StringBuilder text = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                text.append(paragraph.getText()).append("\n");
            }

            return ResponseEntity.ok(text.toString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("文档转换失败: " + e.getMessage());
        }
    }

}

