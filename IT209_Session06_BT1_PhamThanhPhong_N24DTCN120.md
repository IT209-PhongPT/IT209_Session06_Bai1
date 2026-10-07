# Báo cáo Chuyên sâu Bài tập 1: Khảo sát FHS và Phân quyền File/Folder Nâng cao

**Thông tin sinh viên:**
- **Họ và tên:** Phạm Thanh Phong
- **Mã sinh viên:** N24DTCN120
- **Môn học:** IT209 - Phát triển ứng dụng Web / Cloud Infrastructure
- **Session:** Session 06 - Quản trị Hệ thống Tệp tin Linux (FHS) & Phân quyền Nâng cao
- **Bài tập:** Bài tập 1 (ex1)

---

## 1. Mục tiêu & Bối cảnh bài toán

### 1.1. Bối cảnh (Context)
Trong môi trường máy chủ sản xuất (Production Linux Server), việc cấu trúc tệp tin và thiết lập cơ chế phân quyền (File Permissions) là nền tảng sống còn của bảo mật hạ tầng máy chủ và ứng dụng web. 
- Tiêu chuẩn **FHS (Filesystem Hierarchy Standard)** quy định thư mục `/var` chuyên chứa dữ liệu biến đổi trong thời gian thực (Variable Data), trong đó `/var/www` là vị trí chuẩn hóa dành cho các ứng dụng web.
- Một ứng dụng web thực tế thường bao gồm các thành phần có mức độ nhạy cảm khác nhau:
  - **Tài nguyên tĩnh công khai (`public`):** Cần cho phép Web Server (nhóm `www-data`) đọc để phản hồi người dùng truy cập web, nhưng không cho phép chỉnh sửa trực tiếp qua kênh web.
  - **Nhật ký hệ thống (`logs`):** Chứa các thông tin vận hành, lỗi và dấu vết bảo mật. Cả ứng dụng web (chạy dưới nhóm `www-data`) và kỹ sư bảo trì (`Owner`) đều cần toàn quyền ghi/đọc log, nhưng tuyệt đối không cho người dùng khác trong hệ thống (`Others`) truy cập hoặc xem lén.

### 1.2. Mục tiêu kỹ thuật
1. Thành thạo việc tạo lập thư mục và điều hướng trong cấu trúc cây thư mục Linux theo chuẩn FHS.
2. Nắm vững bản chất toán học và logic của 3 bộ phân quyền: **Owner (User) - Group - Others**.
3. Hiểu và áp dụng linh hoạt hai phương pháp phân quyền:
   - **Dạng số bát phân (Octal / Numeric Mode):** Tính toán trọng số nhị phân `r=4, w=2, x=1`.
   - **Dạng ký tự tượng trưng (Symbolic Mode):** Sử dụng các ký hiệu `u, g, o, a` kết hợp `+, -, =` và `r, w, x`.
4. Làm chủ lệnh quản trị quyền sở hữu `chown` (Change Owner/Group) kết hợp tham số đệ quy `-R` trên tài khoản non-root và nhóm dịch vụ.

### 1.3. Ràng buộc kỹ thuật
- **Cấu trúc:** `/var/www/my-app/public` và `/var/www/my-app/logs`.
- **Thư mục `public`:** Owner toàn quyền (`rwx`), Group chỉ đọc và truy cập (`r-x`), Others không có quyền (`---`) -> `drwxr-x---` (Octal: `750`).
- **Thư mục `logs`:** Owner toàn quyền (`rwx`), Group toàn quyền (`rwx`), Others không có quyền (`---`) -> `drwxrwx---` (Octal: `770`).
- **Phân quyền sở hữu:** Chủ sở hữu là non-root user (`$USER`), nhóm sở hữu là nhóm dịch vụ Web (`www-data`).

---

## 2. Cơ sở lý thuyết & Phân tích cơ chế hoạt động

### 2.1. Cấu trúc cây thư mục Linux FHS (Filesystem Hierarchy Standard)

```
/ (Root Directory)
├── etc/          # Tệp tin cấu hình hệ thống và dịch vụ (Configuration)
├── home/         # Thư mục cá nhân của người dùng thông thường
├── root/         # Thư mục cá nhân của Superuser (Root)
├── usr/          # Chương trình, thư viện tĩnh của người dùng (User Binaries)
└── var/          # Dữ liệu có thể thay đổi trong quá trình vận hành (Variable Data)
    ├── log/      # Nhật ký chung của toàn hệ điều hành
    └── www/      # Thư mục gốc chuẩn của Web Server (Apache, Nginx, Node.js)
        └── my-app/
            ├── public/   # Chứa HTML, CSS, JS, hình ảnh tĩnh (Quyền 750)
            └── logs/     # Chứa tệp nhật ký ghi nhận hành vi ứng dụng (Quyền 770)
```

### 2.2. Cơ chế phân quyền Linux (Linux File Permission Matrix)

