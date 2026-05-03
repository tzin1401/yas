# Báo cáo đồ án CI — làm việc nhóm trên nhánh `docs/bao-cao`

Nhánh này dùng **chỉ cho nội dung báo cáo** (Markdown/LaTeX, ảnh minh chứng), tránh lẫn với code CI trên `main` hoặc nhánh feature.

## Quy ước nhanh

| Nội dung | Vị trí |
|----------|--------|
| Bản nháp / outline | `docs/bao-cao/` |
| Ảnh chụp màn hình | `docs/bao-cao/screenshots/<tên-thành-viên>/` |
| File nộp (Word) | **Không commit** file `.docx` lớn nếu repo giới hạn; có thể để link Drive hoặc chỉ 1 bản cuối |
| Đặt tên file nộp bài | `<MSSV1>_<MSSV2>_<MSSV3>.docx` (MSSV tăng dần) — theo đề bài |

## Luồng làm việc

1. Mỗi người checkout nhánh: `git checkout docs/bao-cao && git pull`
2. Viết/sửa trong `docs/bao-cao/` hoặc thêm ảnh vào `screenshots/...`
3. Commit nhỏ, message rõ: `docs: ...`
4. Push và mở **Pull Request** vào `main` (hoặc merge trực tiếp nếu team thống nhất — nên dùng PR để CI không chạy lung tung nếu chỉ đổi docs)

## Gợi ý phân công (đã có hướng dẫn chụp ảnh)

- `docs/screenshots-guide-vinh-nl.md` — Leader: GitHub (rulesets, webhook), PR, log Jenkins tối thiểu (Gitleaks / Detect / Coverage Gate)
- `docs/screenshots-guide-vinh-pq.md` — Infra: Jenkins (dashboard, cấu hình, tools, credentials), SonarQube UI, Snyk + console, Stage View đủ 8 stage
- `docs/screenshots-guide-tri.md` — Testing: PR + mã test, Jenkins Test Result, `Tests run:`, JaCoCo, Coverage Gate (`line coverage:`)

Chụp xong đặt ảnh vào `docs/bao-cao/screenshots/<tên-thành-viên>/` đúng tên file trong từng guide.

## Tóm tắt pipeline (để copy vào Word/LaTeX)

Xem **`CI-PIPELINE-NOI-DUNG-BAO-CAO.md`** — mô tả các stage Jenkins, Gitleaks, coverage gate, SonarQube, Snyk và các điểm đã ổn định khi làm đồ án (khớp `Jenkinsfile` + `ci/*.sh`).

## Overleaf / LaTeX

Nếu dùng template LaTeX riêng, có thể đặt file `.tex` trong `docs/bao-cao/` (ví dụ `main.tex`) và **không** đưa PDF build ra git (thêm vào `.gitignore` nếu cần).
