# Hướng dẫn chụp ảnh - Vinh.PQ (Infra & Security)

> Chụp xong đặt ảnh vào thư mục `screenshots/vinh-pq/` đặt tên theo số thứ tự.

---

## 1. Jenkins Dashboard

### 1.1 Trang chủ Jenkins
- Vào http://3.27.92.213:8080
- Chụp full trang dashboard (thấy job `yas-ci-pipeline`)
- **Tên file**: `01-jenkins-dashboard.png`

### 1.2 Multibranch Pipeline - Branches tab
- Vào `yas-ci-pipeline` → tab Branches
- Chụp full (thấy danh sách branches: main, ci/setup-jenkins-pipeline, ...)
- **Tên file**: `02-jenkins-branches.png`

### 1.3 Multibranch Pipeline - Pull Requests tab
- Vào `yas-ci-pipeline` → tab Pull Requests (hoặc Change Requests)
- Chụp full (thấy PR-6, PR-7, ...)
- **Tên file**: `03-jenkins-prs.png`

---

## 2. Jenkins Configuration

### 2.1 Multibranch Pipeline Config - Branch Sources
- Vào `yas-ci-pipeline` → Configure
- Chụp phần "Branch Sources" (thấy GitHub repo URL, discover branches strategy, discover PRs)
- **Tên file**: `04-jenkins-branch-sources.png`

### 2.2 Multibranch Pipeline Config - Build Configuration
- Cùng trang Configure
- Chụp phần "Build Configuration" (thấy "by Jenkinsfile", Script Path)
- **Tên file**: `05-jenkins-build-config.png`

### 2.3 Global Tool Configuration - JDK
- Vào Manage Jenkins → Tools
- Chụp phần JDK installations (thấy JDK-25)
- **Tên file**: `06-jenkins-jdk.png`

### 2.4 Global Tool Configuration - Maven
- Cùng trang Tools
- Chụp phần Maven installations (thấy Maven)
- **Tên file**: `07-jenkins-maven.png`

### 2.5 Credentials
- Vào Manage Jenkins → Credentials
- Chụp danh sách credentials (thấy `sonarqube-token`, `snyk-token`)
- ⚠️ KHÔNG hiển thị giá trị token
- **Tên file**: `08-jenkins-credentials.png`

---

## 3. SonarQube

### 3.1 SonarQube Dashboard
- Vào http://3.27.92.213:9000
- Chụp trang Projects (thấy project YAS)
- **Tên file**: `09-sonarqube-projects.png`

### 3.2 SonarQube Project Detail
- Click vào project YAS
- Chụp trang overview (thấy: Bugs, Vulnerabilities, Code Smells, Coverage %, Duplications)
- **Tên file**: `10-sonarqube-detail.png`

### 3.3 Console output - SonarQube
- Vào Jenkins → bất kỳ build nào → Console Output
- Tìm (Ctrl+F): `ANALYSIS SUCCESSFUL`
- Chụp đoạn console thấy dòng này
- **Tên file**: `11-sonarqube-console.png`

---

## 4. Snyk

### 4.1 Console output - Snyk
- Cùng Console Output
- Tìm: `Snyk scanning`
- Chụp đoạn console thấy Snyk test + monitor output
- **Tên file**: `12-snyk-console.png`

---

## 5. Pipeline Stage View

### 5.1 Stage View - build thành công
- Vào Jenkins → `yas-ci-pipeline` → PR-7 (hoặc PR-6) → build #1
- Chụp trang Stages (thấy tất cả 8 stages với màu xanh/đỏ)
- **Tên file**: `13-pipeline-stages-pass.png`

### 5.2 Stage View - build thất bại (nếu có)
- Nếu có build nào fail, chụp trang Stages đó
- **Tên file**: `14-pipeline-stages-fail.png` (optional)

---

## Tổng: ~14 ảnh
