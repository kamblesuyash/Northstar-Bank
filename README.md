# Bank Account Management System

A Java Enterprise student project demonstrating the complete request path:

**HTML/CSS/JavaScript → Jakarta Servlet → JDBC → MySQL 8**

The application uses plain Java Servlets (no Spring Boot), Maven, MySQL Connector/J and Apache Tomcat 10.1+. It is an academic demonstration and is not intended for real financial use.

## Features

- Account creation with a generated 12-digit account number and optional opening deposit.
- Password sign-in, session fixation protection, session timeout, sign-out and protected account pages.
- Deposit and withdrawal with server-side validation, balance locking and atomic transaction records.
- Balance, account details and the latest 100 transaction records.
- Password hashing using PBKDF2-HMAC-SHA-256; SQL uses prepared statements throughout.
- Responsive vanilla HTML, CSS and JavaScript interface.
- CSRF token validation for authenticated state-changing requests.

## Requirements

- JDK 17+
- Apache Maven 3.8+
- MySQL Server 8+
- Apache Tomcat 10.1+ (Jakarta Servlet 6)

## Configure the database

1. Run the SQL script in MySQL Workbench or the MySQL command line:

   ```sql
   SOURCE database/bank_management.sql;
   ```

2. Set the connection environment variables before starting Tomcat. Do not put credentials in source control:

   **PowerShell**

   ```powershell
   $env:DB_URL = "jdbc:mysql://localhost:3306/bank_management?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
   $env:DB_USER = "root"
   $env:DB_PASSWORD = "YOUR_PASSWORD"
   ```

   `DB_URL` and `DB_USER` have local-development defaults. `DB_PASSWORD` defaults to an empty string; set it to the password for your local database account.

## Build and run

From the project root:

```shell
mvn clean package
```

Copy `target/bank-management.war` into Tomcat's `webapps` directory, start Tomcat and open:

```text
http://localhost:8080/bank-management/
```

Do not open `src/main/webapp/index.html` or another HTML file directly (`file:///...`) or use VS Code Live Server. The pages need the running Java Servlet backend; opening them as static files cannot create accounts or sign in.

In VS Code, install the Java Extension Pack and a Tomcat server extension if you want to deploy the WAR directly from the editor. The project can also be opened as a Maven project.

## Application flow

1. Choose **Open an account** and create a customer account.
2. Save the generated account number shown after successful creation.
3. Sign in with the account number and password.
4. Use the dashboard navigation to view details, deposit, withdraw, check balance or review transaction history.

Account creation is public so a first customer can register. Every dashboard page and account operation requires an authenticated session. Deposits and withdrawals apply to the signed-in account; the account number is never accepted from the browser for those operations.

## API endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/accounts` | Create an account |
| POST | `/api/login` | Authenticate and start a session |
| GET | `/api/session` | Current account and CSRF token |
| POST | `/api/logout` | Invalidate the current session |
| POST | `/api/deposit` | Deposit to the signed-in account |
| POST | `/api/withdraw` | Withdraw from the signed-in account |
| GET | `/api/balance` | Read current balance |
| GET | `/api/account` | Read account details |
| GET | `/api/transactions` | Read the latest 100 transactions |

## Security and project notes

- Password values are never stored as plaintext. `password_hash` contains a salted PBKDF2 hash.
- SQL values are passed through `PreparedStatement`; user data is not interpolated into SQL.
- Deposit/withdrawal balance changes and transaction inserts share one database transaction and lock the account row.
- Authenticated POST requests require the session's CSRF token.
- `DB_PASSWORD` is environment-only; do not commit real credentials.
- This educational implementation does not include production bank controls such as multi-factor authentication, audit/operations tooling, regulatory controls, rate limiting or a real payment network.
