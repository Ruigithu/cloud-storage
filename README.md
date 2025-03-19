# Cloud Storage Project Technical Documentation

## 1. Project Overview

**Project Name**: Cloud Storage  
**Description**: Cloud Storage is a full-stack cloud storage system based on Spring Boot and React, supporting user registration, login, file upload, download, and other functions.  
**Project Goal**: Provide a secure, efficient, and user-friendly cloud file storage and collaboration platform to meet the file management needs of individuals and teams.

## 2. Technology Stack

### Backend
- **Language**: Java 17
- **Framework**: Spring Boot 3.4.1
- **Database**: PostgreSQL 42.7.5
- **Persistence**: MyBatis
- **Security**: Spring Security
- **Real-time Communication**: WebSocket
- **Document Processing**: Apache POI (Supporting DOC/DOCX parsing)
- **Build Tool**: Maven 3.9.0
- **File Storage**: Local File System

### Frontend
- **Language**: JavaScript (ES6+)
- **Framework**: React 18
- **State Management**: Redux Toolkit
- **UI Library**: Material UI 5.14.0
- **Rich Text Editor**: Quill
- **Routing**: React Router 6.10.0
- **Real-time Communication**: Socket.io-client
- **Build Tool**: Vite 4.3.9

## 3. Main Features

### Backend Features
- **User Management**: User registration, login, permission management
- **File Management**: File upload, download, delete, version control
- **File Sharing**: Generate sharing links with permission control and expiration settings
- **Security**: Spring Security access control
- **File Processing**: Support for document preview, metadata extraction

### Frontend Features
- **User Interface**: Modern UI design
- **File Management**: Intuitive file upload and download interface
- **Search**: Quick file search by name
- **Real-time Collaboration**: Rich text editor supporting online editing

## 4. Architecture Design

This project adopts a frontend-backend separation architecture, with the frontend built using React for UI and the backend providing RESTful APIs based on Spring Boot.

### System Architecture Diagram
```
┌─────────────┐      ┌─────────────────────────────┐      ┌─────────────┐
│             │      │                             │      │             │
│   React     │◄────►│   Spring Boot (REST API)    │◄────►│  PostgreSQL │
│  Frontend   │      │                             │      │  Database   │
│             │      └─────────────────────────────┘      │             │
└─────────────┘                    ▲                      └─────────────┘
                                   │                             
                                   ▼                             
                            ┌─────────────┐                      
                            │             │                      
                            │  Local File │                      
                            │   System    │                      
                            │             │                      
                            └─────────────┘
```

- **Frontend**: Access backend APIs via Fetch/Axios, communicate with WebSocket for real-time functions
- **Backend**: Use Spring Boot MVC structure to provide APIs for the frontend
- **Database**: Use PostgreSQL to store file metadata and user information
- **File Storage**: Store file content in the local file system, with database records tracking file locations
- **Security**: Use Spring Security for authentication and permission management

## 5. API Documentation

### Core API Endpoints

| Endpoint | Method | Description | Authentication Required |
|----------|--------|-------------|------------------------|
| `/signup` | POST | User registration | No                     |
| `/login` | POST | User login | No                     |
| `/getAllFiles` | GET | Get file list | Yes                    |
| `/getFileByUserIdAndFileId` | GET | Get single file information | Yes                    |
| `/upload` | POST | Upload file | Yes                    |
| `/deleteFile` | DELETE | Delete file | Yes                    |
| `/createShareLink` | POST | Create sharing link | Yes                    |
| `/share/{shareId}` | GET | Access shared file | Yes                    |

Complete API documentation can be accessed via Swagger UI: `http://localhost:8080/swagger-ui.html`

## 6. Deployment Guide

### Environment Requirements
- JDK 17 or above
- Maven 3.8+
- Node.js 16+
- PostgreSQL 14+
- Sufficient disk space for file storage

### Backend Deployment
1. Configure database and file storage
   ```
   # Database configuration
   spring.datasource.url=jdbc:postgresql://localhost:5432/cloud-storage
   spring.datasource.username=postgres
   spring.datasource.password=123456
   
   # File storage configuration
   file.storage.path=E:/data/cloud-storage/
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

4. Deploy the built files to Nginx or other web servers

## 7. Future Optimization Directions
- **Distributed Storage**: Implement MinIO or other object storage solutions for better scalability
- **Full-text Search**: Implement search based on file content and metadata using Elasticsearch
- **Real-time Collaboration**: Implement multi-user file editing via WebSocket, add conflict resolution mechanisms
- **Pagination**: Implement efficient pagination and virtual scrolling for file lists
- **Responsive Design**: Optimize mobile experience
- **CI/CD**: Implement GitHub Actions automated deployment
- **Logging and Monitoring**: Integrate ELK stack for log analysis and system monitoring
- **Advanced Security**: Implement JWT authentication, 2FA, OAuth 2.0 integration
- **Multi-language Support**: Internationalize the interface
- **Drag and Drop Support**: Add intuitive drag and drop functionality for file management
- **Theme Switching**: Implement light/dark mode theme switching
- **Testing Strategy**: Implement comprehensive testing including unit tests, integration tests, and performance tests