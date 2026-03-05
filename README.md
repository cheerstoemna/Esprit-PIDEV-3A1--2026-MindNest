# # MindNest — Full-Stack Application (PIDEV)

## Overview
**MindNest** is a full-stack application developed as part of the **PIDEV – 3rd Year Engineering Program** at **Esprit School of Engineering** (Academic Year **2025–2026**).
It centralizes multiple services in one ecosystem, combining a modern desktop interface with a secure backend API to deliver a complete, role-based experience.

## Features
- **Authentication & Security**
  - JWT-based authentication
  - Role-based access control (**Admin / Recruiter / Candidate**)

- **Extensible Multi-Module System**
  - Consistent UI theming across modules
  - Designed to easily expand with more modules (therapy, content, coaching, journaling)

## Tech Stack

### Frontend
- Java
- JavaFX (FXML + CSS)

### Backend
- Java
- Spring Boot (REST API)
- Spring Security + JWT
- MySQL

## Architecture
MindNest follows a **Client–Server** architecture:
- **JavaFX Desktop Client**
  - UI built with FXML + CSS
  - Controllers and client services consume REST APIs
- **Spring Boot Backend**
  - Layered structure: Controllers → Services → Repositories → Database
  - Security handled using Spring Security + JWT
- **Role-Based Access**
  - API endpoints protected by roles
  - UI adapts to permissions based on the logged-in user

## Contributors
- Ela Kalleli
- Emna Nasraoui
- Malak Slim
- Mohammed Chebbi
- Ahmed Rahmani

## Academic Context
Developed at **Esprit School of Engineering – Tunisia**  
**PIDEV – 3A | 2025–2026**

## Getting Started

### Prerequisites
- Java 17+
- MySQL or MariaDB
- Maven

### configure the Backend 
1. Configure DB connection (URL, username, password).
2. import the attached sql database

### Run the Frontend (JavaFX)
1. Open the JavaFX project.
2. Ensure the API base URL is correct inside the client services.
3. Run the JavaFX application from your IDE.


## Acknowledgments
- Esprit School of Engineering — PIDEV supervision and evaluation
- JavaFX, Spring Boot, Spring Security, and all open-source libraries used in the project