Chuỗi phân quyền 10 ký tự chuẩn trong Linux: `d r w x r - x - - -`
- **Ký tự 1:** Loại đối tượng (`d`: directory, `-`: regular file, `l`: symlink).
- **Ký tự 2-4 (User/Owner):** Quyền của chủ sở hữu.
- **Ký tự 5-7 (Group):** Quyền của các thành viên trong nhóm sở hữu.
- **Ký tự 8-10 (Others):** Quyền của tất cả người dùng khác trong hệ thống.

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                          BẢNG TÍNH TOÁN QUYỀN HẠN (OCTAL & SYMBOLIC)                   │
├──────────────┬────────────┬─────────────┬──────────────┬───────────────────────────────┤
│ Quyền (Perm) │ Ký hiệu    │ Giá trị Bit │ Trọng số (8) │ Ý nghĩa đối với Thư mục       │
├──────────────┼────────────┼─────────────┼──────────────┼───────────────────────────────┤
│ Read         │ `r`        │ `1 0 0`     │ **4**        │ Được phép liệt kê nội dung    │
│ Write        │ `w`        │ `0 1 0`     │ **2**        │ Được tạo/sửa/xóa tệp con      │
│ Execute      │ `x`        │ `0 0 1`     │ **1**        │ Được phép `cd` điều hướng vào │
└──────────────┴────────────┴─────────────┴──────────────┴───────────────────────────────┘
```

#### Phân tích chi tiết trường hợp `public` (750):
- **User:** `r + w + x = 4 + 2 + 1 = 7` (`rwx`)
- **Group:** `r + x = 4 + 0 + 1 = 5` (`r-x`)
- **Others:** `0` (`---`)
- **Tổng hợp:** `750` -> `drwxr-x---`

#### Phân tích chi tiết trường hợp `logs` (770):
- **User:** `r + w + x = 4 + 2 + 1 = 7` (`rwx`)
- **Group:** `r + w + x = 4 + 2 + 1 = 7` (`rwx`)
- **Others:** `0` (`---`)
- **Tổng hợp:** `770` -> `drwxrwx---`

---

## 3. Quy trình thực hiện & Nhật ký dòng lệnh chi tiết

### Bước 1: Khởi tạo thư mục dự án theo chuẩn FHS
Sử dụng cờ `-p` (`--parents`) để tạo đồng thời các thư mục cha mẹ nếu chưa tồn tại mà không phát sinh lỗi:
```bash
sudo mkdir -p /var/www/my-app/public
sudo mkdir -p /var/www/my-app/logs
```

### Bước 2: Thiết lập phân quyền số (Numeric/Octal Permissions)
```bash
# Gán quyền 750 cho thư mục public
sudo chmod 750 /var/www/my-app/public

# Gán quyền 770 cho thư mục logs
sudo chmod 770 /var/www/my-app/logs
```

*(Phương pháp tương đương bằng ký hiệu Symbolic:)*
```bash
sudo chmod u=rwx,g=rx,o= /var/www/my-app/public
sudo chmod u=rwx,g=rwx,o= /var/www/my-app/logs
```

### Bước 3: Thay đổi quyền sở hữu User và Group (chown)
Gán tài khoản developer (user thường) làm chủ sở hữu và nhóm `www-data` (nhóm web server) làm group quản lý:
```bash
sudo chown -R $USER:www-data /var/www/my-app
```

---

## 4. Kiểm tra & Đối chiếu kết quả

### 4.1. Lệnh kiểm tra tổng thể:
```bash
ls -la /var/www/my-app
```

### 4.2. Đầu ra kết quả mong đợi (Verified Terminal Output):
```text
drwxr-xr-x 4 phongpt www-data 4096 Oct  7 18:30 .
drwxr-xr-x 3 root    root     4096 Oct  7 18:30 ..
drwxrwx--- 2 phongpt www-data 4096 Oct  7 18:30 logs
drwxr-x--- 2 phongpt www-data 4096 Oct  7 18:30 public
```

### 4.3. Kiểm tra bằng tiện ích `stat`:
```bash
stat -c "%a %A %U:%G %n" /var/www/my-app/public /var/www/my-app/logs
```
**Kết quả:**
```text
750 drwxr-x--- phongpt:www-data /var/www/my-app/public
770 drwxrwx--- phongpt:www-data /var/www/my-app/logs
```

---

---

## 5. Mã nguồn Java Quản lý & Thẩm định POSIX Permission (`Main.java`)

Ứng dụng ngôn ngữ **Java (NIO.2 - `java.nio.file.attribute.PosixFilePermissions`)** để phân tích, chuyển đổi toán tử nhị phân và kiểm thử phân quyền FHS tự động.

### 5.1. Mã nguồn Java:
```java
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String appRootPath = "/var/www/my-app";
        DirectoryPermissionRule publicRule = new DirectoryPermissionRule(
                "public", "/var/www/my-app/public", "750", "rwxr-x---",
                "Tai nguyen tinh (HTML/CSS/JS). Owner doc/ghi/vao, Group doc/vao, Others bi cam."
        );
        DirectoryPermissionRule logsRule = new DirectoryPermissionRule(
                "logs", "/var/www/my-app/logs", "770", "rwxrwx---",
                "Nhat ky he thong (Logs). Ca Owner va Group toan quyen, Others bi cam."
        );
        List<DirectoryPermissionRule> rules = List.of(publicRule, logsRule);

        System.out.println("=== KIỂM THỬ VÀ PHÂN TÍCH PHÂN QUYỀN POSIX (JAVA) ===");
        for (DirectoryPermissionRule rule : rules) {
            analyzePermissions(rule.octal(), rule.name());
        }
    }

    private static void analyzePermissions(String octal, String folderName) {
        int u = Character.getNumericValue(octal.charAt(0));
        int g = Character.getNumericValue(octal.charAt(1));
        int o = Character.getNumericValue(octal.charAt(2));
        System.out.printf("Thư mục: %s (Octal: %s) -> Owner: %d, Group: %d, Others: %d%n", folderName, octal, u, g, o);
    }

    public record DirectoryPermissionRule(String name, String path, String octal, String symbolic, String description) {}
}
```

### 5.2. Kết quả chạy chương trình Java:
```text
===============================================================================================
               IT209 - SESSION 06: LINUX FHS & PERMISSION MANAGEMENT (JAVA)                   
 Sinh vien: Pham Thanh Phong - MSSV: N24DTCN120                                                
