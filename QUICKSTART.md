# Quick Start Guide

## 🚀 Getting Started in 5 Minutes

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- Docker (optional)
- kubectl (for Kubernetes deployment)

### Option 1: Quick Local Run

```bash
# Build and run with one script
./build-and-run.sh
```

### Option 2: Manual Build

```bash
# Build the application
mvn clean package

# Run the JAR
java -jar target/vulnerable-app-1.0.0.jar

# Access at http://localhost:8080
```

### Option 3: Docker

```bash
# Build Docker image
docker build -t vulnerable-app .

# Run container
docker run -p 8080:8080 vulnerable-app

# Or use docker-compose
docker-compose up
```

## 🧪 Test the Vulnerabilities

Once the application is running, test all vulnerabilities:

```bash
./test-vulnerabilities.sh
```

Or manually visit:
- http://localhost:8080 - Home page with all vulnerability categories
- http://localhost:8080/injection - SQL, Command, LDAP injection
- http://localhost:8080/xss - Cross-Site Scripting
- http://localhost:8080/access-control - IDOR, privilege escalation
- http://localhost:8080/crypto - Weak cryptography
- http://localhost:8080/auth - Authentication failures
- http://localhost:8080/ssrf - Server-Side Request Forgery
- http://localhost:8080/config - Security misconfiguration
- http://localhost:8080/integrity - Deserialization
- http://localhost:8080/logging - Logging failures

## 🔍 Test Individual Vulnerabilities

### SQL Injection
```bash
curl -X POST http://localhost:8080/injection/sql \
  -d "username=' OR '1'='1&password=' OR '1'='1"
```

### XSS
```bash
curl "http://localhost:8080/xss/search?query=<script>alert('XSS')</script>"
```

### Command Injection
```bash
curl -X POST http://localhost:8080/injection/command \
  -d "host=localhost; ls -la"
```

### IDOR
```bash
curl http://localhost:8080/access-control/profile/1
```

## 📊 View Results

### H2 Database Console
- URL: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: `password`

### Spring Boot Actuator
- Health: http://localhost:8080/actuator/health
- Info: http://localhost:8080/actuator/info
- All: http://localhost:8080/actuator

##  Deploy to Kubernetes

```bash
# Create namespace
kubectl create namespace vulnerable-app

# Apply configurations
kubectl apply -f k8s/configmap.yaml -n vulnerable-app
kubectl apply -f k8s/deployment.yaml -n vulnerable-app

# Get service URL
kubectl get svc vulnerable-app-service -n vulnerable-app

# Port forward for local access
kubectl port-forward svc/vulnerable-app-service 8080:80 -n vulnerable-app
```

## 📋 Troubleshooting

### Application Won't Start
- Check Java version: `java -version` (needs 17+)
- Check Maven version: `mvn -version` (needs 3.6+)
- Check port 8080 is free: `lsof -i :8080`

### Build Fails
```bash
# Clean Maven cache
mvn clean install -U

# Skip tests if they fail
mvn clean package -DskipTests
```

### Docker Issues
```bash
# Clean Docker cache
docker system prune -a

# Rebuild without cache
docker build --no-cache -t vulnerable-app .
```

## ⚠️ Important Security Notice

**This application is INTENTIONALLY VULNERABLE!**

- ❌ DO NOT deploy to production
- ❌ DO NOT expose to the internet
- ❌ DO NOT use in public networks
- ✅ USE ONLY in isolated test environments
- ✅ USE for security testing and training
- ✅ USE to test IAST tools

## 📚 Next Steps

1. ✅ Run the application
2. ✅ Test all vulnerabilities
3. ✅ Set up your IAST tool
4. ✅ View detected vulnerabilities
5. ✅ Learn remediation techniques
6. ✅ Set up Harness pipeline
7. ✅ Deploy to EKS

## 🤝 Need Help?

- Check [README.md](README.md) for full documentation
- Check application logs: `docker logs <container-id>`
- View Kubernetes logs: `kubectl logs -f <pod-name>`

## 📝 Available Scripts

- `./build-and-run.sh` - Build and optionally run the app
- `./test-vulnerabilities.sh` - Test all OWASP Top 10 vulnerabilities

---

**Happy Testing! 🔒**
