# Cloud Storage Project Technical Documentation

## 1. Project Overview

**Project Name**: Cloud Storage  
**Description**: Cloud Storage is a full-stack cloud storage system based on Spring Boot and React, supporting user registration, login, file upload, download, and other functions.  
**Project Goal**: Provide a secure, efficient, and user-friendly cloud file storage and collaboration platform to meet the file management needs of individuals and teams.
**Code Quality**: Follow Clean Code principles and constantly improve the code robustness, enhancing its scalability and maintainablity

## 2. Technology Stack

### Backend
- **Language**: Java 17
- **Framework**: Spring Boot 3.4.1
- **Database**: PostgreSQL 42.7.5
- **Persistence**: MyBatis
- **Security**: Spring Security
- **Document Processing**: Mammoth (Supporting DOCX parsing, not supporting Doc)
- **Build Tool**: Maven 3.9.0
- **File Storage**: AWS S3
- **Log**: SLF4J
- **Unit Test**:Junit 5

### Frontend
- **Language**: JavaScript (ES6+)
- **Framework**: React 18
- **UI Library**: Material UI 5.14.0
- **Rich Text Editor**: Quill
- **Routing**: React Router 6.10.0
- **Build Tool**: Webpack


## 3. Main Features

### Backend Features
- **User Management**: User registration, login, permission management
- **File/Folder Management**: File/Folder resumable upload, download, edit, version control,delete
- **File Sharing**: Generate sharing links with permission control and expiration settings
- **Security**: Spring Security access control, with JWT authentication
- **File Processing**: Support for conversion from docx to html and html to docx

### Frontend Features
- **User Interface**: Modern UI design,Google Drive mock up
- **File Management**: Intuitive file upload and download interface
- **File Edition**: Rich text editor supporting online editing

## 4. Architecture Design
This project adopts a frontend-backend separation architecture, with the frontend built using React for UI and the backend providing RESTful APIs based on Spring Boot.

### System Architecture Diagram
```
         ┌─────────────────────────────────────────────────────────────┐
         │                         Client Layer                        │
         │                    (React + Material-UI)                    │
         └──────────────────────────┬──────────────────────────────────┘
                                    │ HTTPS/REST API
                                    │ JWT Authentication
         ┌──────────────────────────▼──────────────────────────────────┐
         │                    Application Layer                        │
         │              (Spring Boot + Spring Security)                │
         │  ┌────────────┬──────────────┬──────────────┬─────────────┐│
         │  │Controllers │   Services   │  Repositories│   Security  ││
         │  └────────────┴──────────────┴──────────────┴─────────────┘│
         └──────────────────────────┬──────────────────────────────────┘
                                    │
                     ┌──────────────┼──────────────┐
                     │              │              │
         ┌───────────▼────┐  ┌──────▼──────┐  ┌───▼────────┐
         │   PostgreSQL   │  │    AWS S3   │  │   Redis    │
         │  (Metadata)    │  │  (Content)  │  │  (Future)  │
         └────────────────┘  └─────────────┘  └────────────┘
```

- **Frontend**: Access backend APIs via Fetch,with JWT authentication
- **Backend**: Use Spring Boot MVC structure to provide APIs for the frontend
- **Database**: Use PostgreSQL to store file metadata and user information
- **File Storage**: Store file content in AWS S3 bucket, with database records tracking file storage path
- **Security**: Use Spring Security for authentication and permission management

### Module Structure
```
backend/
├── controller/          # REST API endpoints
├── service/            # Business logic
│   ├── FileS3Service
│   ├── FolderS3Service
│   ├── ShareService
│   └── ...
├── mapper/             # MyBatis data access
├── entity/             # Domain models
├── dto/                # Data transfer objects
├── config/             # Configuration
│   └── security/       # Security config
├── exception/          # Custom exceptions
└── utils/              # Utilities
frontend/
├── src/
│   ├── components/     # React components
│   ├── pages/          # Page components
│   ├── services/       # API services
│   └── utils/          # Helper functions
└── public/             # Static assets
```

## 5. API Documentation

### Core API Endpoints
## API Documentation

### Authentication Endpoints

| Endpoint | Method | Description | Authentication Required |
|----------|--------|-------------|------------------------|
| `/api/auth/signup` | POST | User registration | No |
| `/api/auth/login` | POST | User login | No |
| `/getUserInfo` | GET | Get current user info | Yes |

### File Management Endpoints

| Endpoint | Method | Description | Authentication Required |
|----------|--------|-------------|------------------------|
| `/getRootFiles` | GET | Get root folder files | Yes |
| `/getAllFiles` | GET | Get files in folder | Yes |
| `/getFileByUserIdAndFileId` | GET | Get single file information | Yes |
| `/upload` | POST | Upload file | Yes |
| `/uploadNewFile` | POST | Upload new file version | Yes |
| `/download` | GET | Download file | Yes |
| `/softDeleteFile` | DELETE | Move file to trash | Yes |
| `/deleteFile` | DELETE | Permanently delete file | Yes |
| `/restoreFile` | POST | Restore file from trash | Yes |
| `/getAllDeletedFiles` | GET | Get deleted files | Yes |