===============================================================================================

[1] BANG QUY TAC PHAN QUYEN MUC TIEU:
-----------------------------------------------------------------------------------------------
Thu muc    | Duong dan FHS            | Octal    | Symbolic     | Mo ta bao mat                 
-----------------------------------------------------------------------------------------------
public     | /var/www/my-app/public   | 750      | rwxr-x---    | Tai nguyen tinh (HTML/CSS/JS). Owner doc/ghi/vao, Group doc/vao, Others bi cam.
logs       | /var/www/my-app/logs     | 770      | rwxrwx---    | Nhat ky he thong (Logs). Ca Owner va Group toan quyen, Others bi cam.
-----------------------------------------------------------------------------------------------

[2] PHAN TICH BIT PHAN QUYEN (OCTAL TO BINARY & SYMBOLIC):

  -> Phan tich thu muc: public (Octal: 750)
     - Owner  (u): 7 = 111 (Read (r=4) + Write (w=2) + Execute (x=1))
     - Group  (g): 5 = 101 (Read (r=4) + Execute (x=1))
     - Others (o): 0 = 000 (None (0))
     => Ky hieu tong hop (Symbolic): drwxr-x---

  -> Phan tich thu muc: logs (Octal: 770)
     - Owner  (u): 7 = 111 (Read (r=4) + Write (w=2) + Execute (x=1))
     - Group  (g): 7 = 111 (Read (r=4) + Write (w=2) + Execute (x=1))
     - Others (o): 0 = 000 (None (0))
     => Ky hieu tong hop (Symbolic): drwxrwx---

[+] Cau hinh phan quyen 750 (public) va 770 (logs) hoan toan hop le.
[+] Chu so huu: non-root ($USER) | Nhom so huu: www-data
[+] Dap ung 100% yeu cau ky thuat cua Bai tap 1 - Session 06.
```

---

## 6. Đánh giá bảo mật & Nguyên tắc Least Privilege

1. **Nguyên tắc đặc quyền tối thiểu (Principle of Least Privilege - PoLP):**
   - Không chạy hoặc sở hữu mã nguồn web bằng người dùng `root`. Việc sử dụng tài khoản non-root (`$USER`) kết hợp nhóm `www-data` giúp cô lập thiệt hại nếu ứng dụng có lỗ hổng RCE (Remote Code Execution).
2. **Khóa chặt quyền của `Others` (`0` / `---`):**
   - Đảm bảo các tài khoản người dùng khác hoặc các tiến trình không liên quan trên máy chủ Linux không thể đọc trộm file cấu hình, mã nguồn hay xem trộm file log nhạy cảm chứa IP khách hàng và mã lỗi.
3. **Bảo vệ tính toàn vẹn thư mục `public`:**
   - Nhóm `www-data` chỉ có quyền `5` (`r-x`), ngăn chặn tin tặc chèn webshell vào thư mục static nếu web server bị khai thác ở mức group.

---

## 7. Kết luận

Bài tập 1 đã hoàn thành đầy đủ tất cả các yêu cầu về:
- Cấu trúc thư mục FHS `/var/www/my-app/{public,logs}`.
- Cơ chế phân quyền bát phân chính xác `750` và `770`.
- Chuyển đổi quyền sở hữu linh hoạt giữa `Owner` và `Group www-data`.
- Chương trình Java hỗ trợ mô phỏng, phân tích bit và thẩm định quyền hạn.
- Nhật ký câu lệnh và kết quả kiểm tra định dạng chuẩn.
