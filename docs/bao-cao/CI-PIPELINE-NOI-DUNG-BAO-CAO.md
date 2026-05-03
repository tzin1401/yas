# Nội dung kỹ thuật — Pipeline Jenkins YAS (để chèn vào báo cáo)

Tài liệu này tóm tắt **trạng thái pipeline đã triển khai** trên Jenkins (multibranch, `Jenkinsfile` ở root repo), phục vụ viết báo cáo đồ án CI/CD.

## 1. Mục tiêu và bối cảnh

- **Monorepo** YAS: nhiều microservice trong một repo; CI chỉ cần build/test phần **đã thay đổi** so với `origin/main`.
- **Jenkins** (AWS): job multibranch đọc `Jenkinsfile`; **JDK 25**, **Maven** cấu hình trong *Global Tool Configuration*.
- **SonarQube** tự host (không dùng SonarCloud trong pipeline lớp trình): phân tích mã + chỉ số coverage từ JaCoCo XML.
- **Snyk**: quét phụ thuộc (Maven `pom.xml`); lỗi quét **không làm fail toàn bộ pipeline** (ghi log, tiếp tục).

## 2. Luồng các stage (theo `Jenkinsfile`)

| Thứ tự | Stage | Ý nghĩa ngắn |
|--------|--------|----------------|
| 1 | **Checkout** | `checkout scm`, `git fetch origin main`, `chmod +x` cho script trong `ci/`. |
| 2 | **Detect Changed Modules** | Chạy `ci/detect-changed-modules.sh` → biến `CHANGED_MODULES` (danh sách module, có thể gồm `common-library`). |
| 3 | **Gitleaks – Secret Scan** | Quét secret; có baseline `gitleaks-baseline.json` nếu có — **phát hiện secret mới thì fail**. |
| 4 | **Test** | `mvn … test jacoco:report -DskipITs` theo từng module đổi; post: **JUnit** + archive `jacoco.xml`. |
| 5 | **Coverage Gate** | Với từng module service (trừ `common-library`): `ci/check-coverage.sh <module> <COVERAGE_THRESHOLD>` — ngưỡng mặc định **≥ 70%** dòng (LINE) trên báo cáo JaCoCo. |
| 6 | **Build** | `mvn package -DskipTests` theo module đổi. |
| 7 | **SonarQube – Analysis** | `mvn sonar:sonar` với `-pl`/`-am` cho module đổi; token qua credential `sonarqube-token`; `sonar.coverage.jacoco.xmlReportPaths` trỏ tới `target/site/jacoco/jacoco.xml`. |
| 8 | **Snyk – Dependency Scan** | `snyk auth`, với mỗi module: `snyk test` (có retry giảm `--max-depth`) rồi `snyk monitor`; **exit code không chặn pipeline** (chỉ log). |

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
