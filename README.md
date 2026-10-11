Smart Expense Tracker API is a secure RESTful backend application that helps users manage their daily expenses efficiently. 
The application provides user authentication using JWT and allows every user to securely create,  update, delete, and view only their own expense records.

The project follows a clean layered architecture using Spring Boot best practices and demonstrates secure API development with role-based authentication, 
database integration, and RESTful design.

Client (Postman)

        │

        ▼

Controller Layer
        │

        ▼

Service Layer
        │

        ▼

Repository Layer
        │

        ▼

MySQL Database


⚙ Tech Stack :

| Technology      | Purpose                           |
| --------------- | --------------------------------- |
| Java 21         | Programming Language              |
| Spring Boot     | Backend Framework                 |
| Spring Security | Authentication & Authorization    |
| JWT             | Secure Token-Based Authentication |
| Spring Data JPA | ORM                               |
| Hibernate       | Database Mapping                  |
| MySQL           | Database                          |
| Maven           | Dependency Management             |
| Postman         | API Testing                       |


📂 Project Structure

src
 ├── controller
 ├── service
 ├── repository
 ├── entity
 ├── dto
 ├── security
 ├── configuration
 ├── exception
 └── util

🚀 Features

🔐 Authentication & Authorization
• User Registration
• User Login
• JWT Token Generation
• Spring Security Integration
• Password Encryption using BCrypt
• Protected REST APIs
• Users can access only their own expense records

💵 Expense Management

• Add New Expense
• Update Existing Expense
• Delete Expense
• View All Expenses
• View Expense by ID

🔍 Filtering

Users can filter expenses based on:
 • Category
 • Date Range
 • Multiple search combinations

 Example:
 • Food expenses
 • Travel expenses
 • Expenses between two dates

✅ Validation

Implemented request validation for:
• Empty fields
• Invalid amount
• Invalid category
• Missing required fields

🔐 Authentication Flow

Register User
      │
      ▼
Login
      │
      ▼
Generate JWT Token
      │
      ▼
Send Token in Authorization Header

Authorization:
Bearer <JWT_TOKEN>

      │
      ▼
Access Protected APIs


📖 Key Learnings

During this project, I gained practical experience in:

 • Building secure REST APIs using Spring Boot
 • Implementing JWT-based authentication and authorization
 • Designing layered backend architecture
 • Using Spring Security for endpoint protection
 • Database integration with Spring Data JPA
 • Exception handling and request validation
 • API testing using Postman

🚀 Future Improvements

Expense Analytics Dashboard
Monthly Expense Reports
Budget Management
Email Notifications
Export Reports to PDF/Excel
Docker Containerization
Swagger/OpenAPI Documentation
Unit & Integration Testing





 


🖥 Web Frontend (Thymeleaf)

A server-rendered web UI now sits on top of the same service layer as the REST API.

Pages
• /login, /register   – sign in or create an account
• /expenses           – dashboard: total, spending-by-category bar, filters (category, date range), expense list
• /expenses/new       – add an expense
• /expenses/{id}/edit – edit an expense (delete is a button on the list)

How authentication works
• The REST API (/api/**) is unchanged: stateless, JWT in the Authorization header, no CSRF.
• The web UI uses a second security filter chain: form login, an HTTP session cookie and CSRF protection.
• Both use the same users table and BCrypt passwords, and every query is scoped to the signed-in user.

Run it

1. Install Java 21 and MySQL, then set (or edit in application.properties):
   DB_URL, DB_USERNAME, DB_PASSWORD and JWT_SECRET (32+ characters)
2. mvn spring-boot:run
3. Open http://localhost:8080  (the REST API stays at /api/**)

Optional setting: app.currency-symbol=$ in application.properties

Project layout

    pom.xml
    src/main/java/com/expensetracker/
        config/  controller/  dto/  exception/  model/  repository/  security/  service/
        web/                        <- Thymeleaf controllers
    src/main/resources/
        application.properties
        messages.properties
        templates/                  <- login, register, expenses, expense-form, error, fragments/layout
        static/css/style.css
