#!/usr/bin/env bash
# ==============================================================================
# Học phần: IT209 - Phát triển ứng dụng Web / Cloud Infrastructure
# Session: 06 - Khảo sát FHS và Phân quyền File/Folder nâng cao
# Bài tập 1: Phân quyền thư mục Web tĩnh (/public) và Nhật ký hệ thống (/logs)
# Sinh viên thực hiện: Phạm Thanh Phong - MSSV: N24DTCN120
# ==============================================================================

set -euo pipefail

APP_ROOT="/var/www/my-app"
PUBLIC_DIR="${APP_ROOT}/public"
LOGS_DIR="${APP_ROOT}/logs"
WEB_USER="${SUDO_USER:-$USER}"
WEB_GROUP="www-data"

echo "======================================================================"
echo " [IT209 - Session 06 - BT1] BẮT ĐẦU THIẾT LẬP CẤU TRÚC VÀ PHÂN QUYỀN "
echo "======================================================================"
echo "[INFO] User sở hữu: ${WEB_USER}"
echo "[INFO] Nhóm sở hữu: ${WEB_GROUP}"
echo "[INFO] Thư mục gốc: ${APP_ROOT}"
echo "----------------------------------------------------------------------"

# 1. Khởi tạo cấu trúc thư mục theo tiêu chuẩn Linux FHS (/var/www/...)
echo "[1/4] Khởi tạo cây thư mục /var/www/my-app/public và /var/www/my-app/logs..."
sudo mkdir -p "${PUBLIC_DIR}"
sudo mkdir -p "${LOGS_DIR}"

# Đảm bảo nhóm www-data tồn tại trong hệ thống (phổ biến trên Debian/Ubuntu/CentOS)
if ! getent group "${WEB_GROUP}" > /dev/null 2>&1; then
    echo "[INFO] Tạo nhóm ${WEB_GROUP} cho Web Server..."
    sudo groupadd "${WEB_GROUP}"
fi

# 2. Tạo các tệp dữ liệu mẫu
echo "[2/4] Tạo tệp dữ liệu mẫu index.html và app.log..."
cat << 'EOF' | sudo tee "${PUBLIC_DIR}/index.html" > /dev/null
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>IT209 - Session 06 - My App</title>
</head>
<body>
    <h1>Ứng dụng Web tĩnh IT209</h1>
    <p>Sinh viên: Phạm Thanh Phong - MSSV: N24DTCN120</p>
    <p>Trạng thái: Hoạt động bình thường</p>
</body>
</html>
EOF

cat << 'EOF' | sudo tee "${LOGS_DIR}/app.log" > /dev/null
[2026-10-07 18:30:00] [INFO] [System]: Application initialized successfully.
[2026-10-07 18:30:01] [INFO] [Router]: Route /public loaded (Static content).
[2026-10-07 18:30:02] [SECURITY] [Auth]: Access control enforced (750 for public, 770 for logs).
EOF

# 3. Phân quyền thư mục và tệp tin theo yêu cầu
echo "[3/4] Áp dụng phân quyền bảo mật (chmod & chown)..."

# Phân quyền thư mục public: Owner rwx (7), Group r-x (5), Others --- (0) => 750
# Ký hiệu Symbolic: u=rwx,g=rx,o=
sudo chmod 750 "${PUBLIC_DIR}"
sudo chmod 640 "${PUBLIC_DIR}/index.html"

# Phân quyền thư mục logs: Owner rwx (7), Group rwx (7), Others --- (0) => 770
# Ký hiệu Symbolic: u=rwx,g=rwx,o=
sudo chmod 770 "${LOGS_DIR}"
sudo chmod 660 "${LOGS_DIR}/app.log"

# Đổi chủ sở hữu (Owner) sang non-root user và Group sang www-data
sudo chown -R "${WEB_USER}:${WEB_GROUP}" "${APP_ROOT}"

echo "----------------------------------------------------------------------"
echo "[4/4] KIỂM TRA KẾT QUẢ PHÂN QUYỀN HỆ THỐNG:"
echo "----------------------------------------------------------------------"
ls -la "${APP_ROOT}"

echo "======================================================================"
echo "[SUCCESS] Thiết lập hoàn tất theo đúng tiêu chuẩn yêu cầu!"
echo "======================================================================"
