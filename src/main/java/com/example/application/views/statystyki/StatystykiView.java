package com.example.application.views.statystyki;

import com.example.application.data.entity.StatusKsiazki;
import com.example.application.data.service.BookService;
import com.example.application.data.service.RentalService;
import com.example.application.data.service.UserService;
import com.example.application.views.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import com.github.appreciated.apexcharts.ApexCharts;
import com.github.appreciated.apexcharts.ApexChartsBuilder;
import com.github.appreciated.apexcharts.config.YAxis;
import com.github.appreciated.apexcharts.config.yaxis.Labels;
import com.github.appreciated.apexcharts.config.builder.ChartBuilder;
import com.github.appreciated.apexcharts.config.builder.DataLabelsBuilder;
import com.github.appreciated.apexcharts.config.builder.LegendBuilder;
import com.github.appreciated.apexcharts.config.builder.PlotOptionsBuilder;
import com.github.appreciated.apexcharts.config.builder.XAxisBuilder;
import com.github.appreciated.apexcharts.config.chart.Type;
import com.github.appreciated.apexcharts.config.legend.Position;
import com.github.appreciated.apexcharts.config.plotoptions.builder.BarBuilder;
import com.github.appreciated.apexcharts.helper.Series;

import java.time.LocalDate;

/**
 * Widok ze statystykami wypożyczalni dostępny dla Kierownika.
 * Agreguje dane z różnych serwisów (książki, użytkownicy, wypożyczenia) w jednym miejscu.
 */
@RolesAllowed("KIEROWNIK")
@Route(value = "statystyki", layout = MainLayout.class)
@PageTitle("Statystyki | Wypożyczalnia książek")
public class StatystykiView extends VerticalLayout {

    private final BookService bookService;
    private final UserService userService;
    private final RentalService rentalService;

    private final DatePicker startDate = new DatePicker("Data od");
    private final DatePicker endDate = new DatePicker("Data do");

    private final Span totalUsersCount = new Span("0");
    private final Span totalEmployeesCount = new Span("0");
    private final Span totalBooksCount = new Span("0");

    private final Span periodRentalsCount = new Span("0");
    private final Span periodReturnsCount = new Span("0");
    private final Span activeUsersCount = new Span("0");

    private final VerticalLayout statusChartContainer = new VerticalLayout();
    private final VerticalLayout activityChartContainer = new VerticalLayout();

    public StatystykiView(BookService bookService, UserService userService, RentalService rentalService) {
        this.bookService = bookService;
        this.userService = userService;
        this.rentalService = rentalService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        addClassName("statystyki-view");

        add(new H2("Statystyki wypożyczalni"));

        add(new H4("Statystyki globalne"));
        HorizontalLayout globalCards = new HorizontalLayout();
        globalCards.setWidthFull();
        globalCards.add(
                createCard("Liczba użytkowników", totalUsersCount, VaadinIcon.USERS, "#6200EA"),
                createCard("Liczba pracowników", totalEmployeesCount, VaadinIcon.USER_STAR, "#009688"),
                createCard("Liczba książek", totalBooksCount, VaadinIcon.BOOK, "#2962FF")
        );
        add(globalCards);

        startDate.setValue(LocalDate.now().minusDays(30));
        endDate.setValue(LocalDate.now());

        Button refreshBtn = new Button("Odśwież", VaadinIcon.REFRESH.create());
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        refreshBtn.addClickListener(e -> updateStats());

        H4 periodHeader = new H4("Statystyki czasowe");
        periodHeader.getStyle().set("margin-top", "30px");

        HorizontalLayout toolbar = new HorizontalLayout(startDate, endDate, refreshBtn);
        toolbar.setAlignItems(FlexComponent.Alignment.BASELINE);

        add(periodHeader, toolbar);

        HorizontalLayout periodCards = new HorizontalLayout();
        periodCards.setWidthFull();
        periodCards.add(
                createCard("Wypożyczenia w wybranym okresie", periodRentalsCount, VaadinIcon.ARROW_CIRCLE_UP, "blue"),
                createCard("Zwroty w wybranym okresie", periodReturnsCount, VaadinIcon.ARROW_CIRCLE_DOWN, "green"),
                createCard("Aktywni użytkownicy w wybranym okresie", activeUsersCount, VaadinIcon.GROUP, "orange")
        );
        add(periodCards);

        HorizontalLayout chartsLayout = new HorizontalLayout();
        chartsLayout.setWidthFull();
        chartsLayout.setSpacing(true);
        chartsLayout.getStyle().set("margin-top", "20px");

        statusChartContainer.setWidth("50%");
        styleContainer(statusChartContainer);

        activityChartContainer.setWidth("50%");
        styleContainer(activityChartContainer);

        chartsLayout.add(statusChartContainer, activityChartContainer);
        add(chartsLayout);

        updateStats();
    }

