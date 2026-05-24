package com.pbl3.project.pbl3_project.ui.component;

public final class SidebarIconFactory {

    private static final javafx.scene.paint.Color DANGER_COLOR = javafx.scene.paint.Color.web("#ef4444");

    private SidebarIconFactory() {
    }

    public static javafx.scene.Node createDashboardNavIcon() {
        java.util.function.Function<javafx.scene.shape.Rectangle, javafx.scene.shape.Rectangle> rectFactory = rect -> {
            rect.setFill(javafx.scene.paint.Color.TRANSPARENT);
            rect.setArcWidth(2);
            rect.setArcHeight(2);
            rect.setSmooth(true);
            rect.getStyleClass().add("sidebar-nav-icon-stroke");
            return rect;
        };

        javafx.scene.layout.Pane iconPane = new javafx.scene.layout.Pane(
            rectFactory.apply(new javafx.scene.shape.Rectangle(3, 3, 7, 9)),
            rectFactory.apply(new javafx.scene.shape.Rectangle(14, 3, 7, 5)),
            rectFactory.apply(new javafx.scene.shape.Rectangle(14, 12, 7, 9)),
            rectFactory.apply(new javafx.scene.shape.Rectangle(3, 16, 7, 5))
        );
        iconPane.getStyleClass().add("dashboard-nav-icon");
        iconPane.setMinSize(24, 24);
        iconPane.setPrefSize(24, 24);
        iconPane.setMaxSize(24, 24);
        iconPane.setScaleX(0.82);
        iconPane.setScaleY(0.82);
        iconPane.setMouseTransparent(true);
        return iconPane;
    }

