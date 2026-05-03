# Hướng dẫn chụp ảnh — Vinh.NL (Leader / GitHub & quy trình PR)

## Quy ước chung

| Mục | Chi tiết |
|-----|----------|
| **Thư mục lưu ảnh** | `docs/bao-cao/screenshots/vinh-nl/` (đúng quy ước trong `docs/bao-cao/README.md`) |
| **Định dạng** | PNG, độ phân giải đủ đọc chữ |
| **Bảo mật** | Không lộ token, secret, cookie nhạy cảm trên ảnh |
| **Repo GitHub** | Thay `https://github.com/tzin1401/yas` nếu nhóm đổi remote |

Khi chụp log Jenkins, nên dùng **cùng một số build** với các ảnh của Vinh.PQ / Trí để báo cáo thống nhất.

---

## Thứ tự stage CI (tham chiếu)

Pipeline đọc `Jenkinsfile` tại root repo, **8 stage**:

1. Checkout  
2. Detect Changed Modules  
3. Gitleaks – Secret Scan  
4. Test  
5. Coverage Gate  
6. Build  
7. SonarQube – Analysis  
8. Snyk – Dependency Scan  

Chi tiết kỹ thuật: `docs/bao-cao/CI-PIPELINE-NOI-DUNG-BAO-CAO.md`.

---

## 1. GitHub — repository & quy tắc

### 1.1 Trang chính repo

- Mở `https://github.com/tzin1401/yas` (hoặc URL repo nhóm).
- Chụp **full** trang (tên repo, About/branches, phần nhìn thấy collaborator nếu có).
- **Tên file:** `01-github-repo-home.png`

### 1.2 Rulesets (branch protection)

- **Settings** → **Rules** → **Rulesets** (hoặc **Branches** / rule cho `main`, tùy giao diện GitHub).
- Chụp rule áp dụng cho `main`: yêu cầu review, status checks, cấm force-push (nội dung hiển thị trên màn hình).
- **Tên file:** `02-github-rulesets-main.png`

### 1.3 Webhook tới Jenkins

- **Settings** → **Webhooks**
- Một ảnh: danh sách webhook — thấy **Payload URL** trỏ tới Jenkins (ví dụ dạng `http://<jenkins>/github-webhook/`).
- Một ảnh (nếu có): **Recent Deliveries** — một delivery **thành công** (✓, HTTP 200).
- **Tên file:** `03-github-webhook-list.png`, `04-github-webhook-delivery-ok.png`

---

## 2. Pull Request — quy trình nhóm

### 2.1 Danh sách PR

- Tab **Pull requests** của repo.
- Chụp danh sách (vài PR đang mở/đã merge gần đây).
- **Tên file:** `05-github-pr-list.png`

### 2.2 Một PR đại diện — checks & merge

- Mở **một PR thật** đã chạy Jenkins (không bắt buộc số cố định).
- Chụp phần: yêu cầu review (ví dụ 2 approvers), **Checks** / trạng thái CI, dòng “Merge” bị chặn hoặc cho phép merge.
- **Tên file:** `06-github-pr-checks-and-merge.png`

### 2.3 Conversation & review

- Cùng PR: tab **Conversation** — thấy commit và/hoặc review comment.
- **Tên file:** `07-github-pr-conversation.png`

---

## 3. Jenkins — log minh chứng (cùng build với pipeline)

Vào job multibranch (ví dụ `yas-ci-pipeline`) → chọn **branch/PR** → build đã **thành công** → **Console Output**.

### 3.1 Gitleaks pass

- Ctrl+F: `Gitleaks scan PASSED` hoặc `>>> Gitleaks scan PASSED`.
- **Tên file:** `08-jenkins-console-gitleaks-pass.png`

### 3.2 Detect Changed Modules

- Ctrl+F: `Modules selected for CI:` (và dòng `echo` ngay sau).
- **Tên file:** `09-jenkins-console-detect-modules.png`

### 3.3 Coverage Gate

- Ctrl+F: `line coverage:` hoặc `Coverage gate failed` (nếu minh chứng gate hoạt động).
- **Tên file:** `10-jenkins-console-coverage-gate.png`

---

## Tổng số ảnh (mục tiêu)

**10 file** (`01` … `10`) như bảng trên. Có thể bớt ảnh webhook delivery nếu GitHub không hiển thị — giữ tối thiểu ảnh webhook **list**.
