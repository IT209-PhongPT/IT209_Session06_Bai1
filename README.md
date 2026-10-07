# Báo cáo Bài tập 1: Khảo sát FHS và Phân quyền File/Folder nâng cao

- **Họ và tên:** Phạm Thanh Phong
- **Mã sinh viên:** N24DTCN120
- **Môn học:** IT209
- **Session:** 06 - Quản trị Hệ thống Tệp tin Linux (FHS) & Phân quyền Nâng cao
- **Đường dẫn nộp bài:** `homework/session_06/ex1/`

---

## 1. Bối cảnh & Yêu cầu bài tập

Triển khai cấu trúc thư mục dùng chung cho ứng dụng Web tại `/var/www/my-app` tuân thủ tiêu chuẩn Linux Filesystem Hierarchy Standard (FHS):
1. **Thư mục `/var/www/my-app/public` (Chứa tài nguyên tĩnh):**
   - Chủ sở hữu (Owner): Đọc, ghi và thực thi/truy cập (`rwx` - 7).
   - Nhóm sở hữu (Group - `www-data`): Đọc và truy cập (`r-x` - 5).
   - Người dùng khác (Others): Không có quyền (`---` - 0).
   - **Quyền mục tiêu:** `drwxr-x---` (`750`).
2. **Thư mục `/var/www/my-app/logs` (Chứa nhật ký hệ thống):**
   - Chủ sở hữu (Owner): Toàn quyền đọc, ghi, truy cập (`rwx` - 7).
   - Nhóm sở hữu (Group - `www-data`): Toàn quyền đọc, ghi, truy cập (`rwx` - 7).
   - Người dùng khác (Others): Không có quyền (`---` - 0).
   - **Quyền mục tiêu:** `drwxrwx---` (`770`).
3. **Chủ sở hữu & Nhóm:**
   - Sử dụng tài khoản non-root (`$USER`) làm chủ sở hữu và gán nhóm dịch vụ Web (`www-data`).

---

## 2. Nhật ký câu lệnh thực hiện (Command Execution Log)

```bash
# ==============================================================================
# BƯỚC 1: Khởi tạo cấu trúc thư mục theo tiêu chuẩn Linux FHS
# ==============================================================================
sudo mkdir -p /var/www/my-app/public
sudo mkdir -p /var/www/my-app/logs

# Đảm bảo nhóm www-data tồn tại trong hệ thống
sudo groupadd -f www-data

# ==============================================================================
# BƯỚC 2: Phân quyền thư mục bằng ký hiệu số (Octal Notation)
# ==============================================================================
# Phân quyền 750 (u=rwx, g=rx, o=) cho thư mục public
sudo chmod 750 /var/www/my-app/public

# Phân quyền 770 (u=rwx, g=rwx, o=) cho thư mục logs
sudo chmod 770 /var/www/my-app/logs

# ==============================================================================
# BƯỚC 3: Thay đổi quyền sở hữu User và Group (chown)
# ==============================================================================
# Gán Owner là user hiện tại ($USER) và Group là www-data cho toàn bộ cây thư mục my-app
sudo chown -R $USER:www-data /var/www/my-app
```

---

## 3. Kết quả kiểm tra xác thực (Verification Output)

### Lệnh thực thi:
```bash
ls -la /var/www/my-app
```

### Đầu ra thực tế (Terminal Output):
```text
total 16
drwxr-xr-x 4 phongpt www-data 4096 Oct  7 18:30 .
drwxr-xr-x 3 root    root     4096 Oct  7 18:30 ..
drwxrwx--- 2 phongpt www-data 4096 Oct  7 18:30 logs
drwxr-x--- 2 phongpt www-data 4096 Oct  7 18:30 public
```

### Kiểm tra chi tiết bằng lệnh `stat`:
```bash
stat -c "%a %A %U:%G %n" /var/www/my-app/public /var/www/my-app/logs
```

**Đầu ra:**
```text
750 drwxr-x--- phongpt:www-data /var/www/my-app/public
770 drwxrwx--- phongpt:www-data /var/www/my-app/logs
```

---

## 4. Bảng tổng hợp đối chiếu quyền hạn

| Thư mục / Tệp tin | Owner (Chủ sở hữu) | Group (Nhóm: `www-data`) | Others (Người khác) | Ký hiệu Symbolic | Mã Octal | Giải thích bảo mật |
| :--- | :---: | :---: | :---: | :---: | :---: | :--- |
| `/var/www/my-app/public` | `rwx` (Đọc/Ghi/Vào) | `r-x` (Đọc/Vào) | `---` (Không có quyền) | `drwxr-x---` | `750` | Cho phép Web Server đọc mã nguồn tĩnh để phục vụ client, ngăn chặn người ngoài duyệt thư mục. |
| `/var/www/my-app/logs` | `rwx` (Toàn quyền) | `rwx` (Toàn quyền) | `---` (Không có quyền) | `drwxrwx---` | `770` | Cho phép Web Server và Developer đọc/ghi file log, bảo mật thông tin nhạy cảm khỏi user khác trên máy chủ. |

---

## 5. Chương trình Java Thẩm định Phân quyền POSIX (`Main.java`)

Bài tập đi kèm mã nguồn Java chuẩn (`Main.java`) để tự động hóa việc phân tích bit nhị phân, kiểm thử quy tắc phân quyền FHS và tương thích thư viện `java.nio.file.attribute.PosixFilePermissions`.

### Lệnh thực thi:
```bash
java Main.java
```

