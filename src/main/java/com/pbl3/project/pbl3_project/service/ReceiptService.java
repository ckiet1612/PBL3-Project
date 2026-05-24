package com.pbl3.project.pbl3_project.service;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.entity.OrderItem;
import com.pbl3.project.pbl3_project.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

@Service
public class ReceiptService {

    private static final Rectangle RECEIPT_SIZE = new Rectangle(226, 800);
    private static final DateTimeFormatter RECEIPT_DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final String DEFAULT_BUSINESS_NAME = "PBL3 STORE";
    private static final String DEFAULT_FOOTER_NOTE = "Please keep this receipt for returns and warranty support.";
    private static final String RECEIPT_OUTPUT_DIRECTORY_KEY = "receipt.output.directory";
    private static final String RECEIPT_STORE_PHONE_KEY = "receipt.store.phone";
    private static final String RECEIPT_STORE_ADDRESS_KEY = "receipt.store.address";
    private static final String RECEIPT_FOOTER_NOTE_KEY = "receipt.footer.note";
    private static final String RECEIPT_LOGO_PATH_KEY = "receipt.logo.path";

    public record ReceiptSettings(
        String storePhone,
        String storeAddress,
        String footerNote,
        File logoFile
    ) {
    }

    private final String businessName;
    private final Preferences localPreferences;
    private final OrderRepository orderRepository;

    @Autowired
    public ReceiptService(
        @Value("${app.business.name:PBL3 STORE}") String businessName,
        OrderRepository orderRepository
    ) {
        this(businessName, Preferences.userNodeForPackage(ReceiptService.class), orderRepository);
    }

    ReceiptService(String businessName, Preferences localPreferences) {
        this(businessName, localPreferences, null);
    }

    ReceiptService(String businessName, Preferences localPreferences, OrderRepository orderRepository) {
        this.businessName = normalizeBusinessName(businessName);
        this.localPreferences = localPreferences;
        this.orderRepository = orderRepository;
    }

    public void generateAndOpenReceipt(Order order) {
        File pdfFile = generateReceipt(order);
        try {
            openPdfIfSupported(pdfFile);
        } catch (Exception ex) {
            throw new ReceiptGenerationException("Receipt PDF was generated but could not be opened.", ex);
        }
    }

