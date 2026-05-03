# Nội dung kỹ thuật — Pipeline Jenkins YAS (để chèn vào báo cáo)

Tài liệu này tóm tắt **trạng thái pipeline đã triển khai** trên Jenkins (multibranch, `Jenkinsfile` ở root repo), phục vụ viết báo cáo đồ án CI/CD.

## 1. Mục tiêu và bối cảnh

- **Monorepo** YAS: nhiều microservice trong một repo; CI cố gắng chỉ build/test các module **liên quan thay đổi**. Cách chọn module nằm ở `ci/detect-changed-modules.sh` (không phải lúc nào cũng diff với `origin/main` — xem mục 6).
- **Jenkins** (AWS): job multibranch đọc `Jenkinsfile`; **JDK 25**, **Maven** cấu hình trong *Global Tool Configuration*.
- **SonarQube** tự host (không dùng SonarCloud trong pipeline lớp trình): phân tích mã + chỉ số coverage từ JaCoCo XML.
- **Snyk**: quét phụ thuộc (Maven `pom.xml`); lỗi quét **không làm fail toàn bộ pipeline** (ghi log, tiếp tục).

## 2. Luồng các stage (theo `Jenkinsfile`)

| Thứ tự | Stage | Ý nghĩa ngắn |
|--------|--------|----------------|
| 1 | **Checkout** | `checkout scm`, `git fetch origin main`, `chmod +x` cho script trong `ci/`. |
| 2 | **Detect Changed Modules** | Chạy `ci/detect-changed-modules.sh` → biến `CHANGED_MODULES` (danh sách module, có thể gồm `common-library`). |
| 3 | **Gitleaks – Secret Scan** | Quét secret; có baseline `gitleaks-baseline.json` nếu có — **phát hiện secret mới thì fail**. |
| 4 | **Test** | `mvn … test jacoco:report -DskipITs` theo từng module đổi; post: **JUnit** + **recordCoverage** (hiển thị UI Coverage trên Jenkins). |
| 5 | **Coverage Gate** | Với từng module service (trừ `common-library`): `ci/check-coverage.sh <module> <COVERAGE_THRESHOLD>` — ngưỡng mặc định **≥ 70%** dòng (LINE) trên báo cáo JaCoCo. |
| 6 | **Build** | `mvn package -DskipTests` theo module đổi. |
| 7 | **SonarQube – Analysis & Quality Gate** | Sử dụng `withSonarQubeEnv` để phân tích mã và treo pipeline đợi webhook báo cáo kết quả thông qua lệnh `waitForQualityGate`. |
| 8 | **Snyk – Dependency Scan** | Kiểm tra dependency tree với `-pl -am` để tránh lỗi nội suy POM, sau đó chạy `snyk test` và `snyk monitor` kèm `--maven-skip-wrapper`. Lỗi quét cấu hình không chặn pipeline. |

## 3. Điểm đã “ổn định” khi làm đồ án (để ghi trong báo cáo)

1. **Phân tách trách nhiệm coverage**  
   - Báo cáo JaCoCo do Maven tạo; **ngưỡng 70%** áp dụng qua script `ci/check-coverage.sh` (đọc aggregate LINE cuối file XML), tránh xung đột với việc chỉ test một phần monorepo.

2. **Gitleaks trước test**  
   - Fail sớm nếu commit chứa secret mới (giảm lãng phí tài nguyên build).

3. **SonarQube self-hosted**  
   - Phân tích theo module đổi; coverage đưa vào Sonar từ JaCoCo XML đã build ở stage Test.

4. **Snyk**  
   - Ghi nhận rủi ro dependency; pipeline **không dừng hàng loạt** khi Snyk báo lỗi/CLI (phù hợp môi trường học tập và org/token giới hạn). Trong báo cáo nên nêu **trade-off**: an toàn vs. “gate cứng” trên Snyk.

5. **Credential Jenkins**  
   - `sonarqube-token`, `snyk-token` — **không** đưa giá trị vào báo cáo; chỉ mô tả loại credential.

## 4. Gợi ý hình minh chứng (đồng bộ với `docs/screenshots-guide-*.md`)

- Dashboard Jenkins + tab Branches/PR của multibranch.
- Một build thành công: **Stage View** đủ các stage trên.
- Console: dòng **Sonar** (analysis success) và đoạn **Snyk** (test/monitor).
- SonarQube UI: project YAS, overview (bugs, coverage, …).

## 5. Khác với tài liệu `docs/README.md` gốc upstream

File `docs/README.md` mô tả **GitHub Actions** + SonarCloud của repo public `nashtech-garage/yas`. Báo cáo đồ án của nhóm nên làm rõ: pipeline thực tế triển khai là **Jenkins + SonarQube server + script monorepo**, không nhầm với workflow GitHub Actions trong README upstream.

---

## 6. Đối chiếu kỹ với mã nguồn (tránh ghi sai trong báo cáo)

### 6.1 Từ **agent** trong Jenkins (không phải “AI agent”)

Trong `Jenkinsfile`, dòng `agent any` là **cú pháp Jenkins Declarative Pipeline**: nghĩa là “chạy build trên **bất kỳ executor/node** nào Jenkins gán được”. Đây **không** liên quan tới việc tạo agent AI, Cursor Agent hay bot.

### 6.2 `ci/detect-changed-modules.sh` — mốc so sánh thực tế

Script **không nhận tham số** từ `Jenkinsfile`. Logic chọn `base_ref`:

| Tình huống | Mốc diff |
|------------|----------|
| Build PR (Jenkins có `CHANGE_TARGET`, ví dụ `main`) | `origin/<CHANGE_TARGET>` |
| Build nhánh thường, có commit cha | `HEAD~1` (so với commit trước trên cùng nhánh) |
| Repo chỉ 1 commit | Toàn bộ danh sách module |
| Đổi `pom.xml`, `.github/`, `common-library/`, `docker/`, … | **Full** tất cả module |

Comment trong `Jenkinsfile` có đoạn “fetch origin main” để shallow clone đủ ref; **không** đồng nghĩa mọi build đều diff với `main`.

### 6.3 Những tính năng Nâng cao đã tích hợp trong `Jenkinsfile`

Trong file pipeline thực tế, nhóm đã cấu hình thành công các tính năng nâng cao (rất nên ghi vào báo cáo để được điểm cao):

- **`waitForQualityGate`**: Chờ Quality Gate từ SonarQube trả webhook về Jenkins.
- **`withSonarQubeEnv('sonar-server')`**: Liên kết trực tiếp biến môi trường SonarQube Server của Jenkins System.
- **`recordCoverage`** (Coverage Plugin Jenkins): Đọc file `jacoco.xml` và vẽ biểu đồ + highlight code từng dòng trực tiếp trên giao diện Jenkins.
- Sử dụng **`-pl -am`** để xử lý bài toán Monorepo dependencies khi chạy Snyk CLI (giải quyết triệt để lỗi Exit code -13).

### 6.4 JaCoCo “prepare-agent” trong `pom.xml`

Goal Maven **`jacoco:prepare-agent`** (từ plugin JaCoCo) là **bytecode instrumentation** khi chạy test — khác hẳn Jenkins `agent`. Trong báo cáo nên phân biệt hai khái niệm này nếu có đề cập.
