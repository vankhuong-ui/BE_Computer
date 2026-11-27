package com.example.MyProject.config;

import com.example.MyProject.AUTH.entity.Account;
import com.example.MyProject.AUTH.entity.Useraddresses;
import com.example.MyProject.AUTH.repository.AccountRepository;
import com.example.MyProject.AUTH.repository.UseraddressRepository;
import com.example.MyProject.ORDER.entity.Cart;
import com.example.MyProject.ORDER.entity.OrderItem;
import com.example.MyProject.ORDER.entity.Orders;
import com.example.MyProject.ORDER.entity.Payment;
import com.example.MyProject.ORDER.repository.CartRepository;
import com.example.MyProject.ORDER.repository.OrdersRepository;
import com.example.MyProject.ORDER.repository.PaymentRepository;
import com.example.MyProject.PRODUCT.entity.Branch;
import com.example.MyProject.PRODUCT.entity.Brands;
import com.example.MyProject.PRODUCT.entity.Categories;
import com.example.MyProject.PRODUCT.entity.Inventory;
import com.example.MyProject.PRODUCT.entity.Products;
import com.example.MyProject.PRODUCT.repository.BranchRepository;
import com.example.MyProject.PRODUCT.repository.BrandsRepository;
import com.example.MyProject.PRODUCT.repository.CategoriesRepository;
import com.example.MyProject.PRODUCT.repository.InventoryRepository;
import com.example.MyProject.PRODUCT.repository.ProductRepository;
import com.example.MyProject.REVIEW.entity.RepairBooking;
import com.example.MyProject.REVIEW.entity.Review;
import com.example.MyProject.REVIEW.entity.WarrantyClaim;
import com.example.MyProject.REVIEW.repository.RepairBookingRepository;
import com.example.MyProject.REVIEW.repository.ReviewRepository;
import com.example.MyProject.REVIEW.repository.WarrantyClaimRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Idempotent cross-table demo data seeder for HK COMPUTER.
 * Can be disabled in production by setting APP_SEED_ENABLED=false (app.seed.enabled=false).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataSeeder implements CommandLineRunner {

    private static final Set<String> CORE_BRANCH_CODES = Set.of("HN-THAIHA", "HCM-QUAN10", "DN-HAICHAU");

    private final AccountRepository accountRepository;
    private final UseraddressRepository useraddressRepository;
    private final PasswordEncoder passwordEncoder;
    private final BranchRepository branchRepository;
    private final BrandsRepository brandsRepository;
    private final CategoriesRepository categoriesRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CartRepository cartRepository;
    private final OrdersRepository ordersRepository;
    private final PaymentRepository paymentRepository;
    private final ReviewRepository reviewRepository;
    private final WarrantyClaimRepository warrantyClaimRepository;
    private final RepairBookingRepository repairBookingRepository;

    @Override
    public void run(String... args) {
        seedAccountIfAbsent("admin@hkcomputer.vn", "Quản trị viên HK", "Admin@123", "ADMIN");
        Account customer1 = seedAccountIfAbsent("khachhang@hkcomputer.vn", "Nguyễn Văn An", "User@123", "USER");
        Account customer2 = seedAccountIfAbsent("khach@hkcomputer.vn", "Trần Minh Hoàng", "Khach@123", "USER");

        List<Branch> coreBranches = ensureCoreBranches();
        seedCatalogAndCoreBranchInventory(coreBranches);
        seedOperationalDemoData(customer1, customer2, coreBranches);
    }

    private Account seedAccountIfAbsent(String gmail, String accountName, String rawPassword, String role) {
        return accountRepository.findBygmail(gmail).orElseGet(() -> {
            Account account = new Account();
            account.setGmail(gmail);
            account.setAccountname(accountName);
            account.setPassword(passwordEncoder.encode(rawPassword));
            account.setEnabled(true);
            account.setRole(role);
            return accountRepository.save(account);
        });
    }

    private List<Branch> ensureCoreBranches() {
        Branch hn = ensureBranch(
                "HN-THAIHA", "HK COMPUTER - Thái Hà", "Hà Nội", "Q. Đống Đa",
                "123 Thái Hà, P. Trung Liệt, Q. Đống Đa, Hà Nội", "024 7300 6801",
                "8:00 - 21:00", "Showroom Chính · Sửa Laptop trong ngày · Bảo hành"
        );
        Branch hcm = ensureBranch(
                "HCM-QUAN10", "HK COMPUTER - Quận 10", "TP.HCM", "Q. 10",
                "456 Cách Mạng Tháng 8, P. 11, Q. 10, TP.HCM", "028 7300 6804",
                "8:00 - 21:00", "Showroom Miền Nam · Build PC · Bảo hành 1 đổi 1"
        );
        Branch dn = ensureBranch(
                "DN-HAICHAU", "HK COMPUTER - Hải Châu", "Đà Nẵng", "Q. Thanh Khê",
                "142 Hàm Nghi, P. Thạc Gián, Q. Thanh Khê, Đà Nẵng", "0236 730 6807",
                "8:00 - 20:30", "Showroom Miền Trung · Sửa chữa Laptop & PC · Bảo hành"
        );

        for (Branch b : branchRepository.findAllByOrderByIdAsc()) {
            if (b.isActive() && (b.getCode() == null || !CORE_BRANCH_CODES.contains(b.getCode().toUpperCase()))) {
                b.setActive(false);
                branchRepository.save(b);
            }
        }
        return List.of(hn, hcm, dn);
    }

    private Branch ensureBranch(
            String code, String name, String city, String district,
            String address, String phone, String openingHours, String services
    ) {
        return branchRepository.findByCodeIgnoreCase(code).map(existing -> {
            if (!existing.isActive()) {
                existing.setActive(true);
                return branchRepository.save(existing);
            }
            return existing;
        }).orElseGet(() -> {
            Branch b = new Branch();
            b.setCode(code);
            b.setName(name);
            b.setCity(city);
            b.setDistrict(district);
            b.setAddress(address);
            b.setPhone(phone);
            b.setOpeningHours(openingHours);
            b.setServices(services);
            b.setActive(true);
            return branchRepository.save(b);
        });
    }

    private void seedCatalogAndCoreBranchInventory(List<Branch> coreBranches) {
        if (coreBranches.isEmpty()) return;

        Categories catLaptopOffice = ensureCategory("Laptop Văn phòng & Doanh nhân", "laptop-van-phong-doanh-nhan", "LAPTOP");
        Categories catLaptopGaming = ensureCategory("Laptop Gaming & Đồ họa", "laptop-gaming-do-hoa", "LAPTOP");
        Categories catPcDesktop = ensureCategory("PC Gaming & Workstation", "pc-gaming-workstation", "PC");
        Categories catMonitor = ensureCategory("Màn hình máy tính", "man-hinh-may-tinh", "PC");
        Categories catMouse = ensureCategory("Chuột máy tính", "chuot-may-tinh", "ACCESSORY");
        Categories catKeyboard = ensureCategory("Bàn phím cơ", "ban-phim-co", "ACCESSORY");
        Categories catRamSsd = ensureCategory("Linh kiện RAM & SSD", "linh-kien-ram-ssd", "ACCESSORY");

        Brands brDell = ensureBrand("Dell", "dell");
        Brands brAsus = ensureBrand("Asus", "asus");
        Brands brLenovo = ensureBrand("Lenovo", "lenovo");
        Brands brHp = ensureBrand("HP", "hp");
        Brands brApple = ensureBrand("Apple", "apple");
        Brands brAcer = ensureBrand("Acer", "acer");
        Brands brLg = ensureBrand("LG", "lg");
        Brands brLogitech = ensureBrand("Logitech", "logitech");
        Brands brKeychron = ensureBrand("Keychron", "keychron");
        Brands brKingston = ensureBrand("Kingston", "kingston");
        Brands brSamsung = ensureBrand("Samsung", "samsung");

        Products pDell7490 = ensureProduct(
                brDell, catLaptopOffice,
                "Laptop Dell Latitude 7490 Core i7 — Bản Doanh Nhân Siêu Bền",
                "laptop-dell-latitude-7490",
                new BigDecimal("8990000"),
                "CPU: Intel Core i7-8650U (4 nhân 8 luồng) | RAM: 16GB DDR4 | Ổ cứng: 512GB SSD NVMe | VGA: Intel UHD Graphics 620 | Màn hình: 14 inch FHD IPS chống chói | Pin: 60Wh sạc nhanh | Khung hợp kim Magie 1.4kg · Bảo hành điện tử 24 tháng",
                "https://hethong.24hlaptop.com/image_event/laptop-dell-latitude-7490-1741774237237.jpg",
                "Mẫu Laptop doanh nhân nổi bật trên Banner ưu đãi của HK COMPUTER: khung hợp kim Magie carbon đạt chuẩn quân đội MIL-STD-810G, phím gõ êm, tối ưu cho văn phòng và lập trình."
        );
        Products pDell5500 = ensureProduct(
                brDell, catLaptopOffice,
                "Laptop Dell Latitude 5500 Core i5 — Màn 15.6 inch Kèm Phím Số",
                "laptop-dell-latitude-5500",
                new BigDecimal("8490000"),
                "CPU: Intel Core i5-8365U | RAM: 16GB DDR4 | Ổ cứng: 512GB SSD NVMe | VGA: Intel UHD Graphics 620 | Màn hình: 15.6 inch FHD IPS rộng rãi | Pin: 68Wh bền bỉ | Bàn phím Full-size có cụm phím số kế toán",
                "https://hethong.24hlaptop.com/image_event/laptop-dell-latitude-5500-1741774260012.jpg",
                "Mẫu Laptop bán chạy trên Banner HK COMPUTER dành cho sinh viên kinh tế, kế toán và nhân sự văn phòng: màn 15.6 inch sắc nét kèm phím số riêng và đầy đủ cổng kết nối."
        );
        Products pAsusOled = ensureProduct(
                brAsus, catLaptopOffice,
                "Laptop Asus Vivobook 15 OLED A1505VA i5-13500H",
                "laptop-asus-vivobook-15-oled-a1505va",
                new BigDecimal("16490000"),
                "CPU: Intel Core i5-13500H | RAM: 16GB DDR4 | Ổ cứng: 512GB SSD PCIe 4.0 | VGA: Intel Iris Xe | Màn hình: 15.6 inch FHD OLED 100% DCI-P3 | Pin: 50Wh sạc nhanh 65W",
                "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=700&q=80",
                "Laptop văn phòng & sáng tạo nội dung màn hình OLED rực rỡ chuẩn điện ảnh, vi xử lý dòng H hiệu năng mạnh mẽ trong thân máy 1.7kg."
        );
        Products pRogG16 = ensureProduct(
                brAsus, catLaptopGaming,
                "Laptop Gaming Asus ROG Strix G16 G614JU i7-13650HX RTX 4060",
                "laptop-gaming-asus-rog-strix-g16-rtx-4060",
                new BigDecimal("31990000"),
                "CPU: Intel Core i7-13650HX | RAM: 16GB DDR5 4800MHz | Ổ cứng: 1TB SSD Gen4 | VGA: NVIDIA GeForce RTX 4060 8GB (140W) | Màn hình: 16 inch FHD+ 165Hz 100% sRGB | Pin: 90Wh",
                "https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=700&q=80",
                "Chiến binh eSports & dựng phim 3D hàng đầu phân khúc với GPU RTX 4060 full TGP 140W, hệ thống tản nhiệt Tri-Fan mát lạnh."
        );
        Products pLenovoLoq = ensureProduct(
                brLenovo, catLaptopGaming,
                "Laptop Gaming Lenovo LOQ 15IAX9 i5-12450HX RTX 3050 6GB",
                "laptop-gaming-lenovo-loq-15iax9-rtx-3050",
                new BigDecimal("19490000"),
                "CPU: Intel Core i5-12450HX | RAM: 16GB DDR5 | Ổ cứng: 512GB SSD NVMe | VGA: RTX 3050 6GB GDDR6 | Màn hình: 15.6 inch FHD IPS 144Hz 100% sRGB | MUX Switch",
                "https://images.unsplash.com/photo-1593642632823-8f785ba67e45?w=700&q=80",
                "Mẫu Laptop Gaming quốc dân chuẩn màu 100% sRGB vừa chiến mượt các tựa game AAA vừa thiết kế đồ họa 2D/Premiere chuẩn xác."
        );
        Products pMacM3 = ensureProduct(
                brApple, catLaptopOffice,
                "Apple MacBook Air 13 inch M3 16GB 256GB Chính hãng VN/A",
                "apple-macbook-air-13-m3-16gb-256gb",
                new BigDecimal("26990000"),
                "CPU: Apple M3 8 nhân | RAM: 16GB Unified Memory | Ổ cứng: 256GB SSD | Màn hình: 13.6 inch Liquid Retina True Tone | Pin: Lên tới 18 giờ | Vỏ nhôm 1.24kg",
                "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=700&q=80",
                "Phiên bản RAM 16GB tiêu chuẩn của Apple giúp đa nhiệm mượt mà hàng chục tab và ứng dụng sáng tạo, pin trọn ngày."
        );
        Products pHpPav = ensureProduct(
                brHp, catLaptopOffice,
                "Laptop HP Pavilion 15 eg3093TU i5-1335U 16GB 512GB",
                "laptop-hp-pavilion-15-eg3093tu",
                new BigDecimal("15290000"),
                "CPU: Intel Core i5-1335U | RAM: 16GB DDR4 | Ổ cứng: 512GB SSD | VGA: Intel Iris Xe | Màn hình: 15.6 inch FHD IPS BrightView | Âm thanh B&O",
                "https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=700&q=80",
                "Thiết kế vỏ kim loại thanh lịch tông bạc sáng, loa tinh chỉnh Bang & Olufsen sống động cho công việc văn phòng hiện đại."
        );
        Products pAcerNitro = ensureProduct(
                brAcer, catLaptopGaming,
                "Laptop Gaming Acer Nitro V ANV15-51 i5-13420H RTX 4050",
                "laptop-gaming-acer-nitro-v-15-rtx-4050",
                new BigDecimal("21990000"),
                "CPU: Intel Core i5-13420H | RAM: 16GB DDR5 | Ổ cứng: 512GB SSD Gen4 | VGA: RTX 4050 6GB DLSS 3 | Màn hình: 15.6 inch FHD IPS 144Hz | Thunderbolt 4",
                "https://images.unsplash.com/photo-1611078489935-0cb964de46d6?w=700&q=80",
                "Trang bị DLSS 3 Frame Generation thế hệ mới giúp tăng khung hình trong các tựa game nặng với mức giá dễ tiếp cận."
        );
        Products pPcTitan = ensureProduct(
                brAsus, catPcDesktop,
                "PC Gaming HK Titan i5-13400F / RAM 16GB / RTX 4060 8GB",
                "pc-gaming-hk-titan-i5-13400f-rtx-4060",
                new BigDecimal("18990000"),
                "CPU: Intel Core i5-13400F | RAM: 16GB DDR4 3600MHz RGB | Ổ cứng: 512GB NVMe Gen4 | VGA: Asus Dual RTX 4060 8GB | Main: B760M Wifi | Nguồn: 650W Bronze",
                "https://images.unsplash.com/photo-1587202372775-e229f172b9d7?w=700&q=80",
                "Bộ PC lắp sẵn tối ưu hiệu năng/giá thành tại HK COMPUTER, test stress 24h trước khi bàn giao và bảo hành linh kiện 36 tháng."
        );
        Products pPcDell = ensureProduct(
                brDell, catPcDesktop,
                "PC Đồng Bộ Văn Phòng Dell OptiPlex 7010 SFF Core i5-13500",
                "pc-dong-bo-dell-optiplex-7010-sff-i5",
                new BigDecimal("13490000"),
                "CPU: Intel Core i5-13500 | RAM: 16GB DDR4 | Ổ cứng: 512GB SSD NVMe | VGA: Intel UHD 770 xuất 3 màn hình | Wi-Fi 6 + LAN Gigabit",
                "https://images.unsplash.com/photo-1593640408182-31c70c8268f5?w=700&q=80",
                "Máy bộ doanh nghiệp vận hành êm ái 24/7, tiết kiệm diện tích bàn làm việc và độ ổn định cao cho văn phòng công ty."
        );
        Products pMonLg = ensureProduct(
                brLg, catMonitor,
                "Màn Hình Gaming LG UltraGear 27GS60F-B 27 inch FHD IPS 180Hz 1ms",
                "man-hinh-lg-ultragear-27gs60f-27-180hz",
                new BigDecimal("3890000"),
                "Kích thước: 27 inch phẳng | Tấm nền: Fast IPS 99% sRGB | Tần số quét: 180Hz · 1ms GtG | HDR10 · G-Sync Compatible",
                "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=700&q=80",
                "Màn hình quốc dân 180Hz siêu mượt kết hợp chuẩn màu 99% sRGB rực rỡ cho cả game thủ FPS lẫn làm việc đa màn hình."
        );
        Products pMonDell = ensureProduct(
                brDell, catMonitor,
                "Màn Hình Đồ Họa Dell UltraSharp U2424H 24 inch FHD IPS 120Hz USB-C",
                "man-hinh-dell-ultrasharp-u2424h-24-120hz",
                new BigDecimal("6190000"),
                "Kích thước: 24 inch InfinityEdge | IPS 100% sRGB Delta E < 2 | Tần số quét: 120Hz | Chân xoay dọc 90° | Hub USB-C & RJ45",
                "https://images.unsplash.com/photo-1547119957-637f8679db1e?w=700&q=80",
                "Dòng UltraSharp cân màu sẵn từ nhà máy kèm cảm biến ánh sáng bảo vệ mắt ComfortView Plus thế hệ mới."
        );
        Products pMouseG304 = ensureProduct(
                brLogitech, catMouse,
                "Chuột Không Dây Gaming Logitech G304 Lightspeed",
                "chuot-khong-day-logitech-g304-lightspeed",
                new BigDecimal("790000"),
                "Không dây Lightspeed 1ms | Cảm biến HERO 12.000 DPI | Pin 250 giờ",
                "https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=600&q=80",
                "Chuột gaming không dây Logitech G304 Lightspeed siêu nhạy, trọng lượng nhẹ 99g phù hợp làm việc và chơi game."
        );
        Products pKeyK8 = ensureProduct(
                brKeychron, catKeyboard,
                "Bàn Phím Cơ Không Dây Keychron K8 Pro QMK/VIA RGB",
                "ban-phim-co-keychron-k8-pro-rgb",
                new BigDecimal("2190000"),
                "Bluetooth 5.1 & Type-C | Hot-swap Gateron Pro | Khung nhôm LED RGB",
                "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=600&q=80",
                "Bàn phím cơ TKL không dây hỗ trợ Mac & Windows, lót sẵn foam tiêu âm, keycap PBT Double-shot bền bỉ."
        );
        Products pRam16 = ensureProduct(
                brKingston, catRamSsd,
                "RAM Laptop Kingston Fury Impact 16GB DDR5 5600MHz",
                "ram-laptop-kingston-fury-impact-16gb-ddr5-5600",
                new BigDecimal("1450000"),
                "16GB DDR5 | Bus 5600MHz | CL40 SODIMM · Miễn phí lắp đặt",
                "https://images.unsplash.com/photo-1562976540-1502c2145186?w=600&q=80",
                "Nâng cấp đa nhiệm mượt mà cho Laptop Gaming & Đồ họa đời mới, miễn phí công lắp đặt lấy liền tại 3 chi nhánh HK COMPUTER."
        );
        Products pSsd990 = ensureProduct(
                brSamsung, catRamSsd,
                "Ổ Cứng SSD Samsung 990 PRO 1TB M.2 NVMe PCIe Gen4",
                "ssd-samsung-990-pro-1tb-nvme-gen4",
                new BigDecimal("2690000"),
                "1TB M.2 NVMe | Đọc 7450 MB/s | Bảo hành điện tử 36 tháng",
                "https://images.unsplash.com/photo-1597872200969-2b65d56bd16b?w=600&q=80",
                "Ổ cứng SSD NVMe tốc độ cao hàng đầu cho Laptop & PC, hỗ trợ clone Windows miễn phí khi nâng cấp tại cửa hàng."
        );

        Branch hn = coreBranches.get(0);
        Branch hcm = coreBranches.size() > 1 ? coreBranches.get(1) : hn;
        Branch dn = coreBranches.size() > 2 ? coreBranches.get(2) : hn;

        ensureBranchStock(pDell7490, hn, 8, 14);
        ensureBranchStock(pDell7490, hcm, 6, 11);
        ensureBranchStock(pDell7490, dn, 2, 5);

        ensureBranchStock(pDell5500, hn, 7, 12);
        ensureBranchStock(pDell5500, hcm, 9, 15);
        ensureBranchStock(pDell5500, dn, 0, 4);

        ensureBranchStock(pAsusOled, hn, 5, 6);
        ensureBranchStock(pAsusOled, hcm, 4, 8);
        ensureBranchStock(pAsusOled, dn, 3, 2);

        ensureBranchStock(pRogG16, hn, 2, 7);
        ensureBranchStock(pRogG16, hcm, 4, 5);
        ensureBranchStock(pRogG16, dn, 1, 2);

        ensureBranchStock(pLenovoLoq, hn, 6, 9);
        ensureBranchStock(pLenovoLoq, hcm, 5, 7);
        ensureBranchStock(pLenovoLoq, dn, 4, 3);

        ensureBranchStock(pMacM3, hn, 4, 10);
        ensureBranchStock(pMacM3, hcm, 6, 13);
        ensureBranchStock(pMacM3, dn, 2, 4);

        ensureBranchStock(pHpPav, hn, 6, 5);
        ensureBranchStock(pHpPav, hcm, 5, 6);
        ensureBranchStock(pHpPav, dn, 4, 2);

        ensureBranchStock(pAcerNitro, hn, 5, 8);
        ensureBranchStock(pAcerNitro, hcm, 3, 6);
        ensureBranchStock(pAcerNitro, dn, 2, 3);

        ensureBranchStock(pPcTitan, hn, 4, 9);
        ensureBranchStock(pPcTitan, hcm, 3, 7);
        ensureBranchStock(pPcTitan, dn, 2, 4);

        ensureBranchStock(pPcDell, hn, 8, 6);
        ensureBranchStock(pPcDell, hcm, 5, 4);
        ensureBranchStock(pPcDell, dn, 4, 2);

        ensureBranchStock(pMonLg, hn, 10, 12);
        ensureBranchStock(pMonLg, hcm, 8, 9);
        ensureBranchStock(pMonLg, dn, 5, 5);

        ensureBranchStock(pMonDell, hn, 5, 6);
        ensureBranchStock(pMonDell, hcm, 4, 5);
        ensureBranchStock(pMonDell, dn, 2, 2);

        ensureBranchStock(pMouseG304, hn, 18, 25);
        ensureBranchStock(pMouseG304, hcm, 15, 20);
        ensureBranchStock(pMouseG304, dn, 9, 10);

        ensureBranchStock(pKeyK8, hn, 7, 11);
        ensureBranchStock(pKeyK8, hcm, 6, 9);
        ensureBranchStock(pKeyK8, dn, 3, 4);

        ensureBranchStock(pRam16, hn, 14, 16);
        ensureBranchStock(pRam16, hcm, 12, 13);
        ensureBranchStock(pRam16, dn, 8, 6);

        ensureBranchStock(pSsd990, hn, 11, 18);
        ensureBranchStock(pSsd990, hcm, 9, 14);
        ensureBranchStock(pSsd990, dn, 5, 7);

        for (Products p : productRepository.findAll()) {
            for (Branch b : coreBranches) {
                ensureBranchStock(p, b, 5, 2);
            }
        }
    }

    private void ensureBranchStock(Products product, Branch branch, int qty, int selled) {
        if (product == null || branch == null) return;
        if (inventoryRepository.findByProducts_IdAndBranch_Id(product.getId(), branch.getId()).isPresent()) {
            return;
        }
        Inventory inv = new Inventory();
        inv.setProducts(product);
        inv.setBranch(branch);
        inv.setSoluong(qty);
        inv.setSelled(selled);
        inventoryRepository.save(inv);
    }

    private void seedOperationalDemoData(Account customer1, Account customer2, List<Branch> coreBranches) {
        if (customer1 == null || coreBranches.isEmpty()) return;

        Branch hn = coreBranches.get(0);
        Branch hcm = coreBranches.size() > 1 ? coreBranches.get(1) : hn;
        Branch dn = coreBranches.size() > 2 ? coreBranches.get(2) : hn;

        Useraddresses addr1Hn = ensureAddress(customer1,
                "Nguyễn Văn An", "0912345678",
                "Số 18 Hoàng Quốc Việt, Q. Cầu Giấy, Hà Nội", true);
        ensureAddress(customer1,
                "Nguyễn Văn An (Văn phòng HCM)", "0912345678",
                "Tòa nhà Viettel Complex, 285 Cách Mạng Tháng 8, Q. 10, TP.HCM", false);

        Useraddresses addr2Dn = customer2 != null
                ? ensureAddress(customer2,
                "Trần Minh Hoàng", "0987654321",
                "Số 88 Nguyễn Văn Linh, Q. Hải Châu, Đà Nẵng", true)
                : addr1Hn;

        Products dell7490 = productRepository.findBySlug("laptop-dell-latitude-7490").orElse(null);
        Products dell5500 = productRepository.findBySlug("laptop-dell-latitude-5500").orElse(null);
        Products asusOled = productRepository.findBySlug("laptop-asus-vivobook-15-oled-a1505va").orElse(null);
        Products hpPav = productRepository.findBySlug("laptop-hp-pavilion-15-eg3093tu").orElse(null);
        Products pcTitan = productRepository.findBySlug("pc-gaming-hk-titan-i5-13400f-rtx-4060").orElse(null);
        Products monLg = productRepository.findBySlug("man-hinh-lg-ultragear-27gs60f-27-180hz").orElse(null);
        Products mouse = productRepository.findBySlug("chuot-khong-day-logitech-g304-lightspeed").orElse(null);
        Products keyK8 = productRepository.findBySlug("ban-phim-co-keychron-k8-pro-rgb").orElse(null);
        Products ssd990 = productRepository.findBySlug("ssd-samsung-990-pro-1tb-nvme-gen4").orElse(null);

        if (cartRepository.findByAccountId(customer1.getId()).isEmpty() && dell5500 != null && mouse != null) {
            Cart c1 = new Cart();
            c1.setAccount(customer1);
            c1.setProducts(dell5500);
            c1.setQuality(1);
            cartRepository.save(c1);

            Cart c2 = new Cart();
            c2.setAccount(customer1);
            c2.setProducts(mouse);
            c2.setQuality(1);
            cartRepository.save(c2);
        }

        LocalDateTime now = LocalDateTime.now();

        if (ordersRepository.count() == 0) {
            Orders o1 = null;
            if (dell7490 != null && mouse != null) {
                o1 = createSampleOrder(addr1Hn, hn, "HOME_DELIVERY", "COMPLETED",
                        now.minusDays(5), List.of(dell7490, mouse), List.of(1, 1));
                saveSamplePayment(o1, "SEPAY_DEMO_1001", "SEPAY", o1.getTotalAmount(), "SUCCESS", "00", now.minusDays(5));

                reviewRepository.save(Review.builder()
                        .order(o1).product(dell7490).rating(5)
                        .comment("Máy Dell Latitude 7490 vỏ magie cực chắc chắn, bàn phím gõ rất thích, giao trong ngày tại Hà Nội và tự động kích hoạt bảo hành điện tử!")
                        .build());
                reviewRepository.save(Review.builder()
                        .order(o1).product(mouse).rating(5)
                        .comment("Chuột G304 nhạy, cầm vừa tay, đóng gói chống sốc rất kỹ.")
                        .build());
            }

            Orders o2 = null;
            if (dell5500 != null) {
                o2 = createSampleOrder(addr1Hn, hcm, "STORE_PICKUP", "COMPLETED",
                        now.minusDays(3), List.of(dell5500), List.of(1));
                saveSamplePayment(o2, "VNP_DEMO_2002", "VNPAY", o2.getTotalAmount(), "SUCCESS", "00", now.minusDays(3));

                reviewRepository.save(Review.builder()
                        .order(o2).product(dell5500).rating(5)
                        .comment("Màn hình 15.6 inch sắc nét kèm phím số kế toán cực tiện, nhận máy trực tiếp tại chi nhánh Quận 10 được hỗ trợ cài đặt miễn phí.")
                        .build());
            }

            Orders o3 = null;
            if (pcTitan != null && monLg != null) {
                o3 = createSampleOrder(addr2Dn, dn, "HOME_DELIVERY", "COMPLETED",
                        now.minusDays(2), List.of(pcTitan, monLg), List.of(1, 1));
                saveSamplePayment(o3, "SEPAY_DEMO_1003", "SEPAY", o3.getTotalAmount(), "SUCCESS", "00", now.minusDays(2));

                reviewRepository.save(Review.builder()
                        .order(o3).product(pcTitan).rating(5)
                        .comment("Bộ PC HK Titan đi dây gọn đẹp, chiến mượt mọi tựa game 1440p, giao tại Đà Nẵng đóng thùng gỗ an toàn.")
                        .build());
            }

            if (keyK8 != null && ssd990 != null) {
                Orders o4 = createSampleOrder(addr1Hn, hn, "HOME_DELIVERY", "COMPLETED",
                        now.minusHours(5), List.of(keyK8, ssd990), List.of(1, 1));
                saveSamplePayment(o4, "VNP_DEMO_2004", "VNPAY", o4.getTotalAmount(), "SUCCESS", "00", now.minusHours(5));
            }

            if (asusOled != null) {
                createSampleOrder(addr1Hn, hn, "HOME_DELIVERY", "UNPAID",
                        now.minusMinutes(8), List.of(asusOled), List.of(1));
            }

            if (hpPav != null) {
                createSampleOrder(addr2Dn, dn, "HOME_DELIVERY", "CANCELLED",
                        now.minusDays(4), List.of(hpPav), List.of(1));
            }

            if (warrantyClaimRepository.count() == 0) {
                if (o1 != null && mouse != null) {
                    warrantyClaimRepository.save(WarrantyClaim.builder()
                            .order(o1).product(mouse).account(customer1).unitNumber(1)
                            .serialCode("BH-" + o1.getId() + "-" + mouse.getId() + "-1")
                            .issueType("Kiểm tra đầu thu USB Lightspeed")
                            .description("Đầu thu USB đôi lúc nhận chập chờn trên cổng USB 3.0, nhờ kỹ thuật kiểm tra và đổi mới nếu lỗi.")
                            .contactPhone("0912345678")
                            .status("PENDING")
                            .warrantyMonths(12)
                            .warrantyExpireDate(o1.getPaidAt().plusMonths(12))
                            .build());
                }
                if (o2 != null && dell5500 != null) {
                    warrantyClaimRepository.save(WarrantyClaim.builder()
                            .order(o2).product(dell5500).account(customer1).unitNumber(1)
                            .serialCode("BH-" + o2.getId() + "-" + dell5500.getId() + "-1")
                            .issueType("Quạt tản nhiệt kêu nhẹ khi tải nặng")
                            .description("Khi chạy Excel file nặng quạt tản nhiệt phát tiếng rè nhẹ, nhờ bảo hành kiểm tra miễn phí.")
                            .contactPhone("0912345678")
                            .status("REPAIRING")
                            .adminNote("Đã tiếp nhận máy tại chi nhánh Quận 10, KTV đang vệ sinh cụm quạt và tra keo MX-4.")
                            .warrantyMonths(24)
                            .warrantyExpireDate(o2.getPaidAt().plusMonths(24))
                            .build());
                }
                if (o3 != null && pcTitan != null && customer2 != null) {
                    warrantyClaimRepository.save(WarrantyClaim.builder()
                            .order(o3).product(pcTitan).account(customer2).unitNumber(1)
                            .serialCode("BH-" + o3.getId() + "-" + pcTitan.getId() + "-1")
                            .issueType("Cập nhật BIOS & đồng bộ LED ARGB")
                            .description("Nhờ kỹ thuật cập nhật BIOS mới nhất và căn chỉnh đồng bộ màu quạt ARGB.")
                            .contactPhone("0987654321")
                            .status("COMPLETED")
                            .adminNote("Đã cập nhật BIOS ổn định, test tải 2 giờ nhiệt độ mát 62°C và bàn giao lại khách.")
                            .warrantyMonths(24)
                            .warrantyExpireDate(o3.getPaidAt().plusMonths(24))
                            .build());
                }
            }
        }

        if (repairBookingRepository.count() == 0) {
            repairBookingRepository.save(RepairBooking.builder()
                    .account(customer1)
                    .customerName("Nguyễn Văn An")
                    .customerPhone("0912345678")
                    .customerEmail(customer1.getGmail())
                    .serviceMethod("AT_STORE")
                    .branchName(hn.getName())
                    .deviceModel("Dell XPS 13 9310")
                    .serviceType("Vệ sinh & Tra keo tản nhiệt (150.000đ)")
                    .issueDescription("Máy nóng ở khu vực bàn phím khi chạy họp trực tuyến, cần vệ sinh lấy ngay trong ngày.")
                    .appointmentDate(now.plusDays(1).toLocalDate().toString())
                    .appointmentTime("09:30 - 11:00")
                    .status("PENDING")
                    .build());

            repairBookingRepository.save(RepairBooking.builder()
                    .account(customer1)
                    .customerName("Nguyễn Văn An")
                    .customerPhone("0912345678")
                    .customerEmail(customer1.getGmail())
                    .serviceMethod("AT_STORE")
                    .branchName(hcm.getName())
                    .deviceModel("MacBook Pro M1 14 inch")
                    .serviceType("Kiểm tra lỗi tổng quát (Miễn phí)")
                    .issueDescription("Pin tụt nhanh hơn bình thường, cần đo dung lượng cell pin và tư vấn thay mới.")
                    .appointmentDate(now.plusDays(2).toLocalDate().toString())
                    .appointmentTime("14:00 - 15:30")
                    .status("IN_PROGRESS")
                    .adminNote("KTV Huy đang đo chu kỳ pin và kiểm tra nguồn sạc USB-C.")
                    .build());

            repairBookingRepository.save(RepairBooking.builder()
                    .account(customer2)
                    .customerName("Trần Minh Hoàng")
                    .customerPhone("0987654321")
                    .customerEmail(customer2 != null ? customer2.getGmail() : null)
                    .serviceMethod("AT_HOME")
                    .branchName("Sửa tại nhà — 88 Nguyễn Văn Linh, Q. Hải Châu, Đà Nẵng")
                    .homeAddress("88 Nguyễn Văn Linh, Q. Hải Châu, Đà Nẵng")
                    .deviceModel("PC Workstation Đồ họa Core i7")
                    .serviceType("Nâng cấp RAM / Ổ cứng SSD")
                    .issueDescription("Nhờ kỹ thuật qua tận văn phòng lắp thêm ổ SSD Samsung 1TB và chuyển hệ điều hành.")
                    .appointmentDate(now.plusDays(1).toLocalDate().toString())
                    .appointmentTime("15:30 - 17:30")
                    .status("CONFIRMED")
                    .adminNote("Đã phân công KTV Phong chi nhánh Đà Nẵng liên hệ trước 30 phút.")
                    .build());

            repairBookingRepository.save(RepairBooking.builder()
                    .customerName("Lê Thu Hương")
                    .customerPhone("0905123456")
                    .serviceMethod("AT_STORE")
                    .branchName(dn.getName())
                    .deviceModel("Lenovo ThinkPad T14 Gen 2")
                    .serviceType("Cài đặt Windows & Phần mềm (100.000đ)")
                    .issueDescription("Cài mới Windows 11 Pro bản quyền và bộ ứng dụng văn phòng.")
                    .appointmentDate(now.minusDays(1).toLocalDate().toString())
                    .appointmentTime("10:00 - 11:30")
                    .status("COMPLETED")
                    .adminNote("Đã cài đặt hoàn tất, kích hoạt bản quyền và bàn giao máy cho khách.")
                    .build());
        }
    }

    private Useraddresses ensureAddress(Account account, String username, String phone, String address, boolean isDefault) {
        List<Useraddresses> existing = useraddressRepository.findByAccountId(account.getId());
        for (Useraddresses ua : existing) {
            if (address.equalsIgnoreCase(ua.getAddress())) {
                return ua;
            }
        }
        Useraddresses ua = new Useraddresses();
        ua.setAccount(account);
        ua.setUsername(username);
        ua.setPhone(phone);
        ua.setAddress(address);
        ua.setDefaultaddress(isDefault && existing.isEmpty());
        return useraddressRepository.save(ua);
    }

    private Orders createSampleOrder(
            Useraddresses addr, Branch branch, String fulfillmentType, String status,
            LocalDateTime createdAt, List<Products> products, List<Integer> quantities
    ) {
        Orders order = new Orders();
        order.setUseraddresses(addr);
        order.setFulfillmentType(fulfillmentType);
        order.setPickupBranch("STORE_PICKUP".equals(fulfillmentType) ? branch : null);
        order.setReceiverName(addr.getUsername());
        order.setReceiverPhone(addr.getPhone());
        order.setStatus(status);
        order.setCreatedAt(createdAt);
        if ("COMPLETED".equals(status)) {
            order.setPaidAt(createdAt);
        }

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        for (int i = 0; i < products.size(); i++) {
            Products p = products.get(i);
            int q = quantities.get(i);
            total = total.add(p.getPrice().multiply(BigDecimal.valueOf(q)));

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProducts(p);
            item.setBranch(branch);
            item.setQuantity(q);
            item.setPrice(p.getPrice());
            items.add(item);
        }
        order.setTotalAmount(total);
        order.setOrderItems(items);
        return ordersRepository.save(order);
    }

    private void saveSamplePayment(
            Orders order, String txId, String gateway, BigDecimal amount,
            String status, String code, LocalDateTime createdAt
    ) {
        paymentRepository.save(Payment.builder()
                .order(order)
                .transactionId(txId)
                .paymentGateway(gateway)
                .amount(amount)
                .status(status)
                .responseCode(code)
                .createdAt(createdAt)
                .build());
    }

    private Categories ensureCategory(String name, String slug, String groupType) {
        return categoriesRepository.findBySlugIgnoreCase(slug)
                .or(() -> categoriesRepository.findByNameCategoriesIgnoreCase(name))
                .map(c -> {
                    if (!groupType.equalsIgnoreCase(c.getGroupType())) {
                        c.setGroupType(groupType);
                        return categoriesRepository.save(c);
                    }
                    return c;
                })
                .orElseGet(() -> {
                    Categories c = new Categories();
                    c.setNameCategories(name);
                    c.setSlug(slug);
                    c.setGroupType(groupType);
                    return categoriesRepository.save(c);
                });
    }

    private Brands ensureBrand(String name, String slug) {
        return brandsRepository.findByNameBrandsIgnoreCase(name)
                .orElseGet(() -> {
                    Brands b = new Brands();
                    b.setNameBrands(name);
                    b.setSlug(slug);
                    b.setLogo("");
                    return brandsRepository.save(b);
                });
    }

    private Products ensureProduct(
            Brands brand, Categories category, String name, String slug,
            BigDecimal price, String specs, String image, String description
    ) {
        return productRepository.findBySlug(slug)
                .or(() -> productRepository.findByNameProduct(name))
                .orElseGet(() -> {
                    Products p = new Products();
                    p.setBrands(brand);
                    p.setCategories(category);
                    p.setNameProduct(name);
                    p.setSlug(slug);
                    p.setPrice(price);
                    p.setSpecs(specs);
                    p.setImage(image);
                    p.setDescription(description);
                    return productRepository.save(p);
                });
    }
}
