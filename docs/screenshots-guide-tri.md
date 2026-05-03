# Hướng dẫn chụp ảnh — Trí (Testing: unit test, JaCoCo, Coverage Gate)

## Quy ước chung

| Mục | Chi tiết |
|-----|----------|
| **Thư mục lưu ảnh** | `docs/bao-cao/screenshots/tri/` |
| **Định dạng** | PNG |
| **PR / build** | Chọn **một PR (hoặc nhánh)** đã chạy Jenkins đủ stage **Test** và **Coverage Gate** — không bắt buộc số PR cố định; ghi rõ số PR trong caption báo cáo |

Gợi ý: dùng **cùng một build** với ảnh Jenkins của Vinh.NL / Vinh.PQ.

---

## Pipeline liên quan phần Testing

| Stage | Nội dung minh chứng |
|-------|---------------------|
| **Test** | Maven Surefire/Failsafe: `mvn … test jacoco:report -DskipITs`; Jenkins plugin **JUnit**; artifact `jacoco.xml`. |
| **Coverage Gate** | Script `ci/check-coverage.sh`: log dạng `Module <tên-module> line coverage: …%`. |

**Lưu ý:** Chuỗi kiểu **`Analyzed bundle`** trong log thường đến từ **SonarQube scanner** khi import JaCoCo, **không** phải output đặc trưng của goal `jacoco:report`. Để minh chứng JaCoCo/Coverage Gate, ưu tiên: **Test Result** trong Jenkins + dòng **`Tests run:`** + dòng **`line coverage:`** (Coverage Gate).

---

## 1. GitHub — PR có thay đổi test

Chọn **một PR** có file test Java / chỉnh `src/test` (số PR ghi trong báo cáo).

### 1.1 Tổng quan PR

- Tab PR: title, mô tả, nhánh, trạng thái checks (Jenkins nếu báo lên GitHub).
- **Tên file:** `01-github-pr-overview.png`

### 1.2 Files changed

- Tab **Files changed** — thấy các file `*Test.java` hoặc test resources.
- Nếu quá dài, chụp 1–2 ảnh chia đoạn.
- **Tên file:** `02-github-pr-files-changed.png` (và `02b-...` nếu cần ảnh thứ hai)

### 1.3 Đoạn mã test (mẫu)

- Mở một file test tiêu biểu — vài `@Test` / method rõ ràng.
- **Tên file:** `03-github-pr-test-code-sample.png`

---

## 2. GitHub — PR thứ hai (tùy chọn, so sánh module khác)

Lặp mục 1 với **PR hoặc commit khác** (module khác: ví dụ media vs tax) **chỉ nếu** báo cáo cần minh chứng 2 case.

- **Tên file:** `04-github-pr2-overview.png`, `05-github-pr2-files-changed.png`, `06-github-pr2-test-sample.png`  
*(Có thể bỏ bớt nếu chỉ cần một PR.)*

---

## 3. Jenkins — Test Result & Console (stage Test)

Vào `yas-ci-pipeline` → **đúng branch/PR** đã chọn ở mục 1 → build thành công qua stage Test.

### 3.1 Trang Test Result

- Trong build → mục **Test Result** (hoặc **Test Result Aggregator** tùy plugin).
- Chụp tổng số test, passed/failed/skipped (nếu có).
- **Tên file:** `07-jenkins-test-result.png`

### 3.2 Console — Surefire / Failsafe

- **Console Output** → Ctrl+F: **`Tests run:`**
- Chụp **các khối** `Tests run:` (có thể nhiều module — `common-library`, `product`, …).
- **Tên file:** `08-jenkins-console-tests-run.png`

### 3.3 Console — JaCoCo report (Maven)

- Tìm các dòng liên quan **`jacoco:report`** hoặc `[INFO] --- jacoco` / hoàn tất report cho module đổi.
- **Không** bắt buộc có chữ “Analyzed bundle” ở bước này.
- **Tên file:** `09-jenkins-console-jacoco-report.png`

---

## 4. Jenkins — Coverage Gate (stage Coverage Gate)

### 4.1 Coverage Gate Pass

- Cùng **Console Output** → Ctrl+F: **`line coverage:`**
- Chụp dòng kiểu: `Module <tên-module> line coverage: XX.XX% (required >= 70%)`  
  (ngưỡng lấy từ biến `COVERAGE_THRESHOLD` trong `Jenkinsfile`, mặc định **70**.)
- **Tên file:** `10-jenkins-console-coverage-gate.png`

### 4.2 Coverage Gate Fail (Mới bổ sung)

- Cố tình mở PR chứa code chưa được test hoặc lấy log build bị tạch coverage (ví dụ `< 70%`).
- Chụp log ghi rõ dòng lỗi: `Coverage gate failed cho module: ... < 70%` để minh chứng Pipeline có chặn lại thực sự.
- **Tên file:** `10b-jenkins-console-coverage-fail.png`

---

## 5. Tùy chọn — Sonar hiển thị coverage

Nếu báo cáo muốn nối JaCoCo với Sonar:

- Dùng ảnh Sonar **Overview** (Coverage %) do Vinh.PQ chụp, **hoặc**
- Trong Console stage **SonarQube – Analysis**, tìm log **JaCoCo XML** / sensor coverage — **tên file riêng** tránh trùng guide PQ: `11-jenkins-console-sonar-jacoco-sensor.png` *(optional)*

---

## Tổng số ảnh

- **Tối thiểu khuyến nghị:** `01`–`03`, `07`–`10` (**8 ảnh**) cho một PR/build (cộng thêm `10b`).
- **Đầy đủ** với PR thứ hai + Sonar optional: tới **~12 ảnh** như trên.
