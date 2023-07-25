package org.Roclh;

import com.image.charts.ImageCharts;
import org.json.JSONArray;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

public class Main {
    private final static String path = "data/dxtrade5.default.sfxprodapp1.RTTStatistics.json";
    private static final String names = "abcdefghijclmnopqrstuvwxyz";

    private static int chartNumber = 0;
    private static int maxAmount = 2000;

    public static void main(String[] args) {
        try {
            String content = new Scanner(new File(path)).useDelimiter("\\Z").next();
            content = "[" + content.replaceAll("\n", ",\n") + "]";
            JSONArray array = new JSONArray(content);
            Map<String, List<Double>> valuesPerSession = new HashMap<>();
            List<Double> values = new ArrayList<>();
            List<String> sessions = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                Double value = array.getJSONObject(i).getDouble("message");
                String session = array.getJSONObject(i).getJSONObject("contextMap").getString("mdc.session");
                if (valuesPerSession.containsKey(session)) {
                    valuesPerSession.get(session).add(value);
                } else {
                    valuesPerSession.put(session, new ArrayList<>());
                    valuesPerSession.get(session).add(value);
                }
                values.add(value);
                sessions.add(session);
            }
            System.out.println("Amount of statistics batches: " + values.size());
            System.out.println("Batch size: " + 10);
            System.out.println("Total statistics records: " + (values.size() * 10));
            System.out.println("Average value: " + values.stream().mapToDouble(a -> a).average().orElse(0d));
            System.out.println("Max value: " + values.stream().mapToDouble(a -> a).max().orElse(0d));
            System.out.println("Min value: " + values.stream().mapToDouble(a -> a).min().orElse(0d));
            System.out.println("Standart deviation: " + standartDeviation(values));
            System.out.println("Standart deviation of the mean: " + standartDeviationOfTheMean(values));
            System.out.println("Amount of sessions for these batches: " + sessions.stream().distinct().count());
            List<Double> avgValues = shorten(values, maxAmount);
            valuesPerSession.values().forEach(doubles -> shorten(doubles, maxAmount));
            ImageCharts imageCharts = createLineChart(avgValues);
            imageCharts.toFile("tmp/general-chart.png");
            valuesPerSession.forEach((key, value) -> {
                if (value.size() < 10) {
                    return;
                }
                ImageCharts chart = createLineChart(value);
                chart.chdl(key.substring(0, 5));
                try {
                    chart.toFile("tmp/" + key.substring(0, 5) + "(size-" + value.size() + ").png");
                } catch (IOException | InvalidKeyException | NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
            });

            List<Map.Entry<String, List<Double>>> result = valuesPerSession.entrySet().stream().filter((val) -> val.getValue().size() < 50).toList();
            result.forEach(val -> valuesPerSession.remove(val.getKey()));
            ImageCharts allCharts = createChartWithMultipleLines(
                    valuesPerSession.values().stream().map(value ->
                            value.stream()
                                    .mapToInt((Double::intValue))
                                    .sorted()
                                    .mapToObj(Objects::toString)
                                    .collect(Collectors.joining(","))
                    ).collect(Collectors.toList()),
                    valuesPerSession.keySet().stream().toList());
            //    allCharts.toFile("chart.png");
            ImageCharts bvgChart = createBVGChart(avgValues.stream().filter(value -> value < 500).toList(), 6);
            bvgChart.toFile("bvg-chart.png");
        } catch (NoSuchAlgorithmException | InvalidKeyException | IOException e) {
            throw new RuntimeException(e);
        }

        //
    }

    public static List<Double> shorten(List<Double> values, int maxAmount){
        List<Double> avgValues = new ArrayList<>(List.copyOf(values));
        while (avgValues.size() > maxAmount) {
            List<Double> tempArray = new ArrayList<>();
            for (int i = 0; i < avgValues.size(); i += 2) {
                tempArray.add((avgValues.get(i) + avgValues.get(i++)) / 2);
            }
            avgValues = new ArrayList<>(List.copyOf(tempArray));
        }
        return avgValues;
    }