    public File generateReceipt(Order order) {
        if (order == null) {
            throw new ReceiptGenerationException("Order is required to generate a receipt.");
        }

        File pdfFile = resolveReceiptFile(order);
        Document document = new Document(RECEIPT_SIZE, 10, 10, 15, 15);
        try {
            PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
            document.addTitle("POS Receipt #" + safeOrderId(order));
            document.addCreator("Sales Management System");

            document.open();
            ReceiptFonts fonts = ReceiptFonts.create();

            addStoreHeader(document, fonts);
            addReceiptInformation(document, order, fonts);
            addSeparator(document);
            document.add(createItemsTable(order, fonts));
            addSeparator(document);
            document.add(createTotalsTable(order, fonts));
            addFooter(document, fonts);
            document.close();
            recordReceiptMetadata(order, pdfFile);
            return pdfFile;
        } catch (Exception ex) {
            throw new ReceiptGenerationException("Could not generate receipt PDF.", ex);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    public boolean hasStoredReceiptFile(Order order) {
        return resolveStoredReceiptFile(order).filter(File::isFile).isPresent();
    }

    public void openStoredReceipt(Order order) {
        File receiptFile = resolveStoredReceiptFile(order)
            .orElseThrow(() -> new ReceiptGenerationException("Receipt file path is not available."));
        if (!receiptFile.isFile()) {
            throw new ReceiptGenerationException("Receipt file was not found. Regenerate the receipt.");
        }
        try {
            openPdfIfSupported(receiptFile);
        } catch (Exception ex) {
            throw new ReceiptGenerationException("Receipt PDF exists but could not be opened.", ex);
        }
    }

    public File getReceiptOutputDirectory() {
        String configuredPath = localPreferences.get(RECEIPT_OUTPUT_DIRECTORY_KEY, null);
        if (configuredPath == null || configuredPath.isBlank()) {
            return getDefaultReceiptOutputDirectory();
        }
        return new File(configuredPath).toPath().toAbsolutePath().normalize().toFile();
    }

    public File getDefaultReceiptOutputDirectory() {
        return new File(new File(System.getProperty("user.home"), "Desktop"), "PBL3_Invoices")
            .toPath()
            .toAbsolutePath()
            .normalize()
            .toFile();
    }

    public ReceiptSettings getReceiptSettings() {
        return new ReceiptSettings(
            localPreferences.get(RECEIPT_STORE_PHONE_KEY, ""),
            localPreferences.get(RECEIPT_STORE_ADDRESS_KEY, ""),
            localPreferences.get(RECEIPT_FOOTER_NOTE_KEY, DEFAULT_FOOTER_NOTE),
            readConfiguredLogoFile()
        );
    }

    public void saveReceiptSettings(ReceiptSettings settings) {
        ReceiptSettings normalized = normalizeReceiptSettings(settings);
        putOrRemove(RECEIPT_STORE_PHONE_KEY, normalized.storePhone());
        putOrRemove(RECEIPT_STORE_ADDRESS_KEY, normalized.storeAddress());
        if (isBlank(normalized.footerNote()) || DEFAULT_FOOTER_NOTE.equals(normalized.footerNote())) {
            localPreferences.remove(RECEIPT_FOOTER_NOTE_KEY);
        } else {
            localPreferences.put(RECEIPT_FOOTER_NOTE_KEY, normalized.footerNote());
        }
        if (normalized.logoFile() == null) {
            localPreferences.remove(RECEIPT_LOGO_PATH_KEY);
        } else {
            localPreferences.put(RECEIPT_LOGO_PATH_KEY, normalized.logoFile().getAbsolutePath());
        }
        flushPreferences();
    }

    public void saveReceiptOutputDirectory(File directory) {
        File normalizedDirectory = normalizeOutputDirectory(directory);
        prepareReceiptOutputDirectory(normalizedDirectory);
        if (sameFile(normalizedDirectory, getDefaultReceiptOutputDirectory())) {
            localPreferences.remove(RECEIPT_OUTPUT_DIRECTORY_KEY);
        } else {
            localPreferences.put(RECEIPT_OUTPUT_DIRECTORY_KEY, normalizedDirectory.getAbsolutePath());
        }
        flushPreferences();
    }

    public String previewReceiptFilePath(File directory) {
        File baseDirectory = directory != null
            ? directory.toPath().toAbsolutePath().normalize().toFile()
            : getReceiptOutputDirectory();
        return new File(baseDirectory, receiptFileName("<order-id>")).getAbsolutePath();
    }

    private File resolveReceiptFile(Order order) {
        File invoiceDir = prepareReceiptOutputDirectory(getReceiptOutputDirectory());
        return new File(invoiceDir, receiptFileName(order));
    }

    protected void recordReceiptMetadata(Order order, File pdfFile) {
        if (order == null || order.getId() == null || pdfFile == null) {
            return;
        }
        String receiptPath = pdfFile.toPath().toAbsolutePath().normalize().toString();
        LocalDateTime generatedAt = LocalDateTime.now();
        order.setReceiptFilePath(receiptPath);
        order.setReceiptGeneratedAt(generatedAt);
        if (orderRepository != null) {
            orderRepository.updateReceiptMetadata(order.getId(), receiptPath, generatedAt);
        }
    }

    private java.util.Optional<File> resolveStoredReceiptFile(Order order) {
        if (order == null) {
            return java.util.Optional.empty();
        }
        if (!isBlank(order.getReceiptFilePath())) {
            File recordedFile = new File(order.getReceiptFilePath()).toPath().toAbsolutePath().normalize().toFile();
            if (recordedFile.isFile()) {
                return java.util.Optional.of(recordedFile);
            }
        }
        File configuredFolderFile = new File(getReceiptOutputDirectory(), receiptFileName(order))
            .toPath()
            .toAbsolutePath()
            .normalize()
            .toFile();
        return java.util.Optional.of(configuredFolderFile);
    }

    private File normalizeOutputDirectory(File directory) {
        if (directory == null) {
            throw new ReceiptGenerationException("Receipt output folder is required.");
        }
        Path normalizedPath = directory.toPath().toAbsolutePath().normalize();
        return normalizedPath.toFile();
    }

    private File prepareReceiptOutputDirectory(File directory) {
        try {
            Path directoryPath = normalizeOutputDirectory(directory).toPath();
            Files.createDirectories(directoryPath);
            if (!Files.isDirectory(directoryPath)) {
                throw new ReceiptGenerationException("Receipt output path is not a folder.");
            }
            if (!Files.isWritable(directoryPath)) {
                throw new ReceiptGenerationException("Receipt output folder is not writable.");
            }
            return directoryPath.toFile();
        } catch (IOException ex) {
            throw new ReceiptGenerationException("Could not prepare receipt output folder.", ex);
        }
    }

    private boolean sameFile(File first, File second) {
        if (first == null || second == null) {
            return false;
        }
        return first.toPath().toAbsolutePath().normalize().equals(second.toPath().toAbsolutePath().normalize());
    }

    private void flushPreferences() {
        try {
            localPreferences.flush();
        } catch (BackingStoreException ex) {
            throw new ReceiptGenerationException("Could not save receipt settings.", ex);
        }
    }

    private void addStoreHeader(Document document, ReceiptFonts fonts) throws Exception {
        ReceiptSettings settings = getReceiptSettings();
        addConfiguredLogo(document, settings);

        Paragraph title = new Paragraph(businessName, fonts.title());
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(2f);
        document.add(title);

        if (!isBlank(settings.storeAddress())) {
            Paragraph address = new Paragraph(settings.storeAddress(), fonts.small());
            address.setAlignment(Element.ALIGN_CENTER);
            address.setSpacingAfter(1f);
            document.add(address);
        }

        if (!isBlank(settings.storePhone())) {
            Paragraph phone = new Paragraph("Phone: " + settings.storePhone(), fonts.small());
            phone.setAlignment(Element.ALIGN_CENTER);
            phone.setSpacingAfter(2f);
            document.add(phone);
        }

        Paragraph subtitle = new Paragraph("POS RECEIPT", fonts.header());
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(2f);
        document.add(subtitle);

        Paragraph thanks = new Paragraph("Thank you for your purchase!", fonts.small());
        thanks.setAlignment(Element.ALIGN_CENTER);
        thanks.setSpacingAfter(6f);
        document.add(thanks);
    }

    private void addReceiptInformation(Document document, Order order, ReceiptFonts fonts) throws Exception {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3f, 5f});

        addInfoRow(table, "Receipt #", "#" + safeOrderId(order), fonts);
        addInfoRow(table, "Date", formatDateTime(order), fonts);
        addInfoRow(table, "Cashier", order.getCreatedByDisplayName(), fonts);
        addInfoRow(table, "Customer", order.hasCustomer() ? order.getCustomerDisplayName() : "Guest", fonts);
        addInfoRow(table, "Phone", order.hasCustomer() ? order.getCustomerPhoneDisplay() : "-", fonts);
        addInfoRow(table, "Payment", formatEnumLabel(order.getPaymentMethod()), fonts);
        addInfoRow(table, "Status", formatEnumLabel(order.getStatus()), fonts);
        document.add(table);
        document.add(new Paragraph(" "));
    }