    private void updateStats() {
        LocalDate start = startDate.getValue();
        LocalDate end = endDate.getValue();

        if (start == null || end == null || start.isAfter(end)) {
            Notification.show("Nieprawidłowy zakres dat");
            return;
        }

        long usersTotal = userService.countAllUsers();
        long employeesTotal = userService.countAllEmployees();
        long booksTotal = bookService.countAllBooks();

        long wypozyczenia = rentalService.countWypozyczeniaWOkresie(start, end);
        long zwroty = rentalService.countZwrotyWOkresie(start, end);
        long activeUsers = rentalService.countActiveUsersInPeriod(start, end);

        totalUsersCount.setText(String.valueOf(usersTotal));
        totalEmployeesCount.setText(String.valueOf(employeesTotal));
        totalBooksCount.setText(String.valueOf(booksTotal));

        periodRentalsCount.setText(String.valueOf(wypozyczenia));
        periodReturnsCount.setText(String.valueOf(zwroty));
        activeUsersCount.setText(String.valueOf(activeUsers));

        statusChartContainer.removeAll();
        statusChartContainer.add(new H4("Struktura zbioru książek"));

        long sDostepna = bookService.countKsiazkiByStatus(StatusKsiazki.DOSTEPNA);
        long sWypozyczona = bookService.countKsiazkiByStatus(StatusKsiazki.WYPOZYCZONA);
        long sZarezerwowana = bookService.countKsiazkiByStatus(StatusKsiazki.ZAREZERWOWANA);
        long sDoOdlozenia = bookService.countKsiazkiByStatus(StatusKsiazki.DO_ODLOZENIA);
        long sWKontroli = bookService.countKsiazkiByStatus(StatusKsiazki.W_KONTROLI);
        long sWRenowacji = bookService.countKsiazkiByStatus(StatusKsiazki.W_RENOWACJI);
        long sWycofana = bookService.countKsiazkiByStatus(StatusKsiazki.WYCOFANA);

        if (booksTotal > 0) {
            ApexCharts bookStatusChart = new ApexChartsBuilder()
                    .withChart(ChartBuilder.get()
                            .withType(Type.DONUT)
                            .withBackground("transparent")
                            .withDropShadow(com.github.appreciated.apexcharts.config.chart.builder.DropShadowBuilder.get()
                                    .withEnabled(true)
                                    .withTop(4.0)
                                    .withLeft(0.0)
                                    .withBlur(5.0)
                                    .withOpacity(0.15)
                                    .build())
                            .build())
                    .withLabels("Dostępne", "Wypożyczone", "Zarezerw.", "Do odłożenia", "W kontroli", "W renowacji", "Wycofane")
                    .withSeries((double)sDostepna, (double)sWypozyczona, (double)sZarezerwowana, (double)sDoOdlozenia, (double)sWKontroli, (double)sWRenowacji, (double)sWycofana)
                    .withLegend(LegendBuilder.get().withPosition(Position.BOTTOM).build())
                    .withColors("#00E396", "#FF4560", "#008FFB", "#775DD0", "#FEB019", "#FF9800", "#A3A4A8")
                    .withStroke(com.github.appreciated.apexcharts.config.builder.StrokeBuilder.get()
                            .withShow(true)
                            .withWidth(3.0)
                            .withColors("transparent")
                            .build())
                    .build();
            bookStatusChart.setWidth("100%");
            bookStatusChart.setHeight("350px");
            bookStatusChart.getStyle().set("margin-top", "15px");
            statusChartContainer.add(bookStatusChart);
        } else {
            statusChartContainer.add(new Span("Brak książek w systemie."));
        }

        activityChartContainer.removeAll();
        activityChartContainer.add(new H4("Wypożyczenia i zwroty"));

        ApexCharts barChart = new ApexChartsBuilder()
                .withChart(ChartBuilder.get()
                        .withType(Type.BAR)
                        .withBackground("transparent")
                        .withToolbar(com.github.appreciated.apexcharts.config.chart.builder.ToolbarBuilder.get().withShow(false).build())
                        .build())
                .withPlotOptions(PlotOptionsBuilder.get()
                        .withBar(BarBuilder.get()
                                .withHorizontal(false)
                                .withColumnWidth("30%")
                                .withDistributed(true)
                                .build())
                        .build())
                .withDataLabels(DataLabelsBuilder.get()
                        .withEnabled(true)
                        .withOffsetY(-25.0)
                        .build())
                .withSeries(new Series<Double>("Ilość", (double)wypozyczenia, (double)zwroty))
                .withXaxis(XAxisBuilder.get().withCategories("Wypożyczenia", "Zwroty").build())
                .withFill(com.github.appreciated.apexcharts.config.builder.FillBuilder.get()
                        .withType("gradient")
                        .withGradient(com.github.appreciated.apexcharts.config.fill.builder.GradientBuilder.get()
                                .withShade("light")
                                .withType("vertical")
                                .withOpacityFrom(0.9)
                                .withOpacityTo(0.6)
                                .withStops(0.0, 100.0)
                                .build())
                        .build())
                .withColors("#008FFB", "#00E396")
                .withLegend(LegendBuilder.get().withShow(false).build())
                .build();

        YAxis yAxis = new YAxis();
        yAxis.setDecimalsInFloat(0.0);
        Labels yLabels = new Labels();
        yLabels.setFormatter("function(val) { return Math.round(val).toString(); }");
        yAxis.setLabels(yLabels);
        barChart.setYaxis(new YAxis[]{yAxis});

        barChart.setWidth("100%");
        barChart.setHeight("350px");
        barChart.getStyle().set("margin-top", "15px");

        activityChartContainer.add(barChart);
    }