    public static double standartDeviation(List<Double> values) {
        Double avg = values.stream().mapToDouble(a -> a).average().orElse(0d);
        return Math.sqrt((1d / (values.size() - 1d)) * values.stream().mapToDouble((val) -> Math.pow(val - avg, 2)).sum());
    }

    public static double standartDeviationOfTheMean(List<Double> values) {
        Double avg = values.stream().mapToDouble(a -> a).average().orElse(0d);
        return Math.sqrt((1d / (values.size() * (values.size() - 1d))) * values.stream().mapToDouble((val) -> Math.pow(val - avg, 2)).sum());
    }

    private static String appendSymbol(String val) {
        char symbol = names.charAt(chartNumber);
        chartNumber++;
        return symbol + ":" + val;
    }

    public static ImageCharts createLineChart(List<Double> values) {
        ImageCharts imageCharts = new ImageCharts();
        imageCharts.cht("lc");
        imageCharts.chs("999x999");
        String chd = "a:" + values.stream()
                .mapToInt((Double::intValue))
                .sorted()
                .mapToObj(Objects::toString)
                .collect(Collectors.joining(","));
        imageCharts.chd(chd);
        imageCharts.chco("fdb45c");
        imageCharts.chls("5, 6, 0");
        imageCharts.chxt("x,y");
        imageCharts.chg("20,50,5,5,CECECE");
        imageCharts.chdl("RTT Average");
        return imageCharts;
    }

    public static ImageCharts createLineChart(List<Double> values, IntPredicate filter) {
        ImageCharts imageCharts = new ImageCharts();
        imageCharts.cht("lc");
        imageCharts.chs("999x999");
        String chd = "a:" + values.stream()
                .mapToInt((Double::intValue))
                .sorted()
                .filter(filter)
                .mapToObj(Objects::toString)
                .collect(Collectors.joining(","));
        imageCharts.chd(chd);
        imageCharts.chco("fdb45c");
        imageCharts.chls("5, 6, 0");
        imageCharts.chxt("x,y");
        imageCharts.chg("20,50,5,5,CECECE");
        imageCharts.chdl("RTT Average");
        return imageCharts;
    }

    public static ImageCharts createChartWithMultipleLines(List<String> chds, List<String> chdl) {
        ImageCharts imageCharts = new ImageCharts();
        imageCharts.cht("lc");
        imageCharts.chs("999x999");
        imageCharts.chd("a:" + chds.stream().map(Objects::toString).collect(Collectors.joining("|")));
        List<String> styles = new ArrayList<>();
        for (int i = 0; i < chds.size(); i++) {
            styles.add("5, 6, 0");
        }
        imageCharts.chls(String.join("|", styles));
        imageCharts.chxt("x,y");
        imageCharts.chg("20,50,5,5,CECECE");
        return imageCharts;
    }

    public static ImageCharts createBVGChart(List<Double> values, Integer dividedBy) {
        ImageCharts imageCharts = new ImageCharts();
        List<Integer> limits = new ArrayList<>();
        for (int i = 0; i <= dividedBy; i++) {
            limits.add((values.stream().mapToInt(Double::intValue).max().orElse(0) / dividedBy) * i);
        }
        List<String> limitsLabels = new ArrayList<>();
        for (int i = 0; i < limits.size() - 1; i++) {
            limitsLabels.add("From " + limits.get(i) + " To " + limits.get(i + 1));
        }
        List<Double> averagesPerPart = new ArrayList<>();
        for (int i = 1; i <= dividedBy; i++) {
            double total = 0;
            for (Double value : values) {
                if (value >= limits.get(i - 1) && value <= limits.get(i)) {
                    total++;
                }
            }
            averagesPerPart.add(total);
        }
        imageCharts.cht("bvg");
        imageCharts.chbr("10");
        imageCharts.chs("999x999");
        imageCharts.chd("a:" + averagesPerPart.stream().map(Objects::toString).collect(Collectors.joining(",")));
        imageCharts.chxt("x,y");
        imageCharts.chxl("0:|" + String.join("|", limitsLabels));

        return imageCharts;
    }

}