    public static javafx.scene.Node createSalesNavIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("sidebar-nav-icon-stroke");
            return path;
        };

        javafx.scene.layout.Pane storeIcon = new javafx.scene.layout.Pane(
            pathFactory.apply("m2 7 4.41-4.41A2 2 0 0 1 7.83 2h8.34a2 2 0 0 1 1.42.59L22 7"),
            pathFactory.apply("M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"),
            pathFactory.apply("M15 22v-4a2 2 0 0 0-2-2h-2a2 2 0 0 0-2 2v4"),
            pathFactory.apply("M2 7h20"),
            pathFactory.apply("M22 7v3a2 2 0 0 1-2 2a2.7 2.7 0 0 1-1.59-.63.7.7 0 0 0-.82 0A2.7 2.7 0 0 1 16 12a2.7 2.7 0 0 1-1.59-.63.7.7 0 0 0-.82 0A2.7 2.7 0 0 1 12 12a2.7 2.7 0 0 1-1.59-.63.7.7 0 0 0-.82 0A2.7 2.7 0 0 1 8 12a2.7 2.7 0 0 1-1.59-.63.7.7 0 0 0-.82 0A2.7 2.7 0 0 1 4 12a2 2 0 0 1-2-2V7")
        );
        storeIcon.setMinSize(24, 24);
        storeIcon.setPrefSize(24, 24);
        storeIcon.setMaxSize(24, 24);
        storeIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(storeIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.8);
        iconWrap.setScaleY(0.8);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    public static javafx.scene.Node createPromotionsNavIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("sidebar-nav-icon-stroke");
            return path;
        };

        javafx.scene.layout.Pane promotionsIcon = new javafx.scene.layout.Pane(
            pathFactory.apply("M20.59 13.41 11 3.83a2 2 0 0 0-2.82 0L3.41 8.59a2 2 0 0 0 0 2.82l9.59 9.59a2 2 0 0 0 2.82 0l4.77-4.77a2 2 0 0 0 0-2.82Z"),
            pathFactory.apply("M7 7h.01"),
            pathFactory.apply("M10 14 14 10"),
            pathFactory.apply("M8.5 11.5 12.5 15.5")
        );
        promotionsIcon.setMinSize(24, 24);
        promotionsIcon.setPrefSize(24, 24);
        promotionsIcon.setMaxSize(24, 24);
        promotionsIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(promotionsIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.82);
        iconWrap.setScaleY(0.82);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    public static javafx.scene.Node createProductsNavIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("sidebar-nav-icon-stroke");
            return path;
        };

        javafx.scene.layout.Pane packageIcon = new javafx.scene.layout.Pane(
            pathFactory.apply("M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z"),
            pathFactory.apply("M16.5 9.4 7.55 4.24"),
            pathFactory.apply("m3.3 7 8.7 5 8.7-5"),
            pathFactory.apply("M12 22V12")
        );
        packageIcon.setMinSize(24, 24);
        packageIcon.setPrefSize(24, 24);
        packageIcon.setMaxSize(24, 24);
        packageIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(packageIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.8);
        iconWrap.setScaleY(0.8);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    public static javafx.scene.Node createImportNavIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("sidebar-nav-icon-stroke");
            return path;
        };

        javafx.scene.shape.Circle rearWheel = new javafx.scene.shape.Circle(7, 18, 2);
        rearWheel.setFill(javafx.scene.paint.Color.TRANSPARENT);
        rearWheel.setSmooth(true);
        rearWheel.getStyleClass().add("sidebar-nav-icon-stroke");

        javafx.scene.shape.Circle frontWheel = new javafx.scene.shape.Circle(17, 18, 2);
        frontWheel.setFill(javafx.scene.paint.Color.TRANSPARENT);
        frontWheel.setSmooth(true);
        frontWheel.getStyleClass().add("sidebar-nav-icon-stroke");

        javafx.scene.layout.Pane truckIcon = new javafx.scene.layout.Pane(
            pathFactory.apply("M14 18V6a2 2 0 0 0-2-2H4a2 2 0 0 0-2 2v11a1 1 0 0 0 1 1h2"),
            pathFactory.apply("M15 18H9"),
            pathFactory.apply("M19 18h2a1 1 0 0 0 1-1v-3.65a1 1 0 0 0-.22-.624l-3.48-4.35A1 1 0 0 0 17.52 8H14"),
            rearWheel,
            frontWheel
        );
        truckIcon.setMinSize(24, 24);
        truckIcon.setPrefSize(24, 24);
        truckIcon.setMaxSize(24, 24);
        truckIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(truckIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.8);
        iconWrap.setScaleY(0.8);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    public static javafx.scene.Node createOrderHistoryNavIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("sidebar-nav-icon-stroke");
            return path;
        };

        javafx.scene.layout.Pane historyIcon = new javafx.scene.layout.Pane(
            pathFactory.apply("M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"),
            pathFactory.apply("M3 3v5h5"),
            pathFactory.apply("M12 7v5l4 2")
        );
        historyIcon.setMinSize(24, 24);
        historyIcon.setPrefSize(24, 24);
        historyIcon.setMaxSize(24, 24);
        historyIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(historyIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.8);
        iconWrap.setScaleY(0.8);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    public static javafx.scene.Node createReturnsNavIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("sidebar-nav-icon-stroke");
            return path;
        };

        javafx.scene.layout.Pane returnsIcon = new javafx.scene.layout.Pane(
            pathFactory.apply("M4 6v5h5"),
            pathFactory.apply("m4 11 5-5 5 5"),
            pathFactory.apply("M20 18v-5h-5"),
            pathFactory.apply("m20 13-5 5-5-5")
        );
        returnsIcon.setMinSize(24, 24);
        returnsIcon.setPrefSize(24, 24);
        returnsIcon.setMaxSize(24, 24);
        returnsIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(returnsIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.82);
        iconWrap.setScaleY(0.82);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    public static javafx.scene.Node createExpensesNavIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("sidebar-nav-icon-stroke");
            return path;
        };

        javafx.scene.layout.Pane expensesIcon = new javafx.scene.layout.Pane(
            pathFactory.apply("M6 3h9l4 4v14a1 1 0 0 1-1.4.91L15 20l-2 2-2-2-2 2-2-2-2 2A1 1 0 0 1 4 21V5a2 2 0 0 1 2-2"),
            pathFactory.apply("M9 9h5"),
            pathFactory.apply("M9 13h6"),
            pathFactory.apply("M9 17h4")
        );
        expensesIcon.setMinSize(24, 24);
        expensesIcon.setPrefSize(24, 24);
        expensesIcon.setMaxSize(24, 24);
        expensesIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(expensesIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.82);
        iconWrap.setScaleY(0.82);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    public static javafx.scene.Node createCustomersNavIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("sidebar-nav-icon-stroke");
            return path;
        };

        javafx.scene.shape.Circle primaryHead = new javafx.scene.shape.Circle(9, 7, 4);
        primaryHead.setFill(javafx.scene.paint.Color.TRANSPARENT);
        primaryHead.setSmooth(true);
        primaryHead.getStyleClass().add("sidebar-nav-icon-stroke");

        javafx.scene.layout.Pane customersIcon = new javafx.scene.layout.Pane(
            primaryHead,
            pathFactory.apply("M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"),
            pathFactory.apply("M22 21v-2a4 4 0 0 0-3-3.87"),
            pathFactory.apply("M16 3.13a4 4 0 0 1 0 7.75")
        );
        customersIcon.setMinSize(24, 24);
        customersIcon.setPrefSize(24, 24);
        customersIcon.setMaxSize(24, 24);
        customersIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(customersIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.8);
        iconWrap.setScaleY(0.8);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    public static javafx.scene.Node createReportsNavIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("sidebar-nav-icon-stroke");
            return path;
        };

        javafx.scene.layout.Pane reportsIcon = new javafx.scene.layout.Pane(
            pathFactory.apply("M3 3v16a2 2 0 0 0 2 2h16"),
            pathFactory.apply("M18 17V9"),
            pathFactory.apply("M13 17V5"),
            pathFactory.apply("M8 17v-3")
        );
        reportsIcon.setMinSize(24, 24);
        reportsIcon.setPrefSize(24, 24);
        reportsIcon.setMaxSize(24, 24);
        reportsIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(reportsIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.8);
        iconWrap.setScaleY(0.8);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    public static javafx.scene.Node createStocktakeNavIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("sidebar-nav-icon-stroke");
            return path;
        };

        javafx.scene.layout.Pane stocktakeIcon = new javafx.scene.layout.Pane(
            pathFactory.apply("M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2"),
            pathFactory.apply("M9 14l2 2 4-4")
        );

        javafx.scene.shape.Rectangle clipTop = new javafx.scene.shape.Rectangle(8, 2, 8, 4);
        clipTop.setArcWidth(2);
        clipTop.setArcHeight(2);
        clipTop.setFill(javafx.scene.paint.Color.TRANSPARENT);
        clipTop.setSmooth(true);
        clipTop.getStyleClass().add("sidebar-nav-icon-stroke");
        stocktakeIcon.getChildren().add(clipTop);

        stocktakeIcon.setMinSize(24, 24);
        stocktakeIcon.setPrefSize(24, 24);
        stocktakeIcon.setMaxSize(24, 24);
        stocktakeIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(stocktakeIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.8);
        iconWrap.setScaleY(0.8);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    public static javafx.scene.Node createLogoutNavIcon() {
        java.util.function.Consumer<javafx.scene.shape.Shape> applyDangerStroke = shape -> {
            shape.setFill(javafx.scene.paint.Color.TRANSPARENT);
            shape.setStroke(DANGER_COLOR);
            shape.setStrokeWidth(1.5);
            shape.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            shape.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            shape.setSmooth(true);
        };

        javafx.scene.shape.SVGPath doorPath = new javafx.scene.shape.SVGPath();
        doorPath.setContent("M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4");
        applyDangerStroke.accept(doorPath);

        javafx.scene.shape.Polyline arrow = new javafx.scene.shape.Polyline(16, 17, 21, 12, 16, 7);
        applyDangerStroke.accept(arrow);

        javafx.scene.shape.Line shaft = new javafx.scene.shape.Line(21, 12, 9, 12);
        applyDangerStroke.accept(shaft);

        javafx.scene.layout.Pane logoutIcon = new javafx.scene.layout.Pane(doorPath, arrow, shaft);
        logoutIcon.setMinSize(24, 24);
        logoutIcon.setPrefSize(24, 24);
        logoutIcon.setMaxSize(24, 24);
        logoutIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(logoutIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.8);
        iconWrap.setScaleY(0.8);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    public static javafx.scene.Node createAuditLogNavIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("sidebar-nav-icon-stroke");
            return path;
        };

        javafx.scene.shape.Circle searchCircle = new javafx.scene.shape.Circle(5, 14, 3);
        searchCircle.setFill(javafx.scene.paint.Color.TRANSPARENT);
        searchCircle.setSmooth(true);
        searchCircle.getStyleClass().add("sidebar-nav-icon-stroke");

        javafx.scene.layout.Pane auditLogIcon = new javafx.scene.layout.Pane(
            pathFactory.apply("M14 2v4a2 2 0 0 0 2 2h4"),
            pathFactory.apply("M4.268 21a2 2 0 0 0 1.727 1H18a2 2 0 0 0 2-2V7l-5-5H6a2 2 0 0 0-2 2v3"),
            pathFactory.apply("m9 18-1.5-1.5"),
            searchCircle
        );
        auditLogIcon.setMinSize(24, 24);
        auditLogIcon.setPrefSize(24, 24);
        auditLogIcon.setMaxSize(24, 24);
        auditLogIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(auditLogIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.8);
        iconWrap.setScaleY(0.8);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    public static javafx.scene.Node createMasterDataNavIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("sidebar-nav-icon-stroke");
            return path;
        };

        javafx.scene.shape.Ellipse topEllipse = new javafx.scene.shape.Ellipse(12, 5, 9, 3);
        topEllipse.setFill(javafx.scene.paint.Color.TRANSPARENT);
        topEllipse.setSmooth(true);
        topEllipse.getStyleClass().add("sidebar-nav-icon-stroke");

        javafx.scene.layout.Pane databaseIcon = new javafx.scene.layout.Pane(
            topEllipse,
            pathFactory.apply("M3 5V19A9 3 0 0 0 21 19V5"),
            pathFactory.apply("M3 12A9 3 0 0 0 21 12")
        );
        databaseIcon.setMinSize(24, 24);
        databaseIcon.setPrefSize(24, 24);
        databaseIcon.setMaxSize(24, 24);
        databaseIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(databaseIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.8);
        iconWrap.setScaleY(0.8);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    public static javafx.scene.Node createMyAccountHeaderIcon() {
        javafx.scene.shape.Circle outerCircle = new javafx.scene.shape.Circle(12, 12, 10);
        outerCircle.setFill(javafx.scene.paint.Color.TRANSPARENT);
        outerCircle.setSmooth(true);
        outerCircle.getStyleClass().add("header-account-icon-stroke");

        javafx.scene.shape.Circle headCircle = new javafx.scene.shape.Circle(12, 10, 3);
        headCircle.setFill(javafx.scene.paint.Color.TRANSPARENT);
        headCircle.setSmooth(true);
        headCircle.getStyleClass().add("header-account-icon-stroke");

        javafx.scene.shape.SVGPath shouldersPath = new javafx.scene.shape.SVGPath();
        shouldersPath.setContent("M7 20.662V19a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v1.662");
        shouldersPath.setFill(javafx.scene.paint.Color.TRANSPARENT);
        shouldersPath.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        shouldersPath.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        shouldersPath.setSmooth(true);
        shouldersPath.getStyleClass().add("header-account-icon-stroke");

        javafx.scene.layout.Pane accountIcon = new javafx.scene.layout.Pane(outerCircle, headCircle, shouldersPath);
        accountIcon.setMinSize(24, 24);
        accountIcon.setPrefSize(24, 24);
        accountIcon.setMaxSize(24, 24);
        accountIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(accountIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(1.16);
        iconWrap.setScaleY(1.16);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    public static javafx.scene.Node createSettingsNavIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("sidebar-nav-icon-stroke");
            return path;
        };

        javafx.scene.shape.Circle gearCore = new javafx.scene.shape.Circle(12, 12, 3);
        gearCore.setFill(javafx.scene.paint.Color.TRANSPARENT);
        gearCore.setSmooth(true);
        gearCore.getStyleClass().add("sidebar-nav-icon-stroke");

        javafx.scene.layout.Pane settingsIcon = new javafx.scene.layout.Pane(
            pathFactory.apply("M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09a1.65 1.65 0 0 0-1-1.51 1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09a1.65 1.65 0 0 0 1.51-1 1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33 1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82 1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1"),
            gearCore
        );
        settingsIcon.setMinSize(24, 24);
        settingsIcon.setPrefSize(24, 24);
        settingsIcon.setMaxSize(24, 24);
        settingsIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(settingsIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.8);
        iconWrap.setScaleY(0.8);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

    public static javafx.scene.Node createAccountsNavIcon() {
        java.util.function.Function<String, javafx.scene.shape.SVGPath> pathFactory = content -> {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(content);
            path.setFill(javafx.scene.paint.Color.TRANSPARENT);
            path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
            path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
            path.setSmooth(true);
            path.getStyleClass().add("sidebar-nav-icon-stroke");
            return path;
        };

        javafx.scene.shape.Circle gearCore = new javafx.scene.shape.Circle(18, 15, 3);
        gearCore.setFill(javafx.scene.paint.Color.TRANSPARENT);
        gearCore.setSmooth(true);
        gearCore.getStyleClass().add("sidebar-nav-icon-stroke");

        javafx.scene.shape.Circle userHead = new javafx.scene.shape.Circle(9, 7, 4);
        userHead.setFill(javafx.scene.paint.Color.TRANSPARENT);
        userHead.setSmooth(true);
        userHead.getStyleClass().add("sidebar-nav-icon-stroke");

        javafx.scene.layout.Pane accountsIcon = new javafx.scene.layout.Pane(
            gearCore,
            userHead,
            pathFactory.apply("M10 15H6a4 4 0 0 0-4 4v2"),
            pathFactory.apply("m21.7 16.4-.9-.3"),
            pathFactory.apply("m15.2 13.9-.9-.3"),
            pathFactory.apply("m16.6 18.7.3-.9"),
            pathFactory.apply("m19.1 12.2.3-.9"),
            pathFactory.apply("m19.6 18.7-.4-1"),
            pathFactory.apply("m16.8 12.3-.4-1"),
            pathFactory.apply("m14.3 16.6 1-.4"),
            pathFactory.apply("m20.7 13.8 1-.4")
        );
        accountsIcon.setMinSize(24, 24);
        accountsIcon.setPrefSize(24, 24);
        accountsIcon.setMaxSize(24, 24);
        accountsIcon.setMouseTransparent(true);

        javafx.scene.layout.StackPane iconWrap = new javafx.scene.layout.StackPane(accountsIcon);
        iconWrap.setMinSize(24, 24);
        iconWrap.setPrefSize(24, 24);
        iconWrap.setMaxSize(24, 24);
        iconWrap.setScaleX(0.8);
        iconWrap.setScaleY(0.8);
        iconWrap.setMouseTransparent(true);
        return iconWrap;
    }

}
