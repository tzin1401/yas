# Hướng dẫn chụp ảnh — Vinh.PQ (Infra, Jenkins, SonarQube, Snyk)

## Quy ước chung

| Mục | Chi tiết |
|-----|----------|
| **Thư mục lưu ảnh** | `docs/bao-cao/screenshots/vinh-pq/` |
| **Định dạng** | PNG; che mọi secret |
| **Jenkins URL** | Ví dụ `http://3.27.92.213:8080` — đổi nếu server nhóm khác |
| **SonarQube URL** | Ví dụ `http://3.27.92.213:9000` — đổi nếu khác |

**Credential:** trong ảnh chỉ được phần **ID** (vd: `sonarqube-token`), **không** hiển thị giá trị token/password.

---

## Pipeline — 8 stage (đúng `Jenkinsfile`)

Trên **Stage View** / Blue Ocean, thứ tự kỳ vọng:

| # | Tên stage (Jenkinsfile) |
|---|-------------------------|
| 1 | Checkout |
| 2 | Detect Changed Modules |
| 3 | Gitleaks – Secret Scan |
| 4 | Test |
| 5 | Coverage Gate |
| 6 | Build |
| 7 | SonarQube – Analysis |
| 8 | Snyk – Dependency Scan |

*(Tên có thể hơi cắt ngắn trên UI — đối chiếu thứ tự và màu xanh/đỏ.)*

---

## 1. Jenkins — Dashboard & multibranch

### 1.1 Dashboard

- Đăng nhập Jenkins → trang chủ.
- Chụp **full** — thấy job multibranch (vd: `yas-ci-pipeline`).
- **Tên file:** `01-jenkins-dashboard.png`

### 1.2 Tab Branches

- Vào job → tab **Branches**.
- Chụp danh sách nhánh có build (`main`, nhánh feature, …).
- **Tên file:** `02-jenkins-multibranch-branches.png`

### 1.3 Tab Pull Requests

- Cùng job → tab **Pull Requests** (hoặc tên tương đương).
- Chụp danh sách PR đã discover (PR-1, PR-2, …).
- **Tên file:** `03-jenkins-multibranch-prs.png`

---

## 2. Jenkins — Cấu hình job & công cụ

### 2.1 Branch Sources

- Job → **Configure** → phần **Branch Sources** (GitHub: repo, discover branches, discover PRs).
- **Tên file:** `04-jenkins-config-branch-sources.png`

### 2.2 Build Configuration

- Cùng trang Configure → **Build Configuration**: mode **by Jenkinsfile**, **Script Path** = `Jenkinsfile` (root).
- **Tên file:** `05-jenkins-config-jenkinsfile-path.png`

### 2.3 JDK (Global Tool)

- **Manage Jenkins** → **Tools** → JDK installations — thấy tên tool khớp pipeline (vd: **JDK-25**).
- **Tên file:** `06-jenkins-tools-jdk.png`

### 2.4 Maven (Global Tool)

- Cùng **Tools** → Maven installations — thấy tên khớp `tools { maven '...' }` (vd: **Maven**).
- **Tên file:** `07-jenkins-tools-maven.png`

### 2.5 Credentials (chỉ danh sách ID)

- **Manage Jenkins** → **Credentials** → domain phù hợp.
- Chụp danh sách: thấy ID **`sonarqube-token`**, **`snyk-token`** (không mở chi tiết chứa secret).
- **Tên file:** `08-jenkins-credentials-ids.png`

### 2.6 SonarQube servers (Mới bổ sung)

- **Manage Jenkins** → **System** → cuộn đến phần **SonarQube servers**.
- Chụp màn hình cấu hình URL và Server authentication token để minh chứng Jenkins được liên kết thành công với Sonar.
- **Tên file:** `08b-jenkins-system-sonarqube-server.png`

---

## 3. SonarQube

### 3.1 Danh sách project

- Mở SonarQube → trang **Projects** — thấy project phân tích YAS (tên đúng instance nhóm).
- **Tên file:** `09-sonarqube-projects.png`

### 3.2 Overview project

- Vào project → **Overview**: Bugs, Vulnerabilities, Code Smells, Coverage, Duplications (tùy layout phiên bản).
- **Tên file:** `10-sonarqube-project-overview.png`

### 3.3 Console Jenkins — stage SonarQube – Analysis

- Build đã chạy Sonar → **Console Output**.
- Ctrl+F: `sonar:sonar` và/hoặc `BUILD SUCCESS` ngay sau bước Sonar; có thể có `ANALYSIS SUCCESSFUL` tùy phiên bản scanner.
- **Tên file:** `11-jenkins-console-sonarqube.png`

### 3.4 Webhooks trả kết quả về Jenkins (Mới bổ sung)

- Trên SonarQube → **Project Settings** → **Webhooks**.
- Chụp màn hình cấu hình Webhook trỏ về Jenkins (URL dạng `http://<jenkins>/sonarqube-webhook/`).
- **Tên file:** `11b-sonarqube-webhook.png`

---

## 4. Snyk — Console & Web (Mới bổ sung)

### 4.1 Console Jenkins — stage Snyk

- **Cùng** hoặc build khác có stage **Snyk – Dependency Scan**.
- Ctrl+F: `Snyk scanning` hoặc `>>> Snyk scanning`.
- Chụp đoạn có `snyk test` / `snyk monitor` và exit message (pipeline có thể **tiếp tục** dù Snyk báo lỗi — đúng thiết kế `returnStatus` trong `Jenkinsfile`).
- **Tên file:** `12-jenkins-console-snyk.png`

### 4.2 Giao diện Snyk Web (Mới bổ sung)

- Đăng nhập vào trang quản lý `app.snyk.io`.
- Chụp màn hình hiển thị dự án YAS với các thông tin lỗi Vulnerability (High, Medium, Low...) do Snyk quét được.
- **Tên file:** `12b-snyk-web-dashboard.png`

---

## 5. Stage View — toàn pipeline

### 5.1 Build thành công

- Job → branch/PR → một build **xanh** end-to-end (hoặc xanh đến stage cuối nhóm cần minh chứng).
- Chụp **Stage View** — thấy đủ **8 stage** theo bảng mục đầu file.
- **Tên file:** `13-jenkins-stage-view-success.png`

### 5.2 Build thất bại (tùy chọn)

- Nếu có build đỏ minh chứng nguyên nhân (vd: Coverage Gate, Gitleaks) — chụp Stage View đỏ.
- **Tên file:** `14-jenkins-stage-view-failure.png` *(optional)*

---

## Tổng số ảnh

**~17 file** (`01` … `14`, cộng thêm `08b`, `11b`, `12b`), trong đó `14` là tuỳ chọn.
