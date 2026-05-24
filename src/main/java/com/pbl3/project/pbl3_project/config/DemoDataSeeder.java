package com.pbl3.project.pbl3_project.config;

import com.pbl3.project.pbl3_project.dto.CreateImportOrderRequest;
import com.pbl3.project.pbl3_project.dto.CreateOrderRequest;
import com.pbl3.project.pbl3_project.entity.AccountAuditAction;
import com.pbl3.project.pbl3_project.entity.AccountAuditLog;
import com.pbl3.project.pbl3_project.entity.Brand;
import com.pbl3.project.pbl3_project.entity.Category;
import com.pbl3.project.pbl3_project.entity.Customer;
import com.pbl3.project.pbl3_project.entity.ImportOrder;
import com.pbl3.project.pbl3_project.entity.ImportOrderStatus;
import com.pbl3.project.pbl3_project.entity.InventoryPositionBaseline;
import com.pbl3.project.pbl3_project.entity.InventoryTransaction;
import com.pbl3.project.pbl3_project.entity.InventoryTransactionType;
import com.pbl3.project.pbl3_project.entity.OperationalAuditAction;
import com.pbl3.project.pbl3_project.entity.OperationalAuditLog;
import com.pbl3.project.pbl3_project.entity.OperationalSubjectType;
import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.entity.Origin;
import com.pbl3.project.pbl3_project.entity.PaymentMethod;
import com.pbl3.project.pbl3_project.entity.Product;
import com.pbl3.project.pbl3_project.entity.Role;
import com.pbl3.project.pbl3_project.entity.Supplier;
import com.pbl3.project.pbl3_project.entity.Unit;
import com.pbl3.project.pbl3_project.entity.User;
import com.pbl3.project.pbl3_project.repository.AccountAuditLogRepository;
import com.pbl3.project.pbl3_project.repository.BrandRepository;
import com.pbl3.project.pbl3_project.repository.CategoryRepository;
import com.pbl3.project.pbl3_project.repository.CustomerRepository;
import com.pbl3.project.pbl3_project.repository.ImportOrderRepository;
import com.pbl3.project.pbl3_project.repository.InventoryPositionBaselineRepository;
import com.pbl3.project.pbl3_project.repository.InventoryTransactionRepository;
import com.pbl3.project.pbl3_project.repository.OperationalAuditLogRepository;
import com.pbl3.project.pbl3_project.repository.OriginRepository;
import com.pbl3.project.pbl3_project.repository.OrderRepository;
import com.pbl3.project.pbl3_project.repository.ProductRepository;
import com.pbl3.project.pbl3_project.repository.SupplierRepository;
import com.pbl3.project.pbl3_project.repository.UnitRepository;
import com.pbl3.project.pbl3_project.repository.UserRepository;
import com.pbl3.project.pbl3_project.service.ImportOrderService;
import com.pbl3.project.pbl3_project.service.MoneySupport;
import com.pbl3.project.pbl3_project.service.OrderService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Configuration
@Profile("demo")
@ConditionalOnProperty(name = "app.demo.seed", havingValue = "true")
public class DemoDataSeeder {
    private static final long DEMO_RANDOM_SEED = 20260415L;
    private static final int HISTORICAL_DAYS = 365;
    private static final int RECENT_SCENARIO_DAYS = 20;

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final CustomerRepository customerRepository;
    private final BrandRepository brandRepository;
    private final OriginRepository originRepository;
    private final UnitRepository unitRepository;
    private final OrderRepository orderRepository;
    private final ImportOrderRepository importOrderRepository;
    private final InventoryPositionBaselineRepository baselineRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final OperationalAuditLogRepository operationalAuditLogRepository;
    private final AccountAuditLogRepository accountAuditLogRepository;
    private final OrderService orderService;
    private final ImportOrderService importOrderService;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(
        UserRepository userRepository,
        CategoryRepository categoryRepository,
        ProductRepository productRepository,
        SupplierRepository supplierRepository,
        CustomerRepository customerRepository,
        BrandRepository brandRepository,
        OriginRepository originRepository,
        UnitRepository unitRepository,
        OrderRepository orderRepository,
        ImportOrderRepository importOrderRepository,
        InventoryPositionBaselineRepository baselineRepository,
        InventoryTransactionRepository inventoryTransactionRepository,
        OperationalAuditLogRepository operationalAuditLogRepository,
        AccountAuditLogRepository accountAuditLogRepository,
        OrderService orderService,
        ImportOrderService importOrderService,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.customerRepository = customerRepository;
        this.brandRepository = brandRepository;
        this.originRepository = originRepository;
        this.unitRepository = unitRepository;
        this.orderRepository = orderRepository;
        this.importOrderRepository = importOrderRepository;
        this.baselineRepository = baselineRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.operationalAuditLogRepository = operationalAuditLogRepository;
        this.accountAuditLogRepository = accountAuditLogRepository;
        this.orderService = orderService;
        this.importOrderService = importOrderService;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    @org.springframework.core.annotation.Order(50)
    CommandLineRunner seedDemoDatabase() {
        return args -> seedDemoDatabaseInternal();
    }

    private void seedDemoDatabaseInternal() {
        if (orderRepository.count() > 0 || importOrderRepository.count() > 0) {
            System.out.println("Demo profile detected existing transactional data. Skipping demo seed.");
            return;
        }

        hideLegacySampleProduct();

        LocalDate anchorDate = LocalDate.now();
        LocalDateTime baselineAt = anchorDate.minusDays(HISTORICAL_DAYS + 21L).atStartOfDay();
        Random random = new Random(DEMO_RANDOM_SEED);

        User admin = ensureUser("admin", "admin", "System Administrator", Role.ADMIN, false);
        User manager = ensureUser("manager", "manager", "Operations Manager", Role.MANAGER, true);
        User staff = ensureUser("staff", "staff", "Sales Associate", Role.STAFF, true);

        seedAccountAudit(admin, manager, AccountAuditAction.CREATE_ACCOUNT, at(anchorDate, 42, 9, 0), "Created demo manager account");
        seedAccountAudit(admin, staff, AccountAuditAction.CREATE_ACCOUNT, at(anchorDate, 41, 9, 20), "Created demo staff account");

        Category beverages = ensureCategory("Beverages");
        Category snacks = ensureCategory("Snacks");
        Category household = ensureCategory("Household");
        Category stationery = ensureCategory("Stationery");
        Category grocery = ensureCategory("Grocery");
        Category officeSupplies = ensureCategory("Office Supplies");

        Unit pieces = ensureUnit("pcs");
        Origin vietnam = ensureOrigin("Vietnam");
        Origin thailand = ensureOrigin("Thailand");
        Origin korea = ensureOrigin("Korea");
        Origin china = ensureOrigin("China");
        Brand aquaPure = ensureBrand("AquaPure");
        Brand greenBarn = ensureBrand("GreenBarn");
        Brand sweetBite = ensureBrand("SweetBite");
        Brand cleanHome = ensureBrand("CleanHome");
        Brand paperTrail = ensureBrand("PaperTrail");
        Brand voltRush = ensureBrand("VoltRush");
        Brand sunnyLeaf = ensureBrand("SunnyLeaf");
        Brand morningPeak = ensureBrand("MorningPeak");
        Brand crunchTime = ensureBrand("CrunchTime");
        Brand riceJoy = ensureBrand("RiceJoy");
        Brand sparkClean = ensureBrand("SparkClean");
        Brand softCloud = ensureBrand("SoftCloud");
        Brand officePlus = ensureBrand("OfficePlus");
        Brand writeFlow = ensureBrand("WriteFlow");
        Brand kitchenMate = ensureBrand("KitchenMate");
        Brand berryFarm = ensureBrand("BerryFarm");
        Brand freshNest = ensureBrand("FreshNest");
        Brand quickBowl = ensureBrand("QuickBowl");

        Supplier urbanDrinks = ensureSupplier("Urban Drinks Co", "0909001001", "12 Nguyen Hue, District 1");
        Supplier freshSource = ensureSupplier("Fresh Source Supply", "0909001002", "77 Le Loi, District 1");
        Supplier homeCare = ensureSupplier("Home Care Wholesale", "0909001003", "9 Bach Dang, Binh Thanh");
        Supplier dailyStaples = ensureSupplier("Daily Staples Distribution", "0909001004", "188 Dien Bien Phu, Binh Thanh");
        Supplier officeHub = ensureSupplier("Office Essentials Hub", "0909001005", "25 Nguyen Thi Minh Khai, District 3");

        Customer lanAnh = ensureCustomer("Tran Lan Anh", "0901000001");
        Customer minhKhoa = ensureCustomer("Nguyen Minh Khoa", "0901000002");
        Customer thuyVan = ensureCustomer("Le Thuy Van", "0901000003");
        Customer quocBao = ensureCustomer("Pham Quoc Bao", "0901000004");
        List<Customer> additionalCustomers = ensureCustomers(
            new CustomerSeed("Vo Duc Thanh", "0901000101"),
            new CustomerSeed("Nguyen Hoang Yen", "0901000102"),
            new CustomerSeed("Pham Gia Han", "0901000103"),
            new CustomerSeed("Bui Thanh Nam", "0901000104"),
            new CustomerSeed("Dang Thu Trang", "0901000105"),
            new CustomerSeed("Tran Minh Chau", "0901000106"),
            new CustomerSeed("Le Quoc Hung", "0901000107"),
            new CustomerSeed("Do Khanh Linh", "0901000108"),
            new CustomerSeed("Hoang Gia Bao", "0901000109"),
            new CustomerSeed("Phan Bao Ngoc", "0901000110"),
            new CustomerSeed("Nguyen Tu Anh", "0901000111"),
            new CustomerSeed("Vu Dinh Phuc", "0901000112"),
            new CustomerSeed("Pham Thanh Truc", "0901000113"),
            new CustomerSeed("Dinh Huy Hoang", "0901000114"),
            new CustomerSeed("Le Ngoc Bich", "0901000115"),
            new CustomerSeed("Truong Bao Minh", "0901000116")
        );
        List<Customer> allCustomers = new ArrayList<>(List.of(lanAnh, minhKhoa, thuyVan, quocBao));
        allCustomers.addAll(additionalCustomers);

        Product sparklingWater = ensureProduct(
            "Sparkling Water 500ml",
            "Fast-moving bottled water for checkout impulse sales",
            decimal("14.00"),
            decimal("7.80"),
            30,
            beverages,
            aquaPure,
            vietnam,
            pieces,
            "BEV-SW-500",
            "8935001000011"
        );
        Product oatMilk = ensureProduct(
            "Oat Milk 1L",
            "Plant-based milk with consistent weekly demand",
            decimal("42.00"),
            decimal("24.70"),
            18,
            beverages,
            greenBarn,
            thailand,
            pieces,
            "BEV-OM-1L",
            "8935001000012"
        );
        Product chocolateBar = ensureProduct(
            "Dark Chocolate Bar 80g",
            "Premium snack item with steady repeat sales",
            decimal("26.00"),
            decimal("12.40"),
            12,
            snacks,
            sweetBite,
            vietnam,
            pieces,
            "SNK-DC-080",
            "8935001000013"
        );
        Product detergent = ensureProduct(
            "Detergent Fresh 1L",
            "Slow-moving household stock used to demonstrate aging inventory",
            decimal("68.00"),
            decimal("39.00"),
            12,
            household,
            cleanHome,
            vietnam,
            pieces,
            "HOU-DF-1L",
            "8935001000014"
        );
        Product notebook = ensureProduct(
            "Notebook A5 Softcover",
            "Stationery line with healthy coverage and moderate demand",
            decimal("32.00"),
            decimal("16.50"),
            15,
            stationery,
            paperTrail,
            vietnam,
            pieces,
            "STA-NB-A5",
            "8935001000015"
        );
        Product energyDrink = ensureProduct(
            "Energy Drink 330ml",
            "Stable beverage item used as a strong-stock example",
            decimal("20.00"),
            decimal("10.80"),
            24,
            beverages,
            voltRush,
            thailand,
            pieces,
            "BEV-ED-330",
            "8935001000016"
        );
        Product bottledTea = ensureProduct(
            "Bottled Tea Lemon 450ml",
            "Fast-moving ready-to-drink tea with stronger summer demand",
            decimal("17.00"),
            decimal("9.20"),
            24,
            beverages,
            sunnyLeaf,
            vietnam,
            pieces,
            "BEV-BTL-450",
            "8935001000017"
        );
        Product coffeeMix = ensureProduct(
            "Instant Coffee Mix 20 Sachets",
            "Staple grocery item with stable weekday demand",
            decimal("56.00"),
            decimal("31.00"),
            14,
            grocery,
            morningPeak,
            vietnam,
            pieces,
            "GRO-COF-020",
            "8935001000018"
        );
        Product potatoChips = ensureProduct(
            "Potato Chips Sea Salt 150g",
            "Snack line with stronger weekend and holiday pull",
            decimal("28.00"),
            decimal("14.50"),
            16,
            snacks,
            crunchTime,
            thailand,
            pieces,
            "SNK-PCS-150",
            "8935001000019"
        );
        Product riceCrackers = ensureProduct(
            "Rice Crackers 200g",
            "Popular rice snack with festive season uplift",
            decimal("24.00"),
            decimal("12.20"),
            14,
            snacks,
            riceJoy,
            vietnam,
            pieces,
            "SNK-RCK-200",
            "8935001000020"
        );
        Product dishwashingLiquid = ensureProduct(
            "Dishwashing Liquid 750ml",
            "Everyday household cleaner with predictable replenishment cycle",
            decimal("45.00"),
            decimal("24.80"),
            10,
            household,
            sparkClean,
            vietnam,
            pieces,
            "HOU-DWL-750",
            "8935001000021"
        );
        Product tissue = ensureProduct(
            "Facial Tissue 180 Sheets",
            "Household essentials line with broad repeat demand",
            decimal("22.00"),
            decimal("11.30"),
            20,
            household,
            softCloud,
            vietnam,
            pieces,
            "HOU-TIS-180",
            "8935001000022"
        );
        Product copyPaper = ensureProduct(
            "A4 Copy Paper 500 Sheets",
            "Office supplies item with seasonal spikes around back-to-school",
            decimal("72.00"),
            decimal("46.00"),
            8,
            officeSupplies,
            officePlus,
            vietnam,
            pieces,
            "OFF-A4-500",
            "8935001000023"
        );
        Product gelPen = ensureProduct(
            "Gel Pen Blue 0.5mm",
            "Low-ticket stationery SKU with basket-add behavior",
            decimal("12.00"),
            decimal("5.40"),
            30,
            officeSupplies,
            writeFlow,
            china,
            pieces,
            "OFF-GPB-05",
            "8935001000024"
        );
        Product soySauce = ensureProduct(
            "Soy Sauce Premium 500ml",
            "Stable grocery condiment with mild festive uplift",
            decimal("33.00"),
            decimal("18.70"),
            12,
            grocery,
            kitchenMate,
            korea,
            pieces,
            "GRO-SOY-500",
            "8935001000025"
        );
        Product yogurtDrink = ensureProduct(
            "Yogurt Drink Strawberry 180ml",
            "Impulse beverage item with stronger heat-season movement",
            decimal("14.00"),
            decimal("7.50"),
            28,
            beverages,
            berryFarm,
            thailand,
            pieces,
            "BEV-YDS-180",
            "8935001000026"
        );
        Product laundrySoftener = ensureProduct(
            "Laundry Softener 800ml",
            "Household line that moves slower but steadily",
            decimal("59.00"),
            decimal("33.00"),
            9,
            household,
            freshNest,
            vietnam,
            pieces,
            "HOU-LSF-800",
            "8935001000027"
        );
        Product instantNoodles = ensureProduct(
            "Instant Noodles Cup Seafood",
            "High-volume convenience SKU with strong festive demand",
            decimal("18.00"),
            decimal("9.60"),
            24,
            grocery,
            quickBowl,
            vietnam,
            pieces,
            "GRO-INC-SEA",
            "8935001000028"
        );

        List<Product> allProducts = List.of(
            sparklingWater,
            oatMilk,
            chocolateBar,
            detergent,
            notebook,
            energyDrink,
            bottledTea,
            coffeeMix,
            potatoChips,
            riceCrackers,
            dishwashingLiquid,
            tissue,
            copyPaper,
            gelPen,
            soySauce,
            yogurtDrink,
            laundrySoftener,
            instantNoodles
        );
        allProducts.forEach(product -> ensureZeroBaseline(product, baselineAt));

        List<HistoricalProductPlan> historicalPlans = List.of(
            new HistoricalProductPlan(bottledTea, urbanDrinks, decimal("9.20"), 120, 48, 132, 24, 14, 5, DemandSeasonality.SUMMER),
            new HistoricalProductPlan(coffeeMix, dailyStaples, decimal("31.00"), 72, 24, 60, 12, 6, 2, DemandSeasonality.FESTIVE),
            new HistoricalProductPlan(potatoChips, freshSource, decimal("14.50"), 96, 30, 90, 18, 10, 4, DemandSeasonality.WEEKEND),
            new HistoricalProductPlan(riceCrackers, freshSource, decimal("12.20"), 84, 26, 78, 18, 7, 3, DemandSeasonality.FESTIVE),
            new HistoricalProductPlan(dishwashingLiquid, homeCare, decimal("24.80"), 44, 16, 42, 10, 5, 2, DemandSeasonality.STABLE),
            new HistoricalProductPlan(tissue, homeCare, decimal("11.30"), 110, 36, 108, 24, 8, 3, DemandSeasonality.STABLE),
            new HistoricalProductPlan(copyPaper, officeHub, decimal("46.00"), 34, 10, 32, 10, 4, 2, DemandSeasonality.SCHOOL),
            new HistoricalProductPlan(gelPen, officeHub, decimal("5.40"), 140, 42, 126, 30, 9, 6, DemandSeasonality.SCHOOL),
            new HistoricalProductPlan(soySauce, dailyStaples, decimal("18.70"), 52, 18, 48, 12, 5, 2, DemandSeasonality.FESTIVE),
            new HistoricalProductPlan(yogurtDrink, freshSource, decimal("7.50"), 132, 54, 144, 24, 12, 5, DemandSeasonality.SUMMER),
            new HistoricalProductPlan(laundrySoftener, homeCare, decimal("33.00"), 38, 12, 36, 12, 4, 2, DemandSeasonality.STABLE),
            new HistoricalProductPlan(instantNoodles, dailyStaples, decimal("9.60"), 110, 42, 108, 24, 11, 4, DemandSeasonality.FESTIVE)
        );

        seedHistoricalYear(anchorDate, admin, manager, staff, allCustomers, historicalPlans, random);

        createCompletedImportAt(
            admin,
            homeCare,
            at(anchorDate, 45, 8, 15),
            "Opening household and stationery stock",
            new ImportLine(detergent, 42, decimal("39.00")),
            new ImportLine(notebook, 30, decimal("16.00"))
        );
        createCompletedImportAt(
            admin,
            urbanDrinks,
            at(anchorDate, 20, 8, 20),
            "Main beverage replenishment",
            new ImportLine(sparklingWater, 80, decimal("7.50")),
            new ImportLine(oatMilk, 36, decimal("24.00")),
            new ImportLine(chocolateBar, 24, decimal("12.00")),
            new ImportLine(energyDrink, 60, decimal("10.50"))
        );
        createCompletedImportAt(
            admin,
            freshSource,
            at(anchorDate, 9, 8, 10),
            "Mid-cycle restock for fast movers",
            new ImportLine(sparklingWater, 40, decimal("7.80")),
            new ImportLine(oatMilk, 24, decimal("24.70")),
            new ImportLine(chocolateBar, 30, decimal("12.40"))
        );
        createCompletedImportAt(
            admin,
            urbanDrinks,
            at(anchorDate, 4, 8, 5),
            "Top-up on stable items",
            new ImportLine(notebook, 18, decimal("16.50")),
            new ImportLine(energyDrink, 48, decimal("10.80"))
        );

        createCompletedOrderAt(staff, lanAnh, at(anchorDate, 20, 12, 30), PaymentMethod.CASH,
            new OrderLine(sparklingWater, 5), new OrderLine(chocolateBar, 2));
        createCompletedOrderAt(manager, minhKhoa, at(anchorDate, 18, 14, 0), PaymentMethod.CARD,
            new OrderLine(sparklingWater, 4), new OrderLine(energyDrink, 2));
        createCompletedOrderAt(staff, thuyVan, at(anchorDate, 17, 10, 20), PaymentMethod.QR,
            new OrderLine(chocolateBar, 3), new OrderLine(notebook, 1));
        createCompletedOrderAt(manager, quocBao, at(anchorDate, 16, 16, 10), PaymentMethod.CASH,
            new OrderLine(detergent, 1));

        createCompletedOrderAt(staff, lanAnh, at(anchorDate, 13, 10, 15), PaymentMethod.CASH,
            new OrderLine(sparklingWater, 8), new OrderLine(chocolateBar, 3));
        createCompletedOrderAt(manager, minhKhoa, at(anchorDate, 12, 11, 5), PaymentMethod.CARD,
            new OrderLine(oatMilk, 4), new OrderLine(sparklingWater, 6));
        createCompletedOrderAt(staff, thuyVan, at(anchorDate, 11, 15, 40), PaymentMethod.QR,
            new OrderLine(chocolateBar, 4), new OrderLine(notebook, 2), new OrderLine(sparklingWater, 3));
        createCompletedOrderAt(manager, quocBao, at(anchorDate, 10, 17, 25), PaymentMethod.CARD,
            new OrderLine(sparklingWater, 7), new OrderLine(energyDrink, 2), new OrderLine(chocolateBar, 2));
        createCompletedOrderAt(staff, lanAnh, at(anchorDate, 9, 9, 45), PaymentMethod.CASH,
            new OrderLine(oatMilk, 3), new OrderLine(sparklingWater, 8));
        createCompletedOrderAt(manager, minhKhoa, at(anchorDate, 8, 13, 10), PaymentMethod.QR,
            new OrderLine(detergent, 1), new OrderLine(notebook, 1), new OrderLine(chocolateBar, 3));
        createCompletedOrderAt(staff, thuyVan, at(anchorDate, 7, 11, 20), PaymentMethod.CASH,
            new OrderLine(sparklingWater, 6), new OrderLine(oatMilk, 4));
        createCompletedOrderAt(manager, quocBao, at(anchorDate, 6, 16, 50), PaymentMethod.CARD,
            new OrderLine(chocolateBar, 4), new OrderLine(energyDrink, 2), new OrderLine(sparklingWater, 4));
        createCompletedOrderAt(staff, lanAnh, at(anchorDate, 5, 15, 30), PaymentMethod.QR,
            new OrderLine(sparklingWater, 9), new OrderLine(oatMilk, 5));
        createCompletedOrderAt(manager, minhKhoa, at(anchorDate, 4, 18, 5), PaymentMethod.CASH,
            new OrderLine(sparklingWater, 7), new OrderLine(notebook, 2), new OrderLine(detergent, 1));
        createCompletedOrderAt(staff, thuyVan, at(anchorDate, 3, 12, 40), PaymentMethod.CARD,
            new OrderLine(oatMilk, 6), new OrderLine(chocolateBar, 5));
        createCompletedOrderAt(manager, quocBao, at(anchorDate, 2, 17, 35), PaymentMethod.QR,
            new OrderLine(sparklingWater, 10), new OrderLine(oatMilk, 5), new OrderLine(energyDrink, 3));

        createCompletedOrderAt(staff, lanAnh, at(anchorDate, 1, 9, 10), PaymentMethod.CASH,
            new OrderLine(sparklingWater, 6), new OrderLine(chocolateBar, 3));
        createCompletedOrderAt(manager, minhKhoa, at(anchorDate, 1, 10, 15), PaymentMethod.CARD,
            new OrderLine(sparklingWater, 5), new OrderLine(oatMilk, 4), new OrderLine(chocolateBar, 1));
        createCompletedOrderAt(staff, thuyVan, at(anchorDate, 1, 11, 20), PaymentMethod.CARD,
            new OrderLine(oatMilk, 5), new OrderLine(notebook, 2));
        createCompletedOrderAt(manager, quocBao, at(anchorDate, 1, 13, 45), PaymentMethod.CASH,
            new OrderLine(chocolateBar, 4), new OrderLine(detergent, 1));
        createCompletedOrderAt(staff, lanAnh, at(anchorDate, 1, 15, 5), PaymentMethod.QR,
            new OrderLine(sparklingWater, 4), new OrderLine(energyDrink, 1));
        createCompletedOrderAt(manager, minhKhoa, at(anchorDate, 1, 18, 25), PaymentMethod.CARD,
            new OrderLine(sparklingWater, 3), new OrderLine(oatMilk, 3));

        createCompletedOrderAt(staff, lanAnh, at(anchorDate, 0, 9, 5), PaymentMethod.CASH,
            new OrderLine(oatMilk, 2), new OrderLine(chocolateBar, 1));
        createCompletedOrderAt(manager, quocBao, at(anchorDate, 0, 12, 10), PaymentMethod.CARD,
            new OrderLine(notebook, 1), new OrderLine(detergent, 1));
        createCanceledOrderAt(manager, manager, minhKhoa, at(anchorDate, 0, 14, 10), at(anchorDate, 0, 14, 35), PaymentMethod.CARD,
            "Customer changed basket before payment close",
            new OrderLine(sparklingWater, 4), new OrderLine(oatMilk, 2));
        createCanceledOrderAt(manager, manager, thuyVan, at(anchorDate, 0, 16, 0), at(anchorDate, 0, 16, 20), PaymentMethod.CASH,
            "Checkout void for duplicate scan",
            new OrderLine(chocolateBar, 3));

        long activeProducts = productRepository.findAll().stream().filter(product -> !product.isDeleted()).count();
        System.out.println(
            "Demo profile seeded: "
                + activeProducts
                + " active products, "
                + customerRepository.count()
                + " customers, "
                + orderRepository.count()
                + " orders and "
                + importOrderRepository.count()
                + " imports across roughly 12 months."
        );
        System.out.println("Demo accounts: admin/admin, manager/manager, staff/staff");
    }

    private void hideLegacySampleProduct() {
        productRepository.findAll().stream()
            .filter(product -> product.getName() != null && product.getName().equalsIgnoreCase("MacBook Pro"))
            .forEach(product -> {
                product.setDeleted(true);
                productRepository.save(product);
            });
    }

    private User ensureUser(String username, String rawPassword, String fullName, Role role, boolean forcePassword) {
        Optional<User> existing = userRepository.findByUsernameIgnoreCase(username);
        User user = existing.orElseGet(User::new);
        user.setUsername(username);
        user.setFullName(fullName);
        user.setRole(role);
        user.setEnabled(true);
        if (user.getId() == null || forcePassword || user.getPassword() == null || !passwordEncoder.matches(rawPassword, user.getPassword())) {
            user.setPassword(passwordEncoder.encode(rawPassword));
        }
        return userRepository.save(user);
    }

    private Category ensureCategory(String name) {
        return categoryRepository.findAll().stream()
            .filter(category -> category.getName() != null && category.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElseGet(() -> {
                Category category = new Category();
                category.setName(name);
                return categoryRepository.save(category);
            });
    }

    private Brand ensureBrand(String name) {
        return brandRepository.findAll().stream()
            .filter(brand -> brand.getName() != null && brand.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElseGet(() -> brandRepository.save(new Brand(name)));
    }

    private Origin ensureOrigin(String name) {
        return originRepository.findAll().stream()
            .filter(origin -> origin.getName() != null && origin.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElseGet(() -> originRepository.save(new Origin(name)));
    }

    private Unit ensureUnit(String name) {
        return unitRepository.findAll().stream()
            .filter(unit -> unit.getName() != null && unit.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElseGet(() -> unitRepository.save(new Unit(name)));
    }

    private Supplier ensureSupplier(String name, String phone, String address) {
        return supplierRepository.findAll().stream()
            .filter(supplier -> supplier.getName() != null && supplier.getName().equalsIgnoreCase(name))
            .findFirst()
            .map(existing -> {
                existing.setPhone(phone);
                existing.setAddress(address);
                existing.setDeleted(false);
                return supplierRepository.save(existing);
            })
            .orElseGet(() -> supplierRepository.save(new Supplier(name, phone, address)));
    }

    private Customer ensureCustomer(String fullName, String phone) {
        return customerRepository.findByPhoneIgnoreCase(phone)
            .map(existing -> {
                existing.setFullName(fullName);
                existing.setEnabled(true);
                return customerRepository.save(existing);
            })
            .orElseGet(() -> {
                Customer customer = new Customer();
                customer.setFullName(fullName);
                customer.setPhone(phone);
                customer.setEnabled(true);
                return customerRepository.save(customer);
            });
    }

    private Product ensureProduct(
        String name,
        String description,
        BigDecimal price,
        BigDecimal importPrice,
        int minStock,
        Category category,
        Brand brand,
        Origin origin,
        Unit unit,
        String sku,
        String barcode
    ) {
        Product product = productRepository.findAll().stream()
            .filter(existing -> existing.getName() != null && existing.getName().equalsIgnoreCase(name))
            .findFirst()
            .orElseGet(Product::new);
        product.setName(name);
        product.setDescription(description);
        product.setPrice(MoneySupport.normalize(price));
        product.setImportPrice(MoneySupport.normalize(importPrice));
        product.setQuantity(0);
        product.setMinStockLevel(minStock);
        product.setCategory(category);
        product.setBrand(brand);
        product.setOrigin(origin);
        product.setUnit(unit);
        product.setSku(sku);
        product.setBarcode(barcode);
        product.setDeleted(false);
        return productRepository.save(product);
    }

    private void ensureZeroBaseline(Product product, LocalDateTime baselineAt) {
        InventoryPositionBaseline baseline = baselineRepository.findByProductId(product.getId())
            .orElseGet(InventoryPositionBaseline::new);
        baseline.setProduct(product);
        baseline.setBaselineAt(baselineAt);
        baseline.setQuantity(0);
        baseline.setAverageCost(MoneySupport.ZERO);
        baseline.setInventoryValue(MoneySupport.ZERO);
        baselineRepository.save(baseline);
    }

    private List<Customer> ensureCustomers(CustomerSeed... seeds) {
        List<Customer> customers = new ArrayList<>();
        Arrays.stream(seeds).forEach(seed -> customers.add(ensureCustomer(seed.fullName(), seed.phone())));
        return customers;
    }

    private void seedHistoricalYear(
        LocalDate anchorDate,
        User admin,
        User manager,
        User staff,
        List<Customer> customers,
        List<HistoricalProductPlan> plans,
        Random random
    ) {
        LocalDate startDate = anchorDate.minusDays(HISTORICAL_DAYS - 1L);
        LocalDate endDate = anchorDate.minusDays(RECENT_SCENARIO_DAYS + 1L);
        Map<Long, Integer> stockByProductId = new HashMap<>();

        Map<Supplier, List<ImportLine>> openingImports = new LinkedHashMap<>();
        for (HistoricalProductPlan plan : plans) {
            openingImports.computeIfAbsent(plan.supplier(), ignored -> new ArrayList<>())
                .add(new ImportLine(plan.product(), plan.openingQuantity(), plan.baseImportPrice()));
            stockByProductId.put(plan.product().getId(), plan.openingQuantity());
        }

        int openingIndex = 0;
        for (Map.Entry<Supplier, List<ImportLine>> entry : openingImports.entrySet()) {
            createCompletedImportAt(
                admin,
                entry.getKey(),
                startDate.minusDays(2).atTime(8, 10 + (openingIndex++ * 7)),
                "Opening stock for 12-month demo trade history",
                entry.getValue().toArray(new ImportLine[0])
            );
        }

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            replenishHistoricalStock(date, admin, manager, plans, stockByProductId, random);

            int orderCount = computeHistoricalOrderCount(date, random);
            for (int orderIndex = 0; orderIndex < orderCount; orderIndex++) {
                List<OrderLine> lines = buildHistoricalBasket(date, plans, stockByProductId, random);
                if (lines.isEmpty()) {
                    continue;
                }

                User actor = pickSeller(staff, manager, random);
                Customer customer = pickCustomer(customers, random);
                PaymentMethod paymentMethod = pickPaymentMethod(random);
                LocalDateTime createdAt = date.atTime(9 + (orderIndex % 9), 5 + ((orderIndex * 11) % 50));
                createCompletedOrderAt(actor, customer, createdAt, paymentMethod, lines.toArray(new OrderLine[0]));
                applyStockChange(stockByProductId, lines, -1);
            }

            if (shouldCreateCanceledOrder(date, random)) {
                List<OrderLine> canceledLines = buildHistoricalBasket(date, plans, stockByProductId, random);
                if (!canceledLines.isEmpty()) {
                    User actor = pickSeller(staff, manager, random);
                    LocalDateTime createdAt = date.atTime(19, 10 + random.nextInt(25));
                    createCanceledOrderAt(
                        actor,
                        manager,
                        pickCustomer(customers, random),
                        createdAt,
                        createdAt.plusMinutes(12 + random.nextInt(15)),
                        pickPaymentMethod(random),
                        randomCancelReason(random),
                        canceledLines.toArray(new OrderLine[0])
                    );
                }
            }
        }
    }

    private void replenishHistoricalStock(
        LocalDate date,
        User admin,
        User manager,
        List<HistoricalProductPlan> plans,
        Map<Long, Integer> stockByProductId,
        Random random
    ) {
        Map<Supplier, List<ImportLine>> groupedImports = new LinkedHashMap<>();
        for (HistoricalProductPlan plan : plans) {
            int onHand = stockByProductId.getOrDefault(plan.product().getId(), 0);
            int reorderPoint = seasonAdjustedUnits(plan.reorderPointUnits(), plan.seasonality(), date);
            if (onHand > reorderPoint) {
                continue;
            }

            int targetUnits = seasonAdjustedUnits(plan.targetUnits(), plan.seasonality(), date);
            int shortage = Math.max(targetUnits - onHand, plan.minBatchQuantity());
            int importQuantity = roundUpToBatch(shortage, plan.minBatchQuantity());
            BigDecimal importPrice = varyImportPrice(plan.baseImportPrice(), date, random);

            groupedImports.computeIfAbsent(plan.supplier(), ignored -> new ArrayList<>())
                .add(new ImportLine(plan.product(), importQuantity, importPrice));
            stockByProductId.put(plan.product().getId(), onHand + importQuantity);
        }

        int supplierOffset = 0;
        for (Map.Entry<Supplier, List<ImportLine>> entry : groupedImports.entrySet()) {
            User actor = supplierOffset % 2 == 0 ? admin : manager;
            createCompletedImportAt(
                actor,
                entry.getKey(),
                date.atTime(8, 5 + (supplierOffset++ * 8)),
                "Routine replenishment for demo yearly trade flow",
                entry.getValue().toArray(new ImportLine[0])
            );
        }
    }

    private int computeHistoricalOrderCount(LocalDate date, Random random) {
        double expected = switch (date.getDayOfWeek()) {
            case SATURDAY -> 2.4;
            case SUNDAY -> 2.1;
            case FRIDAY -> 1.9;
            case MONDAY -> 1.3;
            default -> 1.6;
        };

        int month = date.getMonthValue();
        if (month == 1 || month == 2 || month == 11 || month == 12) {
            expected *= 1.25;
        } else if (month == 8 || month == 9) {
            expected *= 1.15;
        }

        int dayOfMonth = date.getDayOfMonth();
        if (dayOfMonth <= 3 || (dayOfMonth >= 15 && dayOfMonth <= 17)) {
            expected *= 1.08;
        }

        int count = (int) Math.floor(expected);
        if (random.nextDouble() < expected - count) {
            count++;
        }
        if (random.nextDouble() < 0.28) {
            count++;
        }
        if ((date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) && random.nextDouble() < 0.16) {
            count++;
        }
        return Math.max(0, count);
    }

    private List<OrderLine> buildHistoricalBasket(
        LocalDate date,
        List<HistoricalProductPlan> plans,
        Map<Long, Integer> stockByProductId,
        Random random
    ) {
        List<OrderLine> lines = new ArrayList<>();
        int desiredLines = random.nextDouble() < 0.18 ? 3 : (random.nextDouble() < 0.62 ? 2 : 1);
        List<Long> chosenProductIds = new ArrayList<>();

        for (int lineIndex = 0; lineIndex < desiredLines; lineIndex++) {
            HistoricalProductPlan selectedPlan = pickHistoricalPlan(date, plans, stockByProductId, chosenProductIds, random);
            if (selectedPlan == null) {
                break;
            }
            int onHand = stockByProductId.getOrDefault(selectedPlan.product().getId(), 0);
            if (onHand <= 0) {
                continue;
            }
            int quantity = sampleLineQuantity(selectedPlan, onHand, random);
            lines.add(new OrderLine(selectedPlan.product(), quantity));
            chosenProductIds.add(selectedPlan.product().getId());
        }

        return lines;
    }

    private HistoricalProductPlan pickHistoricalPlan(
        LocalDate date,
        List<HistoricalProductPlan> plans,
        Map<Long, Integer> stockByProductId,
        List<Long> excludedProductIds,
        Random random
    ) {
        double totalWeight = 0.0;
        for (HistoricalProductPlan plan : plans) {
            if (excludedProductIds.contains(plan.product().getId())) {
                continue;
            }
            if (stockByProductId.getOrDefault(plan.product().getId(), 0) <= 0) {
                continue;
            }
            totalWeight += effectiveDemandWeight(plan, date);
        }

        if (totalWeight <= 0) {
            return null;
        }

        double roll = random.nextDouble() * totalWeight;
        double cumulative = 0.0;
        for (HistoricalProductPlan plan : plans) {
            if (excludedProductIds.contains(plan.product().getId())) {
                continue;
            }
            if (stockByProductId.getOrDefault(plan.product().getId(), 0) <= 0) {
                continue;
            }
            cumulative += effectiveDemandWeight(plan, date);
            if (roll <= cumulative) {
                return plan;
            }
        }
        return null;
    }

    private double effectiveDemandWeight(HistoricalProductPlan plan, LocalDate date) {
        return plan.selectionWeight() * seasonalDemandFactor(plan.seasonality(), date);
    }

    private double seasonalDemandFactor(DemandSeasonality seasonality, LocalDate date) {
        int month = date.getMonthValue();
        return switch (seasonality) {
            case SUMMER -> (month >= 4 && month <= 8) ? 1.28 : (month == 9 || month == 10 ? 1.10 : 0.88);
            case WEEKEND -> (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) ? 1.25 : 0.95;
            case SCHOOL -> (month == 8 || month == 9) ? 1.55 : (month == 1 ? 1.12 : 0.84);
            case FESTIVE -> (month == 1 || month == 2 || month == 11 || month == 12) ? 1.30 : 1.00;
            case STABLE -> 1.00;
        };
    }

    private int seasonAdjustedUnits(int baseUnits, DemandSeasonality seasonality, LocalDate date) {
        return Math.max(1, (int) Math.round(baseUnits * seasonalDemandFactor(seasonality, date)));
    }

    private int sampleLineQuantity(HistoricalProductPlan plan, int onHand, Random random) {
        int maxAllowed = Math.min(plan.maxUnitsPerLine(), onHand);
        if (maxAllowed <= 1) {
            return 1;
        }

        double roll = random.nextDouble();
        int quantity;
        if (roll > 0.83 && maxAllowed >= 4) {
            quantity = 3 + random.nextInt(maxAllowed - 2);
        } else if (roll > 0.45 && maxAllowed >= 2) {
            quantity = 2;
        } else {
            quantity = 1;
        }
        return Math.min(quantity, maxAllowed);
    }

    private void applyStockChange(Map<Long, Integer> stockByProductId, List<OrderLine> lines, int direction) {
        for (OrderLine line : lines) {
            Long productId = line.product().getId();
            int current = stockByProductId.getOrDefault(productId, 0);
            stockByProductId.put(productId, Math.max(0, current + (direction * line.quantity())));
        }
    }

    private User pickSeller(User staff, User manager, Random random) {
        return random.nextDouble() < 0.68 ? staff : manager;
    }

    private Customer pickCustomer(List<Customer> customers, Random random) {
        if (customers.isEmpty() || random.nextDouble() < 0.18) {
            return null;
        }
        return customers.get(random.nextInt(customers.size()));
    }

    private PaymentMethod pickPaymentMethod(Random random) {
        double roll = random.nextDouble();
        if (roll < 0.44) {
            return PaymentMethod.CASH;
        }
        if (roll < 0.79) {
            return PaymentMethod.CARD;
        }
        return PaymentMethod.QR;
    }

    private boolean shouldCreateCanceledOrder(LocalDate date, Random random) {
        double probability = (date.getDayOfWeek() == DayOfWeek.FRIDAY || date.getDayOfWeek() == DayOfWeek.SATURDAY) ? 0.10 : 0.06;
        return random.nextDouble() < probability;
    }

    private String randomCancelReason(Random random) {
        List<String> reasons = List.of(
            "Customer changed basket before checkout",
            "Cashier void for duplicate barcode scan",
            "Customer requested payment rollback",
            "Counter hold released before payment confirmation"
        );
        return reasons.get(random.nextInt(reasons.size()));
    }

    private BigDecimal varyImportPrice(BigDecimal basePrice, LocalDate date, Random random) {
        BigDecimal monthlyDrift = BigDecimal.valueOf(Math.max(0, date.getMonthValue() - 1))
            .multiply(new BigDecimal("0.003"));
        BigDecimal randomDrift = BigDecimal.valueOf((random.nextDouble() * 0.06) - 0.03);
        BigDecimal multiplier = BigDecimal.ONE.add(monthlyDrift).add(randomDrift);
        return MoneySupport.normalize(basePrice.multiply(multiplier));
    }

    private int roundUpToBatch(int quantity, int batchSize) {
        if (batchSize <= 1) {
            return Math.max(1, quantity);
        }
        int normalized = Math.max(batchSize, quantity);
        int remainder = normalized % batchSize;
        return remainder == 0 ? normalized : normalized + (batchSize - remainder);
    }

    private ImportOrder createCompletedImportAt(
        User actor,
        Supplier supplier,
        LocalDateTime createdAt,
        String notes,
        ImportLine... lines
    ) {
        CreateImportOrderRequest request = new CreateImportOrderRequest();
        request.setSupplierId(supplier.getId());
        request.setUserId(actor.getId());
        request.setNotes(notes);

        List<CreateImportOrderRequest.ImportOrderItemRequest> items = new ArrayList<>();
        Arrays.stream(lines).forEach(line -> {
            CreateImportOrderRequest.ImportOrderItemRequest itemRequest = new CreateImportOrderRequest.ImportOrderItemRequest();
            itemRequest.setProductId(line.product().getId());
            itemRequest.setQuantity(line.quantity());
            itemRequest.setImportPrice(line.importPrice());
            items.add(itemRequest);
        });
        request.setItems(items);

        ImportOrder order = importOrderService.createImportOrder(request);
        backdateImportTimeline(order, createdAt, null);
        return order;
    }

    private Order createCompletedOrderAt(
        User actor,
        Customer customer,
        LocalDateTime createdAt,
        PaymentMethod paymentMethod,
        OrderLine... lines
    ) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(actor.getId());
        request.setCustomerId(customer != null ? customer.getId() : null);
        request.setPaymentMethod(paymentMethod);

        ArrayList<CreateOrderRequest.OrderItemRequest> items = new ArrayList<>();
        Arrays.stream(lines).forEach(line -> {
            CreateOrderRequest.OrderItemRequest itemRequest = new CreateOrderRequest.OrderItemRequest();
            itemRequest.setProductId(line.product().getId());
            itemRequest.setQuantity(line.quantity());
            items.add(itemRequest);
        });
        request.setItems(items);

        Order order = orderService.createOrder(request);
        backdateOrderTimeline(order, createdAt, null);
        return order;
    }

    private Order createCanceledOrderAt(
        User seller,
        User cancelActor,
        Customer customer,
        LocalDateTime createdAt,
        LocalDateTime canceledAt,
        PaymentMethod paymentMethod,
        String reason,
        OrderLine... lines
    ) {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setUserId(seller.getId());
        request.setCustomerId(customer != null ? customer.getId() : null);
        request.setPaymentMethod(paymentMethod);

        ArrayList<CreateOrderRequest.OrderItemRequest> items = new ArrayList<>();
        Arrays.stream(lines).forEach(line -> {
            CreateOrderRequest.OrderItemRequest itemRequest = new CreateOrderRequest.OrderItemRequest();
            itemRequest.setProductId(line.product().getId());
            itemRequest.setQuantity(line.quantity());
            items.add(itemRequest);
        });
        request.setItems(items);

        Order created = orderService.createOrder(request);
        Order canceled = orderService.cancelOrder(created.getId(), cancelActor.getId(), reason);
        backdateOrderTimeline(canceled, createdAt, canceledAt);
        return canceled;
    }

    private void backdateImportTimeline(ImportOrder order, LocalDateTime createdAt, LocalDateTime canceledAt) {
        order.setCreatedAt(createdAt);
        importOrderRepository.save(order);

        List<InventoryTransaction> transactions = inventoryTransactionRepository.findByImportOrderIdOrderByCreatedAtAscIdAsc(order.getId());
        int createdOffset = 0;
        int canceledOffset = 0;
        for (InventoryTransaction transaction : transactions) {
            LocalDateTime targetTime = transaction.getTransactionType() == InventoryTransactionType.CANCEL_IMPORT
                ? (canceledAt != null ? canceledAt.plusSeconds(canceledOffset++) : createdAt.plusMinutes(25).plusSeconds(canceledOffset++))
                : createdAt.plusSeconds(createdOffset++);
            inventoryTransactionRepository.overrideCreatedAt(transaction.getId(), targetTime);
        }

        List<OperationalAuditLog> auditLogs = operationalAuditLogRepository
            .findBySubjectTypeAndSubjectIdOrderByCreatedAtAscIdAsc(OperationalSubjectType.IMPORT_ORDER, order.getId());
        int auditCreateOffset = 0;
        int auditCancelOffset = 0;
        for (OperationalAuditLog log : auditLogs) {
            LocalDateTime targetTime = log.getAction() == OperationalAuditAction.IMPORT_CANCELED
                ? (canceledAt != null ? canceledAt.plusMinutes(1).plusSeconds(auditCancelOffset++) : createdAt.plusMinutes(30).plusSeconds(auditCancelOffset++))
                : createdAt.plusMinutes(1).plusSeconds(auditCreateOffset++);
            operationalAuditLogRepository.overrideCreatedAt(log.getId(), targetTime);
        }
    }

    private void backdateOrderTimeline(Order order, LocalDateTime createdAt, LocalDateTime canceledAt) {
        order.setCreatedAt(createdAt);
        orderRepository.save(order);

        List<InventoryTransaction> transactions = inventoryTransactionRepository.findByOrderIdOrderByCreatedAtAscIdAsc(order.getId());
        int saleOffset = 0;
        int reverseOffset = 0;
        for (InventoryTransaction transaction : transactions) {
            boolean reverseTransaction = transaction.getTransactionType() == InventoryTransactionType.CANCEL_SALE
                || transaction.getTransactionType() == InventoryTransactionType.RETURN;
            LocalDateTime targetTime = reverseTransaction
                ? (canceledAt != null ? canceledAt.plusSeconds(reverseOffset++) : createdAt.plusMinutes(25).plusSeconds(reverseOffset++))
                : createdAt.plusSeconds(saleOffset++);
            inventoryTransactionRepository.overrideCreatedAt(transaction.getId(), targetTime);
        }

        List<OperationalAuditLog> auditLogs = operationalAuditLogRepository
            .findBySubjectTypeAndSubjectIdOrderByCreatedAtAscIdAsc(OperationalSubjectType.ORDER, order.getId());
        int createAuditOffset = 0;
        int closeAuditOffset = 0;
        for (OperationalAuditLog log : auditLogs) {
            boolean closingAudit = log.getAction() == OperationalAuditAction.ORDER_CANCELED
                || log.getAction() == OperationalAuditAction.ORDER_RETURNED;
            LocalDateTime targetTime = closingAudit
                ? (canceledAt != null ? canceledAt.plusMinutes(1).plusSeconds(closeAuditOffset++) : createdAt.plusMinutes(30).plusSeconds(closeAuditOffset++))
                : createdAt.plusMinutes(1).plusSeconds(createAuditOffset++);
            operationalAuditLogRepository.overrideCreatedAt(log.getId(), targetTime);
        }
    }

    private void seedAccountAudit(User actor, User target, AccountAuditAction action, LocalDateTime createdAt, String details) {
        boolean exists = accountAuditLogRepository.findAll().stream().anyMatch(log ->
            log.getActor() != null
                && log.getTargetUser() != null
                && log.getActor().getId().equals(actor.getId())
                && log.getTargetUser().getId().equals(target.getId())
                && log.getAction() == action
                && details.equals(log.getDetails())
        );
        if (exists) {
            return;
        }
        AccountAuditLog log = new AccountAuditLog();
        log.setActor(actor);
        log.setTargetUser(target);
        log.setAction(action);
        log.setDetails(details);
        AccountAuditLog saved = accountAuditLogRepository.save(log);
        accountAuditLogRepository.overrideCreatedAt(saved.getId(), createdAt);
    }

    private LocalDateTime at(LocalDate anchorDate, int daysAgo, int hour, int minute) {
        return anchorDate.minusDays(daysAgo).atTime(hour, minute);
    }

    private BigDecimal decimal(String value) {
        return MoneySupport.normalize(new BigDecimal(value).multiply(new BigDecimal("1000")));
    }

    private record CustomerSeed(String fullName, String phone) {
    }

    private record HistoricalProductPlan(
        Product product,
        Supplier supplier,
        BigDecimal baseImportPrice,
        int openingQuantity,
        int reorderPointUnits,
        int targetUnits,
        int minBatchQuantity,
        int selectionWeight,
        int maxUnitsPerLine,
        DemandSeasonality seasonality
    ) {
    }

    private enum DemandSeasonality {
        STABLE,
        SUMMER,
        WEEKEND,
        SCHOOL,
        FESTIVE
    }

    private record ImportLine(Product product, int quantity, BigDecimal importPrice) {
    }

    private record OrderLine(Product product, int quantity) {
    }
}
