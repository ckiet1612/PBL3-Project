# Deployment

## Desktop Release

Build the desktop artifact for normal users with:

```bash
./mvnw -Pdesktop-release -DskipTests package
```

The `desktop-release` Maven profile adds a release marker and excludes admin-only
entry points/resources from the packaged jar:

- `demo`
- `migration`
- `provisioning`
- Flyway migration resources

The JavaFX desktop entry point also rejects `demo`, `migration`, and
`provisioning` profiles at startup. A desktop release is treated as a
tenant-client build and starts tenant workspaces with:

```text
spring.profiles.active=tenant-client
spring.main.web-application-type=none
```

Desktop releases must call a remote provisioning API through HTTPS:

```bash
SPRING_PROFILES_ACTIVE=tenant-client \
PROVISIONING_API_BASE_URL=https://provisioning.example.com \
PROVISIONING_API_KEY=<client-provisioning-key> \
java -jar target/pbl3-project-0.0.1-SNAPSHOT.jar
```

Local HTTP is for development only:

```bash
SPRING_PROFILES_ACTIVE=tenant-client \
PROVISIONING_API_BASE_URL=http://localhost:8088 \
PROVISIONING_API_ALLOW_LOCAL=true \
PROVISIONING_API_KEY=pbl3-local-dev-key \
./mvnw javafx:run
```

Do not ship TiDB admin/root credentials with the desktop app.

## Installer Packaging

macOS DMG must be built on macOS with a full JDK 21+:

```bash
PROVISIONING_API_BASE_URL=https://provisioning.example.com \
PROVISIONING_API_KEY=<client-provisioning-key> \
./scripts/package-macos-dmg.sh
```

Output:

```text
dist/installers/macos/*.dmg
```

Windows MSI must be built on Windows with a full JDK 21+ and WiX Toolset on
`PATH`:

```powershell
$env:PROVISIONING_API_BASE_URL = "https://provisioning.example.com"
$env:PROVISIONING_API_KEY = "<client-provisioning-key>"
.\scripts\package-windows-msi.ps1
```

Output:

```text
dist\installers\windows\*.msi
```

## Provisioning API

Deploy the provisioning API separately on a server/cloud host and keep it online
for new device onboarding.

Build the API jar locally with:

```bash
./mvnw -Pprovisioning-api -DskipTests package
```

```bash
SPRING_PROFILES_ACTIVE=provisioning \
PROVISIONING_API_KEY=<strong-random-key> \
TENANT_REGISTRY_JDBC_URL=<registry-jdbc-url> \
TENANT_REGISTRY_USERNAME=<registry-user> \
TENANT_REGISTRY_PASSWORD=<registry-password> \
TIDB_ADMIN_JDBC_URL=<admin-jdbc-url> \
TIDB_ADMIN_USERNAME=<admin-user> \
TIDB_ADMIN_PASSWORD=<admin-password> \
APP_LATEST_VERSION=<latest-desktop-version> \
APP_DOWNLOAD_URL_MAC=<mac-dmg-url> \
APP_DOWNLOAD_URL_WINDOWS=<windows-msi-url> \
java -cp <app-classpath-or-jar> \
com.pbl3.project.pbl3_project.provisioning.TenantProvisioningApplication
```

The provisioning API is the only process that should hold database admin
credentials. Put HTTPS in front of it with a reverse proxy/load balancer if the
Java process itself does not terminate TLS.

### Docker Cloud Deploy

The repository includes `Dockerfile.provisioning` for deploying only the
Provisioning API:

```bash
docker build -f Dockerfile.provisioning -t pbl3-provisioning-api .
docker run --rm -p 8088:8080 \
  -e SPRING_PROFILES_ACTIVE=provisioning \
  -e PROVISIONING_API_KEY=<strong-random-key> \
  -e TIDB_JDBC_URL=<registry-jdbc-url> \
  -e TIDB_USERNAME=<registry-user> \
  -e TIDB_PASSWORD=<registry-password> \
  -e TIDB_ADMIN_JDBC_URL=<admin-jdbc-url> \
  -e TIDB_ADMIN_USERNAME=<admin-user> \
  -e TIDB_ADMIN_PASSWORD=<admin-password> \
  -e APP_LATEST_VERSION=<latest-desktop-version> \
  -e APP_DOWNLOAD_URL_MAC=<mac-dmg-url> \
  -e APP_DOWNLOAD_URL_WINDOWS=<windows-msi-url> \
  pbl3-provisioning-api
```

Health check:

```text
GET /api/provisioning/health
```

Desktop update metadata:

```text
GET /api/provisioning/app-update
X-Provisioning-Key: <client-provisioning-key>
```

Example update env:

```text
APP_LATEST_VERSION=1.1.0
APP_MIN_SUPPORTED_VERSION=1.0.0
APP_DOWNLOAD_URL_MAC=https://github.com/<owner>/<repo>/releases/download/v1.1.0/Sales-Mgr-1.1.0.dmg
APP_DOWNLOAD_URL_WINDOWS=https://github.com/<owner>/<repo>/releases/download/v1.1.0/Sales-Mgr-1.1.0.msi
APP_RELEASE_NOTES=Bug fixes and POS performance improvements.
```

The service listens on the cloud `PORT` environment variable when provided.
For local Docker, the container defaults to port `8080`.

### Render Blueprint

`render.yaml` is included for a Docker-based Render deployment. Create a new
Blueprint from the repository and set these environment variables in Render:

```text
PROVISIONING_API_KEY
TIDB_JDBC_URL
TIDB_USERNAME
TIDB_PASSWORD
TIDB_ADMIN_JDBC_URL
TIDB_ADMIN_USERNAME
TIDB_ADMIN_PASSWORD
APP_LATEST_VERSION
APP_MIN_SUPPORTED_VERSION
APP_DOWNLOAD_URL_MAC
APP_DOWNLOAD_URL_WINDOWS
APP_RELEASE_NOTES
```

After deployment, use the Render HTTPS URL as the desktop build URL:

```bash
PROVISIONING_API_BASE_URL=https://<your-render-service>.onrender.com \
PROVISIONING_API_KEY=<same-provisioning-key> \
APP_VERSION=1.1.0 \
./scripts/package-macos-dmg.sh
```

Do not paste or commit real TiDB passwords into source files, scripts, or docs.
