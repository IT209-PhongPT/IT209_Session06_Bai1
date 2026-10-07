#!/usr/bin/env bash
# ==============================================================================
# Script kiểm tra tính đúng đắn của phân quyền thư mục và tệp tin
# Học phần: IT209 - Session 06 - BT1
# Sinh viên: Phạm Thanh Phong - MSSV: N24DTCN120
# ==============================================================================

APP_ROOT="/var/www/my-app"

echo "=== [KIỂM TRA CẤU TRÚC VÀ PHÂN QUYỀN: ${APP_ROOT}] ==="
echo ""
echo "1. Chi tiết phân quyền thư mục gốc và thư mục con:"
ls -la "${APP_ROOT}"
echo ""

echo "2. Phân quyền chi tiết định dạng Octal & Tên nhóm:"
stat -c "%a %A %U:%G %n" "${APP_ROOT}/public"
stat -c "%a %A %U:%G %n" "${APP_ROOT}/logs"
echo ""

echo "3. Kiểm tra nội dung chi tiết bên trong:"
ls -la "${APP_ROOT}/public"
ls -la "${APP_ROOT}/logs"
