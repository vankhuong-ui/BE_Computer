package com.example.MyProject;

import com.example.MyProject.AUTH.entity.Account;
import com.example.MyProject.AUTH.entity.Useraddresses;
import com.example.MyProject.AUTH.repository.AccountRepository;
import com.example.MyProject.AUTH.repository.UseraddressRepository;
import com.example.MyProject.ORDER.entity.OrderItem;
import com.example.MyProject.ORDER.entity.Orders;
import com.example.MyProject.ORDER.entity.Payment;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@EnableScheduling
@EnableAsync
@SpringBootApplication
public class MyProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyProjectApplication.class, args);
        log.info("HK COMPUTER Backend started successfully");
    }

    @Bean
    CommandLineRunner seedDefaultData(
            AccountRepository accountRepository,
            UseraddressRepository useraddressRepository,
            PasswordEncoder passwordEncoder,
            BranchRepository branchRepository,
            BrandsRepository brandsRepository,
            CategoriesRepository categoriesRepository,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository,
            OrdersRepository ordersRepository,
            PaymentRepository paymentRepository,
            ReviewRepository reviewRepository,
            WarrantyClaimRepository warrantyClaimRepository,
            RepairBookingRepository repairBookingRepository
    ) {
        return args -> {
            seedAccountIfAbsent(
                    accountRepository,
                    passwordEncoder,
                    "admin@hkcomputer.vn",
                    "Quản trị viên HK",
                    "Admin@123",
                    "ADMIN"
            );
            Account sampleCustomer = seedAccountIfAbsent(
                    accountRepository,
                    passwordEncoder,
                    "khachhang@hkcomputer.vn",
                    "Khách hàng mẫu",
                    "User@123",
                    "USER"
            );
            seedAccountIfAbsent(
                    accountRepository,
                    passwordEncoder,
                    "khach@hkcomputer.vn",
                    "Nguyễn Minh Hoàng",
                    "Khach@123",
                    "USER"
            );

            seedCatalogAndMultiBranchInventory(
                    branchRepository,
                    brandsRepository,
                    categoriesRepository,
                    productRepository,
                    inventoryRepository
            );

            seedSampleOperationalDataIfEmpty(
                    sampleCustomer,
                    useraddressRepository,
                    branchRepository,
                    productRepository,
                    ordersRepository,
                    paymentRepository,
                    reviewRepository,
                    warrantyClaimRepository,
                    repairBookingRepository
            );
        };
    }

    private Account seedAccountIfAbsent(
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            String gmail,
            String accountName,
            String rawPassword,
            String role
    ) {
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

    private void seedCatalogAndMultiBranchInventory(
            BranchRepository branchRepository,
            BrandsRepository brandsRepository,
            CategoriesRepository categoriesRepository,
            ProductRepository productRepository,
            InventoryRepository inventoryRepository
    ) {
        List<Branch> branches = branchRepository.findAllByOrderByIdAsc();
        if (branches.isEmpty()) {
            return;
        }

        // 1. Categories across LAPTOP, PC, and ACCESSORY
        Categories catLaptopOffice = ensureCategory(categoriesRepository, "Laptop Văn phòng & Doanh nhân", "laptop-van-phong-doanh-nhan", "LAPTOP");
        Categories catLaptopGaming = ensureCategory(categoriesRepository, "Laptop Gaming & Đồ họa", "laptop-gaming-do-hoa", "LAPTOP");
        Categories catPcDesktop = ensureCategory(categoriesRepository, "PC Gaming & Workstation", "pc-gaming-workstation", "PC");
        Categories catMonitor = ensureCategory(categoriesRepository, "Màn hình máy tính", "man-hinh-may-tinh", "PC");
        Categories catMouse = ensureCategory(categoriesRepository, "Chuột máy tính", "chuot-may-tinh", "ACCESSORY");
        Categories catKeyboard = ensureCategory(categoriesRepository, "Bàn phím cơ", "ban-phim-co", "ACCESSORY");
        Categories catHeadset = ensureCategory(categoriesRepository, "Tai nghe Gaming", "tai-nghe-gaming", "ACCESSORY");
        Categories catBag = ensureCategory(categoriesRepository, "Balo & Túi chống sốc", "balo-tui-chong-soc", "ACCESSORY");
        Categories catRamSsd = ensureCategory(categoriesRepository, "Linh kiện RAM & SSD", "linh-kien-ram-ssd", "ACCESSORY");
        Categories catHub = ensureCategory(categoriesRepository, "Hub & Cáp sạc", "hub-cap-sac", "ACCESSORY");

        // 2. Brands
        Brands brDell = ensureBrand(brandsRepository, "Dell", "dell");
        Brands brAsus = ensureBrand(brandsRepository, "Asus", "asus");
        Brands brLenovo = ensureBrand(brandsRepository, "Lenovo", "lenovo");
        Brands brHp = ensureBrand(brandsRepository, "HP", "hp");
        Brands brApple = ensureBrand(brandsRepository, "Apple", "apple");
        Brands brAcer = ensureBrand(brandsRepository, "Acer", "acer");
        Brands brLg = ensureBrand(brandsRepository, "LG", "lg");
        Brands brLogitech = ensureBrand(brandsRepository, "Logitech", "logitech");
        Brands brKeychron = ensureBrand(brandsRepository, "Keychron", "keychron");
        Brands brHyperX = ensureBrand(brandsRepository, "HyperX", "hyperx");
        Brands brKingston = ensureBrand(brandsRepository, "Kingston", "kingston");
        Brands brSamsung = ensureBrand(brandsRepository, "Samsung", "samsung");
        Brands brAnker = ensureBrand(brandsRepository, "Anker", "anker");

        // 3A. Featured Promo Banner Laptops (exact match for Storefront Dual Banners)
        ensureProduct(
                productRepository, brDell, catLaptopOffice,
                "Laptop Dell Latitude 7490 Core i7 — Bản Doanh Nhân Siêu Bền",
                "laptop-dell-latitude-7490",
                new BigDecimal("8990000"),
                "CPU: Intel Core i7-8650U (4 nhân 8 luồng) | RAM: 16GB DDR4 | Ổ cứng: 512GB SSD NVMe | VGA: Intel UHD Graphics 620 | Màn hình: 14 inch FHD IPS chống chói | Pin: 60Wh sạc nhanh | Khung hợp kim Magie 1.4kg · Bảo hành điện tử 24 tháng",
                "https://hethong.24hlaptop.com/image_event/laptop-dell-latitude-7490-1741774237237.jpg",
                "Mẫu Laptop doanh nhân cao cấp nổi bật trên Banner ưu đãi của HK COMPUTER: khung hợp kim Magie carbon siêu bền đạt chuẩn quân đội MIL-STD-810G, bàn phím gõ cực êm, sẵn sàng cho công việc văn phòng, lập trình và học tập dài hạn."
        );
        ensureProduct(
                productRepository, brDell, catLaptopOffice,
                "Laptop Dell Latitude 5500 Core i5 — Màn 15.6 inch Kèm Phím Số",
                "laptop-dell-latitude-5500",
                new BigDecimal("8490000"),
                "CPU: Intel Core i5-8365U | RAM: 16GB DDR4 | Ổ cứng: 512GB SSD NVMe | VGA: Intel UHD Graphics 620 | Màn hình: 15.6 inch FHD IPS rộng rãi | Pin: 68Wh bền bỉ | Bàn phím Full-size có cụm phím số kế toán",
                "https://hethong.24hlaptop.com/image_event/laptop-dell-latitude-5500-1741774260012.jpg",
                "Mẫu Laptop bán chạy trên Banner HK COMPUTER dành cho sinh viên kinh tế, kế toán và nhân sự văn phòng: màn hình lớn 15.6 inch sắc nét kèm bàn phím số riêng biệt, tản nhiệt mát và đầy đủ cổng HDMI, Type-C, LAN RJ45."
        );

        // 3B. Additional Laptops
        ensureProduct(
                productRepository, brAsus, catLaptopOffice,
                "Laptop Asus Vivobook 15 OLED A1505VA i5-13500H",
                "laptop-asus-vivobook-15-oled-a1505va",
                new BigDecimal("16490000"),
                "CPU: Intel Core i5-13500H | RAM: 16GB DDR4 | Ổ cứng: 512GB SSD PCIe 4.0 | VGA: Intel Iris Xe | Màn hình: 15.6 inch FHD OLED 100% DCI-P3 | Pin: 50Wh sạc nhanh 65W | Bảo mật vân tay · Mở phẳng 180°",
                "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=700&q=80",
                "Laptop văn phòng & sáng tạo nội dung màn hình OLED rực rỡ chuẩn điện ảnh, vi xử lý dòng H hiệu năng mạnh mẽ trong thân máy mỏng nhẹ 1.7kg."
        );
        ensureProduct(
                productRepository, brAsus, catLaptopGaming,
                "Laptop Gaming Asus ROG Strix G16 G614JU i7-13650HX RTX 4060",
                "laptop-gaming-asus-rog-strix-g16-rtx-4060",
                new BigDecimal("31990000"),
                "CPU: Intel Core i7-13650HX | RAM: 16GB DDR5 4800MHz | Ổ cứng: 1TB SSD Gen4 | VGA: NVIDIA GeForce RTX 4060 8GB (140W) | Màn hình: 16 inch FHD+ 165Hz 100% sRGB | Pin: 90Wh | Tản nhiệt 3 quạt keo kim loại lỏng",
                "https://images.unsplash.com/photo-1603302576837-37561b2e2302?w=700&q=80",
                "Chiến binh eSports & dựng phim 3D hàng đầu phân khúc với GPU RTX 4060 full TGP 140W, hệ thống tản nhiệt Tri-Fan mát lạnh và dải LED RGB Aura Sync."
        );
        ensureProduct(
                productRepository, brLenovo, catLaptopGaming,
                "Laptop Gaming Lenovo LOQ 15IAX9 i5-12450HX RTX 3050 6GB",
                "laptop-gaming-lenovo-loq-15iax9-rtx-3050",
                new BigDecimal("19490000"),
                "CPU: Intel Core i5-12450HX | RAM: 16GB DDR5 | Ổ cứng: 512GB SSD NVMe | VGA: RTX 3050 6GB GDDR6 | Màn hình: 15.6 inch FHD IPS 144Hz 100% sRGB | Pin: 60Wh | MUX Switch tối ưu FPS",
                "https://images.unsplash.com/photo-1593642632823-8f785ba67e45?w=700&q=80",
                "Mẫu Laptop Gaming quốc dân chuẩn màu 100% sRGB vừa chiến mượt các tựa game AAA, vừa làm thiết kế đồ họa 2D/Premiere chuẩn xác."
        );
        ensureProduct(
                productRepository, brApple, catLaptopOffice,
                "Apple MacBook Air 13 inch M3 16GB 256GB Chính hãng VN/A",
                "apple-macbook-air-13-m3-16gb-256gb",
                new BigDecimal("26990000"),
                "CPU: Apple M3 8 nhân | RAM: 16GB Unified Memory | Ổ cứng: 256GB SSD | VGA: GPU 8 nhân Neural Engine | Màn hình: 13.6 inch Liquid Retina True Tone | Pin: Lên tới 18 giờ | Vỏ nhôm nguyên khối 1.24kg",
                "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=700&q=80",
                "Phiên bản RAM 16GB tiêu chuẩn mới của Apple giúp đa nhiệm mượt mà hàng chục tab và ứng dụng sáng tạo, thời lượng pin trọn ngày không cần mang sạc."
        );
        ensureProduct(
                productRepository, brHp, catLaptopOffice,
                "Laptop HP Pavilion 15 eg3093TU i5-1335U 16GB 512GB",
                "laptop-hp-pavilion-15-eg3093tu",
                new BigDecimal("15290000"),
                "CPU: Intel Core i5-1335U | RAM: 16GB DDR4 | Ổ cứng: 512GB SSD | VGA: Intel Iris Xe | Màn hình: 15.6 inch FHD IPS BrightView | Pin: 41Wh sạc nhanh | Âm thanh B&O Đan Mạch",
                "https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=700&q=80",
                "Thiết kế vỏ kim loại thanh lịch tông bạc sáng, loa tinh chỉnh bởi Bang & Olufsen sống động, tối ưu cho công việc văn phòng hiện đại."
        );
        ensureProduct(
                productRepository, brAcer, catLaptopGaming,
                "Laptop Gaming Acer Nitro V ANV15-51 i5-13420H RTX 4050",
                "laptop-gaming-acer-nitro-v-15-rtx-4050",
                new BigDecimal("21990000"),
                "CPU: Intel Core i5-13420H | RAM: 16GB DDR5 | Ổ cứng: 512GB SSD Gen4 | VGA: RTX 4050 6GB DLSS 3 | Màn hình: 15.6 inch FHD IPS 144Hz | Pin: 57Wh | Cổng Thunderbolt 4 tốc độ cao",
                "https://images.unsplash.com/photo-1611078489935-0cb964de46d6?w=700&q=80",
                "Trang bị DLSS 3 Frame Generation thế hệ mới giúp tăng vọt khung hình trong các tựa game nặng với mức giá dễ tiếp cận."
        );

        // 3C. PC & Monitors
        ensureProduct(
                productRepository, brAsus, catPcDesktop,
                "PC Gaming HK Titan i5-13400F / RAM 16GB / RTX 4060 8GB",
                "pc-gaming-hk-titan-i5-13400f-rtx-4060",
                new BigDecimal("18990000"),
                "CPU: Intel Core i5-13400F (10 nhân 16 luồng) | RAM: 16GB DDR4 3600MHz RGB | Ổ cứng: 512GB NVMe Gen4 | VGA: Asus Dual RTX 4060 8GB | Main: B760M Wifi | Nguồn: 650W 80 Plus Bronze | Vỏ kính cường lực 4 Fan ARGB",
                "https://images.unsplash.com/photo-1587202372775-e229f172b9d7?w=700&q=80",
                "Bộ PC lắp sẵn tối ưu hiệu năng/giá thành tại HK COMPUTER, đi dây gọn gàng, test stress 24 giờ trước khi bàn giao và bảo hành linh kiện lên tới 36 tháng."
        );
        ensureProduct(
                productRepository, brDell, catPcDesktop,
                "PC Đồng Bộ Văn Phòng Dell OptiPlex 7010 SFF Core i5-13500",
                "pc-dong-bo-dell-optiplex-7010-sff-i5",
                new BigDecimal("13490000"),
                "CPU: Intel Core i5-13500 | RAM: 16GB DDR4 | Ổ cứng: 512GB SSD NVMe | VGA: Intel UHD 770 xuất 3 màn hình | Kết nối: Wi-Fi 6 + Bluetooth + LAN Gigabit | Kiểu dáng Small Form Factor siêu gọn",
                "https://images.unsplash.com/photo-1593640408182-31c70c8268f5?w=700&q=80",
                "Máy bộ doanh nghiệp vận hành êm ái 24/7, tiết kiệm diện tích bàn làm việc và độ ổn định cao cho văn phòng công ty, quầy thu ngân."
        );
        ensureProduct(
                productRepository, brLg, catMonitor,
                "Màn Hình Gaming LG UltraGear 27GS60F-B 27 inch FHD IPS 180Hz 1ms",
                "man-hinh-lg-ultragear-27gs60f-27-180hz",
                new BigDecimal("3890000"),
                "Kích thước: 27 inch phẳng | Tấm nền: Fast IPS 99% sRGB | Tần số quét: 180Hz · 1ms GtG | Công nghệ: HDR10, NVIDIA G-Sync Compatible & FreeSync | Cổng: HDMI 2.0 + DisplayPort 1.4",
                "https://images.unsplash.com/photo-1527443224154-c4a3942d3acf?w=700&q=80",
                "Màn hình quốc dân 180Hz siêu mượt kết hợp chuẩn màu 99% sRGB rực rỡ cho cả game thủ FPS lẫn nhu cầu làm việc đa màn hình."
        );
        ensureProduct(
                productRepository, brDell, catMonitor,
                "Màn Hình Đồ Họa Dell UltraSharp U2424H 24 inch FHD IPS 120Hz USB-C",
                "man-hinh-dell-ultrasharp-u2424h-24-120hz",
                new BigDecimal("6190000"),
                "Kích thước: 24 inch viền siêu mỏng InfinityEdge | Tấm nền: IPS 100% sRGB, 100% BT.709, 85% DCI-P3 (Delta E < 2) | Tần số quét: 120Hz | Chân đế công thái học xoay dọc 90° | Hub USB-C & RJ45",
                "https://images.unsplash.com/photo-1547119957-637f8679db1e?w=700&q=80",
                "Dòng UltraSharp danh tiếng cân màu sẵn từ nhà máy kèm cảm biến ánh sáng bảo vệ mắt ComfortView Plus thế hệ mới."
        );

        // 3D. Accessories
        ensureProduct(
                productRepository, brLogitech, catMouse,
                "Chuột Không Dây Gaming Logitech G304 Lightspeed",
                "chuot-khong-day-logitech-g304-lightspeed",
                new BigDecimal("790000"),
                "Không dây Lightspeed | Cảm biến HERO 12.000 DPI | Pin 250 giờ",
                "https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=600&q=80",
                "Chuột gaming không dây Logitech G304 Lightspeed siêu nhạy, trọng lượng nhẹ 99g, độ trễ 1ms phù hợp làm việc và chơi game."
        );
        ensureProduct(
                productRepository, brKeychron, catKeyboard,
                "Bàn Phím Cơ Không Dây Keychron K8 Pro QMK/VIA RGB",
                "ban-phim-co-keychron-k8-pro-rgb",
                new BigDecimal("2190000"),
                "Bluetooth 5.1 & Type-C | Hot-swap Gateron Pro | Khung nhôm LED RGB",
                "https://images.unsplash.com/photo-1587829741301-dc798b83add3?w=600&q=80",
                "Bàn phím cơ TKL không dây hỗ trợ Mac & Windows, lót sẵn foam tiêu âm, keycap PBT Double-shot bền bỉ."
        );
        ensureProduct(
                productRepository, brHyperX, catHeadset,
                "Tai Nghe Chụp Tai Gaming HyperX Cloud III 7.1",
                "tai-nghe-gaming-hyperx-cloud-iii",
                new BigDecimal("1890000"),
                "Âm thanh vòm DTS 7.1 | Màng loa 53mm | Micro khử ồn tháo rời",
                "https://images.unsplash.com/photo-1546435770-a3e426bf472b?w=600&q=80",
                "Tai nghe gaming cao cấp đệm mút hoạt tính êm ái, khung hợp kim nhôm siêu bền kèm kết nối USB-C, USB-A và 3.5mm."
        );
        ensureProduct(
                productRepository, brAnker, catBag,
                "Balo Laptop Chống Sốc Kháng Nước HK Minimal Pro 15.6 inch",
                "balo-laptop-chong-soc-hk-minimal-pro-15-6",
                new BigDecimal("550000"),
                "Đựng vừa 14 - 16 inch | Đệm chống sốc 360° | Vải Oxford kháng nước",
                "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=600&q=80",
                "Balo chống sốc chuyên dụng cho Laptop 14 - 16 inch, ngăn đệm nhung dày bảo vệ góc máy và cổng sạc ẩn tiện lợi."
        );
        ensureProduct(
                productRepository, brKingston, catRamSsd,
                "RAM Laptop Kingston Fury Impact 16GB DDR5 5600MHz",
                "ram-laptop-kingston-fury-impact-16gb-ddr5-5600",
                new BigDecimal("1450000"),
                "16GB DDR5 | Bus 5600MHz | CL40 SODIMM · Miễn phí lắp đặt",
                "https://images.unsplash.com/photo-1562976540-1502c2145186?w=600&q=80",
                "Nâng cấp đa nhiệm mượt mà cho Laptop Gaming & Đồ họa đời mới, miễn phí công lắp đặt lấy liền tại 10 chi nhánh HK COMPUTER."
        );
        ensureProduct(
                productRepository, brSamsung, catRamSsd,
                "Ổ Cứng SSD Samsung 990 PRO 1TB M.2 NVMe PCIe Gen4",
                "ssd-samsung-990-pro-1tb-nvme-gen4",
                new BigDecimal("2690000"),
                "1TB M.2 NVMe | Đọc 7450 MB/s | Bảo hành 60 tháng",
                "https://images.unsplash.com/photo-1597872200969-2b65d56bd16b?w=600&q=80",
                "Ổ cứng SSD NVMe tốc độ cao hàng đầu cho Laptop & PC, hỗ trợ clone Windows miễn phí khi nâng cấp tại cửa hàng."
        );
        ensureProduct(
                productRepository, brAnker, catHub,
                "Hub Chuyển Đổi Type-C Anker PowerExpand 7-in-1 HDMI 4K PD 100W",
                "hub-type-c-anker-powerexpand-7-in-1",
                new BigDecimal("990000"),
                "HDMI 4K@60Hz | Sạc PD 100W | 2×USB 3.0 · SD/TF · Type-C",
                "https://images.unsplash.com/photo-1625842268584-8f3296236761?w=600&q=80",
                "Bộ mở rộng cổng kết nối đa năng chuẩn Type-C tương thích MacBook, Asus Zenbook, Dell XPS, HP Envy."
        );

        // 4. Distribute multi-branch inventory (including realistic low-stock items for Admin BI alerts)
        List<Products> allProducts = productRepository.findAll();
        for (Products p : allProducts) {
            List<Inventory> existingInvs = inventoryRepository.findByProducts_Id(p.getId());
            if (existingInvs.size() <= 1) {
                int seedIndex = (int) (p.getId() % 5);
                for (int bIdx = 0; bIdx < branches.size(); bIdx++) {
                    Branch branch = branches.get(bIdx);
                    boolean alreadyHas = existingInvs.stream()
                            .anyMatch(inv -> inv.getBranch() != null && branch.getId().equals(inv.getBranch().getId()));
                    if (alreadyHas) continue;

                    int qty = ((bIdx + seedIndex) % 5 == 4) ? 0
                            : (((bIdx + seedIndex) % 5 == 3) ? 2 : (4 + ((bIdx * 3 + seedIndex * 2) % 8)));
                    Inventory inv = new Inventory();
                    inv.setProducts(p);
                    inv.setBranch(branch);
                    inv.setSoluong(qty);
                    inv.setSelled(1 + ((bIdx + seedIndex) % 6));
                    inventoryRepository.save(inv);
                }
            }
        }
    }

    private void seedSampleOperationalDataIfEmpty(
            Account customer,
            UseraddressRepository useraddressRepository,
            BranchRepository branchRepository,
            ProductRepository productRepository,
            OrdersRepository ordersRepository,
            PaymentRepository paymentRepository,
            ReviewRepository reviewRepository,
            WarrantyClaimRepository warrantyClaimRepository,
            RepairBookingRepository repairBookingRepository
    ) {
        if (customer == null || ordersRepository.count() > 0) {
            return;
        }

        List<Branch> branches = branchRepository.findAllByOrderByIdAsc();
        if (branches.isEmpty()) return;
        Branch mainBranch = branches.get(0);
        Branch hcmBranch = branches.size() > 3 ? branches.get(3) : mainBranch;

        List<Useraddresses> addrs = useraddressRepository.findByAccountId(customer.getId());
        Useraddresses addr;
        if (addrs.isEmpty()) {
            Useraddresses created = new Useraddresses();
            created.setAccount(customer);
            created.setUsername("Khách hàng mẫu");
            created.setPhone("0912345678");
            created.setAddress("Số 18 Hoàng Quốc Việt, Quận Cầu Giấy, Hà Nội");
            created.setDefaultaddress(true);
            addr = useraddressRepository.save(created);
        } else {
            addr = addrs.get(0);
        }

        Products dell7490 = productRepository.findBySlug("laptop-dell-latitude-7490").orElse(null);
        Products dell5500 = productRepository.findBySlug("laptop-dell-latitude-5500").orElse(null);
        Products mouse = productRepository.findBySlug("chuot-khong-day-logitech-g304-lightspeed").orElse(null);
        Products pcTitan = productRepository.findBySlug("pc-gaming-hk-titan-i5-13400f-rtx-4060").orElse(null);

        LocalDateTime now = LocalDateTime.now();

        if (dell7490 != null && mouse != null) {
            Orders o1 = createSampleOrder(addr, mainBranch, "HOME_DELIVERY", "COMPLETED",
                    now.minusDays(4), List.of(dell7490, mouse), List.of(1, 1), ordersRepository);
            saveSamplePayment(o1, "SEPAY_DEMO_1001", "SEPAY", o1.getTotalAmount(), "SUCCESS", "00", now.minusDays(4), paymentRepository);

            reviewRepository.save(Review.builder()
                    .order(o1).product(dell7490).rating(5)
                    .comment("Máy Dell Latitude 7490 ngoại hình đẹp keng, phím gõ cực êm, pin dùng văn phòng hơn 5 tiếng. Shop giao nhanh và tự kích hoạt bảo hành điện tử luôn!")
                    .build());
            reviewRepository.save(Review.builder()
                    .order(o1).product(mouse).rating(5)
                    .comment("Chuột G304 nhạy, cầm vừa tay, đóng gói chống sốc cẩn thận.")
                    .build());

            if (warrantyClaimRepository.count() == 0) {
                warrantyClaimRepository.save(WarrantyClaim.builder()
                        .order(o1)
                        .product(mouse)
                        .account(customer)
                        .unitNumber(1)
                        .serialCode("BH-" + o1.getId() + "-" + mouse.getId() + "-1")
                        .issueType("Kiểm tra đầu thu Receiver USB")
                        .description("Nhờ kỹ thuật kiểm tra kết nối đầu thu Lightspeed khi cắm qua cổng USB 3.0.")
                        .contactPhone("0912345678")
                        .status("PENDING")
                        .warrantyMonths(12)
                        .warrantyExpireDate(o1.getPaidAt().plusMonths(12))
                        .build());
            }
        }

        if (dell5500 != null) {
            Orders o2 = createSampleOrder(addr, hcmBranch, "STORE_PICKUP", "COMPLETED",
                    now.minusDays(2), List.of(dell5500), List.of(1), ordersRepository);
            saveSamplePayment(o2, "VNP_DEMO_2002", "VNPAY", o2.getTotalAmount(), "SUCCESS", "00", now.minusDays(2), paymentRepository);

            reviewRepository.save(Review.builder()
                    .order(o2).product(dell5500).rating(5)
                    .comment("Màn 15.6 inch rộng rãi, có bàn phím số nhập liệu Excel rất tiện, nhận máy tại chi nhánh Quận 10 được tặng kèm vệ sinh miễn phí.")
                    .build());
        }

        if (pcTitan != null) {
            Orders o3 = createSampleOrder(addr, mainBranch, "HOME_DELIVERY", "COMPLETED",
                    now.minusDays(1), List.of(pcTitan), List.of(1), ordersRepository);
            saveSamplePayment(o3, "SEPAY_DEMO_1003", "SEPAY", o3.getTotalAmount(), "SUCCESS", "00", now.minusDays(1), paymentRepository);
        }

        if (dell7490 != null) {
            createSampleOrder(addr, mainBranch, "HOME_DELIVERY", "UNPAID",
                    now.minusMinutes(10), List.of(dell7490), List.of(1), ordersRepository);
        }

        if (repairBookingRepository.count() == 0) {
            repairBookingRepository.save(RepairBooking.builder()
                    .account(customer)
                    .customerName("Nguyễn Minh Hoàng")
                    .customerPhone("0987654321")
                    .customerEmail(customer.getGmail())
                    .serviceMethod("AT_STORE")
                    .branchName(mainBranch.getName())
                    .deviceModel("Dell XPS 13 9310")
                    .serviceType("Vệ sinh chuyên sâu & Tra keo tản nhiệt")
                    .issueDescription("Máy chạy đồ họa hơi nóng khu vực bản lề, cần vệ sinh lấy ngay trong ngày.")
                    .appointmentDate(now.plusDays(1).toLocalDate().toString())
                    .appointmentTime("09:30 - 11:00")
                    .status("PENDING")
                    .build());

            repairBookingRepository.save(RepairBooking.builder()
                    .customerName("Trần Thu Hà")
                    .customerPhone("0909123456")
                    .serviceMethod("AT_HOME")
                    .branchName("Sửa tại nhà — Tòa Keangnam Landmark 72, Nam Từ Liêm, Hà Nội")
                    .homeAddress("Tòa Keangnam Landmark 72, Nam Từ Liêm, Hà Nội")
                    .deviceModel("PC Workstation Đồ họa Core i7")
                    .serviceType("Nâng cấp RAM / SSD & Cài đặt phần mềm")
                    .issueDescription("Nhờ kỹ thuật qua văn phòng nâng cấp thêm 1 ổ SSD 1TB và cài bộ phần mềm thiết kế.")
                    .appointmentDate(now.plusDays(2).toLocalDate().toString())
                    .appointmentTime("14:00 - 16:00")
                    .status("CONFIRMED")
                    .adminNote("Đã phân công KTV Minh liên hệ trước 30 phút.")
                    .build());
        }
    }

    private Orders createSampleOrder(
            Useraddresses addr,
            Branch branch,
            String fulfillmentType,
            String status,
            LocalDateTime createdAt,
            List<Products> products,
            List<Integer> quantities,
            OrdersRepository ordersRepository
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
            Orders order,
            String txId,
            String gateway,
            BigDecimal amount,
            String status,
            String code,
            LocalDateTime createdAt,
            PaymentRepository paymentRepository
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

    private Categories ensureCategory(CategoriesRepository repo, String name, String slug, String groupType) {
        return repo.findBySlugIgnoreCase(slug)
                .or(() -> repo.findByNameCategoriesIgnoreCase(name))
                .map(c -> {
                    if (!groupType.equalsIgnoreCase(c.getGroupType())) {
                        c.setGroupType(groupType);
                        return repo.save(c);
                    }
                    return c;
                })
                .orElseGet(() -> {
                    Categories c = new Categories();
                    c.setNameCategories(name);
                    c.setSlug(slug);
                    c.setGroupType(groupType);
                    return repo.save(c);
                });
    }

    private Brands ensureBrand(BrandsRepository repo, String name, String slug) {
        return repo.findByNameBrandsIgnoreCase(name)
                .orElseGet(() -> {
                    Brands b = new Brands();
                    b.setNameBrands(name);
                    b.setSlug(slug);
                    b.setLogo("");
                    return repo.save(b);
                });
    }

    private void ensureProduct(
            ProductRepository repo,
            Brands brand,
            Categories category,
            String name,
            String slug,
            BigDecimal price,
            String specs,
            String image,
            String description
    ) {
        if (repo.findBySlug(slug).isPresent() || repo.findByNameProduct(name).isPresent()) {
            return;
        }
        Products p = new Products();
        p.setBrands(brand);
        p.setCategories(category);
        p.setNameProduct(name);
        p.setSlug(slug);
        p.setPrice(price);
        p.setSpecs(specs);
        p.setImage(image);
        p.setDescription(description);
        repo.save(p);
    }
}
