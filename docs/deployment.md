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

```bash
SPRING_PROFILES_ACTIVE=provisioning \
PROVISIONING_API_KEY=<strong-random-key> \
TENANT_REGISTRY_JDBC_URL=<registry-jdbc-url> \
TENANT_REGISTRY_USERNAME=<registry-user> \
TENANT_REGISTRY_PASSWORD=<registry-password> \
TIDB_ADMIN_JDBC_URL=<admin-jdbc-url> \
TIDB_ADMIN_USERNAME=<admin-user> \
TIDB_ADMIN_PASSWORD=<admin-password> \
java -cp <app-classpath-or-jar> \
com.pbl3.project.pbl3_project.provisioning.TenantProvisioningApplication
```

The provisioning API is the only process that should hold database admin
credentials. Put HTTPS in front of it with a reverse proxy/load balancer if the
Java process itself does not terminate TLS.