    private void styleContainer(VerticalLayout layout) {
        layout.getStyle().set("background-color", "var(--lumo-surface-color)")
                .set("padding", "20px")
                .set("border-radius", "10px")
                .set("box-shadow", "0 2px 5px rgba(0,0,0,0.1)");
    }

    private VerticalLayout createCard(String title, Span countSpan, VaadinIcon icon, String colorName) {
        VerticalLayout card = new VerticalLayout();
        card.addClassName("card");
        card.setSpacing(false);
        card.setPadding(true);
        card.setWidth("30%");

        card.getStyle().set("background-color", "var(--lumo-surface-color)");
        card.getStyle().set("box-shadow", "0 4px 6px rgba(0,0,0,0.1)");
        card.getStyle().set("border-radius", "12px");
        card.getStyle().set("border-left", "5px solid " + colorName);

        Span iconSpan = new Span(icon.create());
        iconSpan.getStyle().set("color", colorName).set("font-size", "1.5rem");

        Span titleSpan = new Span(title);
        titleSpan.getStyle().set("font-size", "0.9rem").set("color", "var(--lumo-secondary-text-color)");

        countSpan.getStyle().set("font-size", "2.5rem").set("font-weight", "bold").set("color", "var(--lumo-body-text-color)");

        HorizontalLayout header = new HorizontalLayout(iconSpan, titleSpan);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        card.add(header, countSpan);
        return card;
    }
}