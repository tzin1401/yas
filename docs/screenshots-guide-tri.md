# Hướng dẫn chụp ảnh - Trí (Testing)

> Chụp xong đặt ảnh vào thư mục `screenshots/tri/` đặt tên theo số thứ tự.

---

## 1. Pull Request #7 (media tests)

### 1.1 PR #7 trên GitHub
- Vào https://github.com/tzin1401/yas/pull/7
- Chụp trang PR (thấy title, description, branch name, status checks)
- **Tên file**: `01-pr7-overview.png`

### 1.2 PR #7 - Files changed
- Click tab "Files changed"
- Chụp danh sách files thay đổi (thấy các file test Java)
- Nếu nhiều files, chụp 2-3 ảnh
- **Tên file**: `02-pr7-files.png`

### 1.3 PR #7 - Test code sample
- Mở 1 file test (vd: MediaServiceUnitTest.java)
- Chụp phần code test (vài test methods tiêu biểu)
- **Tên file**: `03-pr7-test-code.png`

---

## 2. Pull Request #6 (tax tests)

### 2.1 PR #6 trên GitHub
- Vào https://github.com/tzin1401/yas/pull/6
- Chụp trang PR (title, status checks)
- **Tên file**: `04-pr6-overview.png`

### 2.2 PR #6 - Files changed
- Click tab "Files changed"
- Chụp danh sách files thay đổi
- **Tên file**: `05-pr6-files.png`

### 2.3 PR #6 - Test code sample
- Mở 1 file test (vd: TaxRateServiceTest.java)
- Chụp phần code test tiêu biểu
- **Tên file**: `06-pr6-test-code.png`

---

## 3. Jenkins Test Results

### 3.1 Test Result - PR #7
- Vào Jenkins → `yas-ci-pipeline` → PR-7 → build #1 → **Test Result**
- Chụp trang Test Result (thấy tổng số tests, pass, fail)
- **Tên file**: `07-test-result-pr7.png`

### 3.2 Console - Tests run PR #7
- Vào Console Output
- Tìm (Ctrl+F): `Tests run:`
- Chụp TẤT CẢ các dòng `Tests run:` (có thể có nhiều dòng cho media + common-library)
- **Tên file**: `08-console-tests-pr7.png`

### 3.3 Console - JaCoCo PR #7
- Tìm: `Analyzed bundle`
- Chụp đoạn thấy `Analyzed bundle 'media'` hoặc `Analyzed bundle 'Common Library'`
- **Tên file**: `09-console-jacoco-pr7.png`

### 3.4 Test Result - PR #6
- Tương tự PR #7 nhưng cho PR-6
- **Tên file**: `10-test-result-pr6.png`

### 3.5 Console - Tests run PR #6
- Tìm `Tests run:` trong Console Output PR-6
- **Tên file**: `11-console-tests-pr6.png`

---

## 4. Coverage (nếu có)

### 4.1 Console - Coverage percentage
- Tìm: `line coverage:` trong Console Output
- Chụp dòng hiển thị `Module media line coverage: XX.XX%` (hoặc tax)
- **Tên file**: `12-coverage-output.png`

---

## Tổng: ~12 ảnh
