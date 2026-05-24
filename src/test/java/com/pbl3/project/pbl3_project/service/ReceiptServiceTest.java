package com.pbl3.project.pbl3_project.service;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.entity.OrderItem;
import com.pbl3.project.pbl3_project.entity.OrderStatus;
import com.pbl3.project.pbl3_project.entity.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReceiptServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void generateReceiptCreatesStandardPosPdfWithOrderContent() throws Exception {
        Preferences testPreferences = new InMemoryPreferences();
        ReceiptService receiptService = new ReceiptService("Cửa hàng Ánh Sao", testPreferences);
        receiptService.saveReceiptOutputDirectory(tempDir.toFile());
        receiptService.saveReceiptSettings(new ReceiptService.ReceiptSettings(
            " 0909 123 456 ",
            " 123 Nguyễn Văn Linh, Đà Nẵng ",
            " Exchange is available within 7 days. ",
            null
        ));
        Order order = sampleOrder();

        File receiptFile = receiptService.generateReceipt(order);

        assertEquals(tempDir.toAbsolutePath().normalize(), receiptFile.toPath().getParent().toAbsolutePath().normalize());
        assertTrue(receiptService.previewReceiptFilePath(tempDir.toFile()).endsWith("Receipt_<order-id>.pdf"));
        assertTrue(receiptFile.exists());
        assertTrue(receiptFile.length() > 1_000);

        String pdfText = extractText(receiptFile);
        assertTrue(pdfText.contains("Cửa hàng Ánh Sao"));
        assertTrue(pdfText.contains("123 Nguyễn Văn Linh, Đà Nẵng"));
        assertTrue(pdfText.contains("0909 123 456"));
        assertTrue(pdfText.contains("POS RECEIPT"));
        assertTrue(pdfText.contains("Receipt #"));
        assertTrue(pdfText.contains("#42"));
        assertTrue(pdfText.contains("Khách hàng Ánh Dương"));
        assertTrue(pdfText.contains("Cà phê sữa đá"));
        assertTrue(pdfText.contains("Product discount"));
        assertTrue(pdfText.contains("Order Promo (May Promo)"));
        assertTrue(pdfText.contains("Amount Paid"));
        assertTrue(pdfText.contains("Net After Refunds"));
        assertTrue(pdfText.contains("QR / VietQR"));
        assertTrue(pdfText.contains("Exchange is available within 7 days."));
    }

    @Test
    void storedReceiptFallsBackToCurrentConfiguredFolderWhenRecordedPathIsUnavailable() throws Exception {
        Preferences testPreferences = new InMemoryPreferences();
        ReceiptService receiptService = new ReceiptService("PBL3 Store", testPreferences);
        receiptService.saveReceiptOutputDirectory(tempDir.toFile());
        Order order = sampleOrder();
        order.setReceiptFilePath(tempDir.resolve("other-machine").resolve("Receipt_42.pdf").toString());
        Files.write(tempDir.resolve("Receipt_42.pdf"), new byte[]{1});

        assertTrue(receiptService.hasStoredReceiptFile(order));
    }

    private Order sampleOrder() {
        Order order = new Order();
        order.setId(42L);
        order.setCreatedAt(LocalDateTime.of(2026, 5, 19, 10, 30, 45));
        order.setCreatedByNameSnapshot("Nguyễn Thị Mai");
        order.setCustomerNameSnapshot("Khách hàng Ánh Dương");
        order.setCustomerPhoneSnapshot("0909123456");
        order.setPaymentMethod(PaymentMethod.QR);
        order.setStatus(OrderStatus.PARTIALLY_RETURNED);
        order.setGrossSubtotal(new BigDecimal("1600000"));
        order.setDiscountTotal(new BigDecimal("200000"));
        order.setOrderLevelDiscountTotal(new BigDecimal("50000"));
        order.setAppliedOrderPromotionNameSnapshot("May Promo");
        order.setTotalPrice(new BigDecimal("1400000"));
        order.setRefundedAmount(new BigDecimal("200000"));

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProductNameSnapshot("Cà phê sữa đá");
        item.setSkuSnapshot("SKU-CF01");
        item.setUnitNameSnapshot("Bottle");
        item.setQuantity(2);
        item.setOriginalUnitPrice(new BigDecimal("800000"));
        item.setPrice(new BigDecimal("725000"));
        item.setLinePromotionDiscountAmount(new BigDecimal("150000"));
        item.setOrderLevelDiscountAllocatedAmount(new BigDecimal("50000"));
        item.setAppliedProductPromotionNameSnapshot("Product Promo");
        order.setOrderItems(List.of(item));
        return order;
    }

    private String extractText(File receiptFile) throws Exception {
        PdfReader reader = new PdfReader(receiptFile.getAbsolutePath());
        try {
            assertTrue(reader.getNumberOfPages() >= 1);
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(PdfTextExtractor.getTextFromPage(reader, page)).append('\n');
            }
            return text.toString();
        } finally {
            reader.close();
        }
    }

    private static final class InMemoryPreferences extends AbstractPreferences {
        private final Map<String, String> values = new HashMap<>();
        private final Map<String, InMemoryPreferences> children = new HashMap<>();

        private InMemoryPreferences() {
            super(null, "");
        }

        private InMemoryPreferences(AbstractPreferences parent, String name) {
            super(parent, name);
        }

        @Override
        protected void putSpi(String key, String value) {
            values.put(key, value);
        }

        @Override
        protected String getSpi(String key) {
            return values.get(key);
        }

        @Override
        protected void removeSpi(String key) {
            values.remove(key);
        }

        @Override
        protected void removeNodeSpi() {
            values.clear();
            children.clear();
        }

        @Override
        protected String[] keysSpi() {
            return values.keySet().toArray(String[]::new);
        }

        @Override
        protected String[] childrenNamesSpi() {
            return children.keySet().toArray(String[]::new);
        }

        @Override
        protected AbstractPreferences childSpi(String name) {
            return children.computeIfAbsent(name, childName -> new InMemoryPreferences(this, childName));
        }

        @Override
        protected void syncSpi() throws BackingStoreException {
        }

        @Override
        protected void flushSpi() throws BackingStoreException {
        }
    }
}
