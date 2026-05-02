# Hướng dẫn chụp ảnh - Vinh.NL (Leader)

> Chụp xong đặt ảnh vào thư mục `screenshots/vinh-nl/` đặt tên theo số thứ tự.
> Mỗi ảnh ghi chú mô tả ngắn bên dưới.

---

## 1. GitHub Repository

### 1.1 Trang chủ repo
- Vào https://github.com/tzin1401/yas
- Chụp full trang (thấy tên repo + collaborators + branches)
- **Tên file**: `01-github-repo.png`

### 1.2 GitHub Rulesets
- Vào Settings → Rules → Rulesets
- Chụp trang cấu hình ruleset cho branch `main`
- Cần thấy: "2 approvals required", "Status checks", "No force push"
- **Tên file**: `02-github-rulesets.png`

### 1.3 Webhook
- Vào Settings → Webhooks
- Chụp trang webhook (thấy URL `http://3.27.92.213:8080/github-webhook/`)
- Chụp thêm 1 ảnh Recent Deliveries (thấy ✅ giao thành công)
- **Tên file**: `03-webhook-config.png`, `04-webhook-deliveries.png`

---

## 2. Pull Requests

### 2.1 Danh sách PRs
- Vào https://github.com/tzin1401/yas/pulls
- Chụp full danh sách (thấy PR #6, #7, #8)
- **Tên file**: `05-pr-list.png`

### 2.2 Chi tiết 1 PR (chọn PR #6 hoặc #7)
- Mở 1 PR, chụp phần:
  - "Review required - At least 2 approving reviews"
  - Status checks (CI pass/fail)
  - "Merging is blocked" hoặc "Merge pull request"
- **Tên file**: `06-pr-detail-checks.png`

### 2.3 PR conversation (commits + review)
- Chụp phần conversation thấy commits và review comments
- **Tên file**: `07-pr-conversation.png`

---

## 3. Console Output - Pipeline Results

### 3.1 Gitleaks output
- Vào Jenkins → PR-7 (hoặc PR-6) → Console Output
- Tìm (Ctrl+F): `Gitleaks scan PASSED`
- Chụp đoạn console thấy dòng này
- **Tên file**: `08-gitleaks-pass.png`

### 3.2 Detect Changed Modules output
- Tìm: `Modules selected for CI:`
- Chụp đoạn console
- **Tên file**: `09-detect-modules.png`

### 3.3 Coverage Gate output
- Tìm: `line coverage:` hoặc `[WARN]`
- Chụp đoạn console thấy kết quả coverage
- **Tên file**: `10-coverage-gate.png`

---

## Tổng: ~10 ảnh
