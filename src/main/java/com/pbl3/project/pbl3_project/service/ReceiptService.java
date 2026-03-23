package com.pbl3.project.pbl3_project.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.pbl3.project.pbl3_project.entity.Order;
import com.pbl3.project.pbl3_project.entity.OrderItem;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class ReceiptService {

    // 80mm thermal paper is approx 226 points wide
    private static final Rectangle RECEIPT_SIZE = new Rectangle(226, 800); 

    public void generateAndOpenReceipt(Order order) {
        try {
            // Save to Desktop/PBL3_Invoices
            String userHome = System.getProperty("user.home");
            File invoiceDir = new File(userHome + "/Desktop", "PBL3_Invoices");
            if (!invoiceDir.exists()) invoiceDir.mkdirs();

            File pdfFile = new File(invoiceDir, "Receipt_" + order.getId() + ".pdf");
            
            Document document = new Document(RECEIPT_SIZE, 10, 10, 15, 15);
            PdfWriter.getInstance(document, new FileOutputStream(pdfFile));

            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

            // Store Title
            Paragraph title = new Paragraph("PBL3 STORE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("Thank you for your purchase!", smallFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);

            document.add(new Paragraph(" ")); // Spacer
            
            // Order Info
            document.add(new Paragraph("Receipt #: " + order.getId(), normalFont));
            document.add(new Paragraph("Date: " + order.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), normalFont));
            document.add(new Paragraph("Cashier: " + order.getUser().getFullName(), normalFont));
            
            // Separator
            document.add(new Chunk(new LineSeparator(0.5f, 100, BaseColor.BLACK, Element.ALIGN_CENTER, -2)));
            document.add(new Paragraph(" "));

            // Items Table
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4f, 1.5f, 2.5f, 2.5f}); // Item, Qty, Price, Total

            // Table Headers
            table.addCell(createBorderlessCell("Item", headerFont, Element.ALIGN_LEFT));
            table.addCell(createBorderlessCell("Qty", headerFont, Element.ALIGN_CENTER));
            table.addCell(createBorderlessCell("Price", headerFont, Element.ALIGN_RIGHT));
            table.addCell(createBorderlessCell("Wait", headerFont, Element.ALIGN_RIGHT)); // Wait, I need Total. Wait, "Total"
            table.addCell(createBorderlessCell("Total", headerFont, Element.ALIGN_RIGHT)); 
            // Wait, previous cell was added by mistake. I should correct it.

            // Resetting table to ensure clean state
            table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{4.5f, 1.2f, 2.2f, 2.5f});
            
            table.addCell(createBorderlessCell("Item", headerFont, Element.ALIGN_LEFT));
            table.addCell(createBorderlessCell("Qty", headerFont, Element.ALIGN_CENTER));
            table.addCell(createBorderlessCell("Price", headerFont, Element.ALIGN_RIGHT));
            table.addCell(createBorderlessCell("Total", headerFont, Element.ALIGN_RIGHT));

            for (OrderItem item : order.getOrderItems()) {
                table.addCell(createBorderlessCell(item.getProduct().getName(), normalFont, Element.ALIGN_LEFT));
                table.addCell(createBorderlessCell(String.valueOf(item.getQuantity()), normalFont, Element.ALIGN_CENTER));
                table.addCell(createBorderlessCell(formatCurrency(item.getPrice()), normalFont, Element.ALIGN_RIGHT));
                table.addCell(createBorderlessCell(formatCurrency(item.getPrice() * item.getQuantity()), normalFont, Element.ALIGN_RIGHT));
            }
            document.add(table);

            document.add(new Paragraph(" "));
            document.add(new Chunk(new LineSeparator(0.5f, 100, BaseColor.BLACK, Element.ALIGN_CENTER, -2)));
            document.add(new Paragraph(" "));

            // Totals
            PdfPTable totalTable = new PdfPTable(2);
            totalTable.setWidthPercentage(100);
            totalTable.setWidths(new float[]{7f, 3f});

            totalTable.addCell(createBorderlessCell("TOTAL DUE:", titleFont, Element.ALIGN_LEFT));
            totalTable.addCell(createBorderlessCell(formatCurrency(order.getTotalPrice()), titleFont, Element.ALIGN_RIGHT));

            totalTable.addCell(createBorderlessCell("Payment Method:", normalFont, Element.ALIGN_LEFT));
            totalTable.addCell(createBorderlessCell(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "CASH", normalFont, Element.ALIGN_RIGHT));

            document.add(totalTable);

            document.add(new Paragraph(" "));
            
            // Footer
            Paragraph footer = new Paragraph("Please keep receipt for returns.", smallFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();

            // Open the generated PDF automatically
            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
                java.awt.Desktop.getDesktop().open(pdfFile);
            }

        } catch (Exception e) {
            System.err.println("Error generating receipt: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private PdfPCell createBorderlessCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        cell.setPaddingBottom(5f);
        return cell;
    }

    private String formatCurrency(Double amount) {
        if (amount == null) return "0";
        return String.format("%,.0f", amount);
    }
}
