# YAS Version Decision

- Source repo: https://github.com/tzin1401/yas
- Lab 2 branch: `lab2/cd-platform`
- Current fork stack verified from root `pom.xml`: Java 25, Spring Boot 4.0.2
- Assignment stack: Java 21, Spring Boot 3.2
- Decision: keep the current fork stack; do not downgrade for Lab 2.
- Reason: Lab 1 CI was already implemented on this fork with Jenkins tool `JDK-25`, Maven, Spring Boot 4.0.2, and Java 25 source/target configuration.
- Impact: Jenkins agents, Docker builds, local Maven commands, and cluster smoke documentation must use Java 25-compatible tooling.

## Production Reality Note

This lab setup may use NodePort, hosts file entries, Tailscale, NFS, and demo credentials because those match the course environment. Production deployments should use proper DNS/TLS, least-privilege RBAC, managed or CSI-backed storage, isolated container builders, and external secret management.
