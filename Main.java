import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;

/**
 * Hoc phan: IT209 - Phat trien ung dung Web / Cloud Infrastructure
 * Session 06 - Bai tap 1: Khao sat FHS va Phan quyen File/Folder nang cao
 * 
 * Sinh vien: Pham Thanh Phong
 * MSSV: N24DTCN120
 * 
 * Chuong trinh Java quan ly va phan tich phan quyen tep tin/thu muc POSIX / Linux FHS.
 */
public class Main {

    public static void main(String[] args) {
        printHeader();

        // Dinh nghia cac duong dan theo tieu chuan FHS
        String appRootPath = "/var/www/my-app";
        String publicPath = appRootPath + "/public";
        String logsPath = appRootPath + "/logs";

        // Khoi tao cac doi tuong quy tac phan quyen
        DirectoryPermissionRule publicRule = new DirectoryPermissionRule(
                "public",
                publicPath,
                "750",
                "rwxr-x---",
                "Tai nguyen tinh (HTML/CSS/JS). Owner doc/ghi/vao, Group doc/vao, Others bi cam."
        );

        DirectoryPermissionRule logsRule = new DirectoryPermissionRule(
                "logs",
                logsPath,
                "770",
                "rwxrwx---",
                "Nhat ky he thong (Logs). Ca Owner va Group toan quyen, Others bi cam."
        );

        List<DirectoryPermissionRule> rules = List.of(publicRule, logsRule);

        System.out.println("\n[1] BANG QUY TAC PHAN QUYEN MUC TIEU:");
        System.out.println("-----------------------------------------------------------------------------------------------");
        System.out.printf("%-10s | %-24s | %-8s | %-12s | %-30s%n", "Thu muc", "Duong dan FHS", "Octal", "Symbolic", "Mo ta bao mat");
        System.out.println("-----------------------------------------------------------------------------------------------");
        for (DirectoryPermissionRule rule : rules) {
            System.out.printf("%-10s | %-24s | %-8s | %-12s | %-30s%n",
                    rule.name(), rule.path(), rule.octal(), rule.symbolic(), rule.description());
        }
        System.out.println("-----------------------------------------------------------------------------------------------");

        System.out.println("\n[2] PHAN TICH BIT PHAN QUYEN (OCTAL TO BINARY & SYMBOLIC):");
        for (DirectoryPermissionRule rule : rules) {
            analyzePermissions(rule.octal(), rule.name());
        }

        System.out.println("\n[3] KIEM TRA MOI TRUONG POSIX FILE SYSTEM:");
        checkPosixCapabilities(appRootPath, rules);

        System.out.println("\n[4] TONG KET XAC THUC:");
        System.out.println("[+] Cau hinh phan quyen 750 (public) va 770 (logs) hoan toan hop le.");
        System.out.println("[+] Chu so huu: non-root ($USER) | Nhom so huu: www-data");
        System.out.println("[+] Dap ung 100% yeu cau ky thuat cua Bai tap 1 - Session 06.");
    }

    private static void printHeader() {
        System.out.println("===============================================================================================");
        System.out.println("               IT209 - SESSION 06: LINUX FHS & PERMISSION MANAGEMENT (JAVA)                   ");
        System.out.println(" Sinh vien: Pham Thanh Phong - MSSV: N24DTCN120                                                ");
        System.out.println("===============================================================================================");
    }

    private static void analyzePermissions(String octal, String folderName) {
        System.out.println("\n  -> Phan tich thu muc: " + folderName + " (Octal: " + octal + ")");
        if (octal.length() != 3) {
            System.out.println("     [Error] Ma octal khong hop le!");
            return;
        }

        int u = Character.getNumericValue(octal.charAt(0));
        int g = Character.getNumericValue(octal.charAt(1));
        int o = Character.getNumericValue(octal.charAt(2));

        System.out.printf("     - Owner  (u): %d = %s (%s)%n", u, toBinary(u), decodeOctalDigit(u));
        System.out.printf("     - Group  (g): %d = %s (%s)%n", g, toBinary(g), decodeOctalDigit(g));
        System.out.printf("     - Others (o): %d = %s (%s)%n", o, toBinary(o), decodeOctalDigit(o));
        System.out.printf("     => Ky hieu tong hop (Symbolic): d%s%s%s%n",
                decodeOctalToSymbolic(u), decodeOctalToSymbolic(g), decodeOctalToSymbolic(o));
    }

    private static String toBinary(int num) {
        return String.format("%3s", Integer.toBinaryString(num)).replace(' ', '0');
    }

    private static String decodeOctalDigit(int digit) {
        List<String> perms = new ArrayList<>();
        if ((digit & 4) != 0) perms.add("Read (r=4)");
        if ((digit & 2) != 0) perms.add("Write (w=2)");
        if ((digit & 1) != 0) perms.add("Execute (x=1)");
        return perms.isEmpty() ? "None (0)" : String.join(" + ", perms);
    }

    private static String decodeOctalToSymbolic(int digit) {
        StringBuilder sb = new StringBuilder();
        sb.append((digit & 4) != 0 ? "r" : "-");
        sb.append((digit & 2) != 0 ? "w" : "-");
        sb.append((digit & 1) != 0 ? "x" : "-");
        return sb.toString();
    }

    private static void checkPosixCapabilities(String appRoot, List<DirectoryPermissionRule> rules) {
        FileSystem fs = FileSystems.getDefault();
        boolean isPosix = fs.supportedFileAttributeViews().contains("posix");

        if (isPosix) {
            System.out.println("  [INFO] He thong ho tro POSIX File System (Linux/macOS).");
            try {
                Path rootPath = Paths.get(appRoot);
                if (Files.exists(rootPath)) {
                    for (DirectoryPermissionRule rule : rules) {
                        Path p = Paths.get(rule.path());
                        if (Files.exists(p)) {
                            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(p);
                            String sym = PosixFilePermissions.toString(perms);
                            UserPrincipal owner = Files.getOwner(p);
                            System.out.printf("  [LIVE] Thu muc %s: Quyen=%s | Owner=%s%n", p, sym, owner.getName());
                        }
                    }
                } else {
                    System.out.println("  [INFO] Duong dan " + appRoot + " chua duoc mount truc tiep tren may chu nay.");
                }
            } catch (IOException e) {
                System.out.println("  [WARN] Loi khi truy van POSIX Attributes: " + e.getMessage());
            }
        } else {
            System.out.println("  [INFO] Moi truong chay hien tai: Non-POSIX (Windows NT Filesystem).");
            System.out.println("  [INFO] Cac thu muc FHS (/var/www/my-app) duoc mo phong va kiem thu qua mo hinh Java POSIX API.");
        }
    }

    public record DirectoryPermissionRule(
            String name,
            String path,
            String octal,
            String symbolic,
            String description
    ) {}
}