### Folder Management Endpoints

| Endpoint | Method | Description | Authentication Required |
|----------|--------|-------------|------------------------|
| `/getRootFolders` | GET | Get root folders | Yes |
| `/createFolder` | POST | Create new folder | Yes |
| `/getAllFolders` | GET | Get subfolders | Yes |
| `/downloadFolder` | GET | Download folder as ZIP | Yes |
| `/uploadFolder` | POST | Upload folder | Yes |
| `/softDeleteFolder` | DELETE | Move folder to trash | Yes |
| `/deleteFolder` | DELETE | Permanently delete folder | Yes |
| `/restoreFolder` | POST | Restore folder from trash | Yes |

### File Sharing Endpoints

| Endpoint | Method | Description | Authentication Required |
|----------|--------|-------------|------------------------|
| `/createShareLink` | POST | Create file sharing link | Yes |
| `/share/{shareId}` | GET | Get shared file info | Yes |
| `/saveShare/{shareId}` | POST | Save shared file to storage | Yes |
| `/getAllSharedFiles` | GET | Get all user's shares | Yes |
| `/cancelShare` | POST | Deactivate share link | Yes |
| `/restore` | POST | Restore cancelled share | Yes |


### Document Conversion Endpoints

| Endpoint | Method | Description | Authentication Required |
|----------|--------|-------------|------------------------|
| `/convertDocxToHtml` | GET | Convert DOCX to HTML | Yes |
| `/convertDocxToHtmlMammoth` | GET | Convert DOCX to HTML (Mammoth) | Yes |
| `/convertHtmlToDocx` | POST | Convert HTML to DOCX | Yes |

### Resumable Upload Endpoints

| Endpoint | Method | Description | Authentication Required |
|----------|--------|-------------|------------------------|
| `/resumable/init` | POST | Initiate resumable upload | Yes |
| `/resumable/part` | POST | Upload file part | Yes |
| `/resumable/parts` | GET | List uploaded parts | Yes |
| `/resumable/complete` | POST | Complete upload | Yes |
| `/resumable/abort` | POST | Abort upload | Yes |

### Folder Multipart Upload Endpoints

| Endpoint | Method | Description | Authentication Required |
|----------|--------|-------------|------------------------|
| `/folders-initiate-upload` | POST | Initiate folder upload | Yes |
| `/folders-upload-part` | POST | Upload folder file part | Yes |
| `/folders-upload-status` | GET | Get upload status | Yes |
| `/folders-complete-upload` | POST | Complete folder upload | Yes |
| `/folders-abort-upload` | POST | Abort folder upload | Yes |

Complete API documentation can be accessed via Swagger UI: `http://localhost:8080/swagger-ui.html`

## 6. Deployment Guide

### Environment Requirements
- JDK 17 or above
- Maven 3.8+
- Node.js 16+
- PostgreSQL 14+
- Configure AWS S3

### Backend Deployment
1. Configure database and file storage
   ```
   # Database configuration
   spring.datasource.url=jdbc:postgresql://localhost:5432/cloud-storage
   spring.datasource.username=postgres
   spring.datasource.password=123456
   
   #S3 storage configuration
   cloud.aws.s3.bucket=${AWS_S3_BUCKET}
   ```

2. Build and run
   ```bash
   mvn clean package
   java -jar target/cloud-storage-0.0.1-SNAPSHOT.jar
   ```

3. Production environment considerations
   - Use environment variables or external configuration files to manage sensitive information
   - Configure appropriate log levels and output paths
   - Set up HTTPS and appropriate CORS policies
   - Ensure regular backups of both database and file storage directory

### Frontend Deployment
1. Install dependencies
   ```bash
   npm install
   ```

2. Run in development environment
   ```bash
   npm start
   ```

3. Build for production environment
   ```bash
   npm run build
   ```
4.CI/CD: Implement GitHub Actions automated deployment
5.Deploy the built files to Nginx or other web servers

## 7. Future Optimization Directions
- **Distributed Storage**: Implement MinIO or other object storage solutions for better scalability
- **Full-text Search**: Implement search based on file content and metadata using Elasticsearch
- **Real-time Collaboration**: Implement multi-user file editing via WebSocket, add conflict resolution mechanisms
- **Pagination**: Implement efficient pagination and virtual scrolling for file lists
- **Logging and Monitoring**: Integrate ELK stack for log analysis and system monitoring
- **Advanced Security**: Implement OAuth 2.0 integration
- **Drag and Drop Support**: Add intuitive drag and drop functionality for file management
- **Theme Switching**: Implement light/dark mode theme switching