    private void addInfoRow(PdfPTable table, String label, String value, ReceiptFonts fonts) {
        table.addCell(createBorderlessCell(label + ":", fonts.smallBold(), Element.ALIGN_LEFT));
        table.addCell(createBorderlessCell(safeText(value, "-"), fonts.small(), Element.ALIGN_LEFT));
    }

    private void addSeparator(Document document) throws Exception {
        document.add(new Chunk(new LineSeparator(0.5f, 100, BaseColor.LIGHT_GRAY, Element.ALIGN_CENTER, -2)));
        document.add(new Paragraph(" "));
    }

    private PdfPTable createItemsTable(Order order, ReceiptFonts fonts) throws Exception {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4.7f, 1f, 2.2f, 2.4f});
        table.addCell(createBorderlessCell("Item", fonts.header(), Element.ALIGN_LEFT));
        table.addCell(createBorderlessCell("Qty", fonts.header(), Element.ALIGN_CENTER));
        table.addCell(createBorderlessCell("Unit", fonts.header(), Element.ALIGN_RIGHT));
        table.addCell(createBorderlessCell("Total", fonts.header(), Element.ALIGN_RIGHT));

        for (OrderItem item : safeOrderItems(order)) {
            table.addCell(createItemCell(item, fonts));
            table.addCell(createBorderlessCell(String.valueOf(safeQuantity(item)), fonts.normal(), Element.ALIGN_CENTER));
            table.addCell(createBorderlessCell(formatCurrency(item.getOriginalUnitPriceSnapshot()), fonts.normal(), Element.ALIGN_RIGHT));
            table.addCell(createBorderlessCell(formatCurrency(item.getLineNetAmount()), fonts.normal(), Element.ALIGN_RIGHT));
        }
        return table;
    }

    private PdfPCell createItemCell(OrderItem item, ReceiptFonts fonts) {
        Phrase phrase = new Phrase();
        phrase.add(new Chunk(item.getProductDisplayName(), fonts.normal()));

        List<String> metaParts = new ArrayList<>();
        String sku = resolveSku(item);
        if (!isBlank(sku)) {
            metaParts.add("SKU: " + sku);
        }
        String unit = resolveUnit(item);
        if (!isBlank(unit)) {
            metaParts.add("Unit: " + unit);
        }
        if (!metaParts.isEmpty()) {
            phrase.add(new Chunk("\n" + String.join(" | ", metaParts), fonts.tiny()));
        }

        if (!isBlank(item.getAppliedProductPromotionNameSnapshot())) {
            phrase.add(new Chunk("\nPromo: " + item.getAppliedProductPromotionNameSnapshot(), fonts.tiny()));
        }
        if (MoneySupport.isPositive(item.getLinePromotionDiscountAmountSnapshot())) {
            phrase.add(new Chunk("\nProduct discount: -" + formatCurrency(item.getLinePromotionDiscountAmountSnapshot()), fonts.tiny()));
        }
        if (MoneySupport.isPositive(item.getOrderLevelDiscountAllocatedAmountSnapshot())) {
            phrase.add(new Chunk("\nOrder discount: -" + formatCurrency(item.getOrderLevelDiscountAllocatedAmountSnapshot()), fonts.tiny()));
        }

        return createBorderlessCell(phrase, Element.ALIGN_LEFT);
    }

    private PdfPTable createTotalsTable(Order order, ReceiptFonts fonts) throws Exception {
        PdfPTable totalTable = new PdfPTable(2);
        totalTable.setWidthPercentage(100);
        totalTable.setWidths(new float[]{5.5f, 4.5f});

        addTotalRow(totalTable, "Gross Subtotal", order.getGrossSubtotalSnapshot(), fonts.normal(), fonts.normal());

        if (MoneySupport.isPositive(order.getProductLevelDiscountTotalSnapshot())) {
            addTotalRow(totalTable, "Product Discount", "-" + formatCurrency(order.getProductLevelDiscountTotalSnapshot()), fonts.normal(), fonts.normal());
        }

        if (MoneySupport.isPositive(order.getOrderLevelDiscountTotalSnapshot())) {
            addTotalRow(totalTable, formatOrderPromotionLabel(order), "-" + formatCurrency(order.getOrderLevelDiscountTotalSnapshot()), fonts.normal(), fonts.normal());
        }

        addTotalRow(totalTable, "Amount Paid", order.getTotalPrice(), fonts.total(), fonts.total());

        if (MoneySupport.isPositive(order.getRefundedAmount())) {
            addTotalRow(totalTable, "Refunded", "-" + formatCurrency(order.getRefundedAmount()), fonts.normal(), fonts.normal());
            addTotalRow(totalTable, "Net After Refunds", order.getNetTotal(), fonts.total(), fonts.total());
        }

        return totalTable;
    }

    private String formatOrderPromotionLabel(Order order) {
        String promotionName = order.getAppliedOrderPromotionNameSnapshot();
        if (promotionName != null && !promotionName.isBlank()) {
            return "Order Promo (" + promotionName + ")";
        }
        return "Order Promotion";
    }

    private void addTotalRow(PdfPTable table, String label, BigDecimal amount, Font labelFont, Font valueFont) {
        addTotalRow(table, label, formatCurrency(amount), labelFont, valueFont);
    }

    private void addTotalRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        table.addCell(createBorderlessCell(label + ":", labelFont, Element.ALIGN_LEFT));
        table.addCell(createBorderlessCell(value, valueFont, Element.ALIGN_RIGHT));
    }

    private void addFooter(Document document, ReceiptFonts fonts) throws Exception {
        document.add(new Paragraph(" "));
        String footerNote = safeText(getReceiptSettings().footerNote(), DEFAULT_FOOTER_NOTE);
        Paragraph footer = new Paragraph(footerNote, fonts.small());
        footer.setAlignment(Element.ALIGN_CENTER);
        document.add(footer);
    }

    private void addConfiguredLogo(Document document, ReceiptSettings settings) throws Exception {
        File logoFile = settings.logoFile();
        if (logoFile == null || !logoFile.isFile()) {
            return;
        }
        Image logo = Image.getInstance(logoFile.getAbsolutePath());
        logo.scaleToFit(42f, 42f);
        logo.setAlignment(Element.ALIGN_CENTER);
        logo.setSpacingAfter(3f);
        document.add(logo);
    }

    private void openPdfIfSupported(File pdfFile) throws Exception {
        if (pdfFile == null || !pdfFile.isFile()) {
            throw new IOException("Receipt PDF file was not found.");
        }

        Exception commandFailure = null;
        try {
            openWithPlatformCommand(pdfFile);
            return;
        } catch (IOException ex) {
            commandFailure = ex;
        }

        try {
            if (java.awt.Desktop.isDesktopSupported()
                && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
                java.awt.Desktop.getDesktop().open(pdfFile);
                return;
            }
        } catch (Exception ex) {
            if (commandFailure != null) {
                ex.addSuppressed(commandFailure);
            }
            throw ex;
        }

        IOException ex = new IOException("No supported PDF opener is available on this computer.");
        if (commandFailure != null) {
            ex.addSuppressed(commandFailure);
        }
        throw ex;
    }

    private void openWithPlatformCommand(File pdfFile) throws IOException {
        String absolutePath = pdfFile.getAbsolutePath();
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        List<String> command;
        if (osName.contains("mac")) {
            command = List.of("open", absolutePath);
        } else if (osName.contains("win")) {
            command = List.of("cmd", "/c", "start", "", absolutePath);
        } else {
            command = List.of("xdg-open", absolutePath);
        }
        new ProcessBuilder(command).start();
    }

    private PdfPCell createBorderlessCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        configureBorderlessCell(cell, alignment);
        return cell;
    }

    private PdfPCell createBorderlessCell(Phrase phrase, int alignment) {
        PdfPCell cell = new PdfPCell(phrase);
        configureBorderlessCell(cell, alignment);
        return cell;
    }

    private void configureBorderlessCell(PdfPCell cell, int alignment) {
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_TOP);
        cell.setPaddingBottom(5f);
        cell.setLeading(9f, 1.0f);
    }

    private String formatCurrency(BigDecimal amount) {
        return String.format(Locale.US, "%,.0f VND", MoneySupport.normalize(amount).doubleValue());
    }

    private String formatDateTime(Order order) {
        return order.getCreatedAt() != null ? order.getCreatedAt().format(RECEIPT_DATE_TIME_FORMATTER) : "-";
    }

    private String resolveSku(OrderItem item) {
        if (!isBlank(item.getSkuSnapshot())) {
            return item.getSkuSnapshot();
        }
        return item.getProduct() != null ? item.getProduct().getSku() : null;
    }

    private String resolveUnit(OrderItem item) {
        if (!isBlank(item.getUnitNameSnapshot())) {
            return item.getUnitNameSnapshot();
        }
        if (item.getProduct() != null && item.getProduct().getUnit() != null) {
            return item.getProduct().getUnit().getName();
        }
        return null;
    }

    private List<OrderItem> safeOrderItems(Order order) {
        return order.getOrderItems() != null ? order.getOrderItems() : List.of();
    }

    private int safeQuantity(OrderItem item) {
        return item.getQuantity() != null ? item.getQuantity() : 0;
    }

    private String safeOrderId(Order order) {
        return order.getId() != null ? String.valueOf(order.getId()) : "draft";
    }

    private String receiptFileName(Order order) {
        return receiptFileName(safeOrderId(order));
    }

    private String receiptFileName(String orderId) {
        return "Receipt_" + orderId + ".pdf";
    }

    private String formatEnumLabel(Enum<?> value) {
        if (value == null) {
            return "-";
        }
        if ("QR".equals(value.name())) {
            return "QR / VietQR";
        }
        String normalized = value.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder builder = new StringBuilder();
        for (String part : normalized.split("\\s+")) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.isEmpty() ? value.name() : builder.toString();
    }

    private static String normalizeBusinessName(String value) {
        return isBlank(value) ? DEFAULT_BUSINESS_NAME : value.trim();
    }

    private ReceiptSettings normalizeReceiptSettings(ReceiptSettings settings) {
        if (settings == null) {
            return new ReceiptSettings("", "", DEFAULT_FOOTER_NOTE, null);
        }
        return new ReceiptSettings(
            normalizePreferenceText(settings.storePhone()),
            normalizePreferenceText(settings.storeAddress()),
            isBlank(settings.footerNote()) ? DEFAULT_FOOTER_NOTE : settings.footerNote().trim(),
            normalizeLogoFile(settings.logoFile())
        );
    }

    private File normalizeLogoFile(File logoFile) {
        if (logoFile == null) {
            return null;
        }
        File normalized = logoFile.toPath().toAbsolutePath().normalize().toFile();
        if (!normalized.isFile()) {
            throw new ReceiptGenerationException("Receipt logo file does not exist.");
        }
        return normalized;
    }

    private File readConfiguredLogoFile() {
        String path = localPreferences.get(RECEIPT_LOGO_PATH_KEY, null);
        if (path == null || path.isBlank()) {
            return null;
        }
        return new File(path).toPath().toAbsolutePath().normalize().toFile();
    }

    private void putOrRemove(String key, String value) {
        if (isBlank(value)) {
            localPreferences.remove(key);
        } else {
            localPreferences.put(key, value);
        }
    }

    private String normalizePreferenceText(String value) {
        return isBlank(value) ? "" : value.trim();
    }

    private static String safeText(String value, String fallback) {
        return isBlank(value) ? fallback : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static class ReceiptGenerationException extends RuntimeException {
        public ReceiptGenerationException(String message) {
            super(message);
        }

        public ReceiptGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private record ReceiptFonts(Font title, Font header, Font normal, Font small, Font smallBold, Font tiny, Font total) {
        private static ReceiptFonts create() throws IOException, DocumentException {
            BaseFont regular = loadFont("/fonts/BeVietnamPro-Regular.ttf", BaseFont.HELVETICA);
            BaseFont bold = loadFont("/fonts/BeVietnamPro-Bold.ttf", BaseFont.HELVETICA_BOLD);
            return new ReceiptFonts(
                new Font(bold, 13, Font.NORMAL, BaseColor.BLACK),
                new Font(bold, 8.5f, Font.NORMAL, BaseColor.BLACK),
                new Font(regular, 8.2f, Font.NORMAL, BaseColor.BLACK),
                new Font(regular, 7.2f, Font.NORMAL, BaseColor.DARK_GRAY),
                new Font(bold, 7.2f, Font.NORMAL, BaseColor.BLACK),
                new Font(regular, 6.4f, Font.NORMAL, BaseColor.DARK_GRAY),
                new Font(bold, 9.5f, Font.NORMAL, BaseColor.BLACK)
            );
        }

        private static BaseFont loadFont(String resourcePath, String fallbackFont) throws IOException, DocumentException {
            URL fontUrl = ReceiptService.class.getResource(resourcePath);
            if (fontUrl == null) {
                return BaseFont.createFont(fallbackFont, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
            }
            return BaseFont.createFont(fontUrl.toExternalForm(), BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        }
    }
}
