# Overview

This project is a comprehensive e-commerce order management system built with Java 21 and Javalin. It provides a robust REST API for managing products, shopping carts, orders, payments, and deliveries. The system features both a server-side HTML interface and a modern React frontend, supporting role-based authentication for customers, managers, and couriers. The business vision is to deliver a scalable, reliable e-commerce solution with a focus on efficient order processing and a rich user experience.

## Recent Updates (Sep 26, 2025)
- ✅ Complete checkout flow implemented with 3 steps:
  - Step 1: Address selection and management
  - Step 2: Payment method selection (Credit Card, Boleto, PIX)
  - Step 3: Order review with full summary
- ✅ Order confirmation process with payment simulation
- ✅ Notification system integrated
- ✅ Address management system with validations
- ✅ Authentication fixes for immediate login after registration

# User Preferences

Preferred communication style: Simple, everyday language.

# System Architecture

## Core Framework and Language
- **Java 21**: Modern Java features and performance.
- **Javalin 6.x**: Lightweight web framework for REST API development.
- **Gradle**: Build automation and dependency management.

## Data Persistence Layer
- **JPA/Hibernate**: Object-relational mapping.
- **HikariCP**: High-performance database connection pooling.
- **H2 Database**: In-memory database for development and testing.
- **Repository Pattern**: Data access abstraction.

## Authentication and Security
- **JWT-based Authentication**: Stateless authentication using Auth0 java-jwt.
- **Role-based Authorization**: Supports CUSTOMER, MANAGER, and COURIER roles.
- **Security Layer**: Centralized authentication and authorization handling.

## API Documentation and Development Tools
- **Javalin-OpenAPI**: Automated API documentation generation.
- **Swagger UI**: Interactive API documentation interface.
- **CORS Configuration**: Enabled for development.

## Code Quality and Productivity
- **MapStruct**: Compile-time mapping between DTOs and entities.
- **Lombok**: Boilerplate code reduction.
- **Layered Architecture**: Organized into Domain, DTO, Repository, Service, and Controller layers.

## Frontend Architecture
- **Dual Frontend Strategy**:
    - **Server-side Interface**: Traditional HTML/CSS/JavaScript with Tailwind CSS.
    - **React Frontend**: Modern SPA built with React 19, TypeScript, Vite, React Router, and TanStack Query.
- **Tailwind CSS**: Utility-first CSS framework for styling.

## Application Structure
- **App.java**: Main application entry point.
- **Global Error Handling**: Centralized exception management.
- **Route Organization**: RESTful endpoints by functionality.
- **Plugin System**: Javalin plugins for OpenAPI and Swagger.

## Business Logic Organization
- **Domain Layer**: Core entities and business logic.
- **Service Layer**: Business logic orchestration and transaction management.
- **DTO Layer**: Data transfer objects for API communication.
- **Controller/Routes Layer**: HTTP request handling.

## Mock Integration Services
- **PaymentService**: Simulated payment processing.
- **DeliveryService**: Mock delivery status tracking.
- **Interface-based Design**: Decoupled integrations.

# External Dependencies

## Backend Dependencies
- **Javalin 6.x**: Web framework.
- **Auth0 java-jwt**: JWT authentication.
- **JPA/Hibernate**: ORM framework.
- **HikariCP**: Database connection pooling.
- **H2 Database**: In-memory database.
- **MapStruct**: DTO mapping.
- **Lombok**: Code generation.
- **Javalin-OpenAPI**: API documentation.

## Frontend Dependencies (React)
- **React 19**: UI library.
- **React Router DOM**: Client-side routing.
- **TanStack React Query**: Server state management.
- **React Hook Form**: Form handling.
- **Zod**: Schema validation.
- **Headless UI**: Accessible UI components.
- **Heroicons**: Icon library.
- **Tailwind CSS**: CSS framework.
- **Vite**: Build tool.

## Development Tools
- **Gradle**: Build automation.
- **TypeScript**: Type-safe JavaScript.
- **ESLint**: Code linting.
- **PostCSS**: CSS processing.
- **Autoprefixer**: CSS vendor prefixing.

## Database and Storage
- **H2 Database**: Development and testing database.
- **JPA/Hibernate**: Database abstraction.
- **HikariCP**: Connection pooling.