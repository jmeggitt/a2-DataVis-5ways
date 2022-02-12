import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public class Main {

    static Map<String, List<String>> readCsv(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));

        Function<String, String> clean =  x -> x.matches("^\"[^\"]*\"$") ? x.substring(1, x.length() - 1) : x;

        String[] header = reader.readLine().split(",");
        List<String> columnNames = Arrays.stream(header).map(x -> clean.apply(x.strip())).toList();

        List<List<String>> columns = new ArrayList<>(header.length);
        columnNames.forEach(x -> columns.add(new ArrayList<>()));

        String line;
        while ((line = reader.readLine()) != null) {
            String[] values = line.split(",");

            IntStream.range(0, values.length)
                    .forEach(x -> columns.get(x).add(clean.apply(values[x].strip())));
        }

        Map<String, List<String>> namedColumns = new HashMap<>();
        IntStream.range(0, columns.size())
                .forEach(x -> namedColumns.put(columnNames.get(x), columns.get(x)));
        return namedColumns;
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("awt.useSystemAAFontSettings","on");
        File dataFile = new File("../cars-sample.csv");

        var csv = readCsv(dataFile);

        Function<String, Double> parseDouble = x -> !x.equals("NA")? Double.parseDouble(x) : null;
        List<Double> weight = csv.get("Weight").stream().map(parseDouble).toList();
        List<Double> mpg = csv.get("MPG").stream().map(parseDouble).toList();

        Color[] colors = {Color.MAGENTA, Color.BLUE, Color.ORANGE, Color.GREEN, Color.RED};
        var manufacturers = csv.get("Manufacturer").stream().distinct().toList();

        Map<String, Color> colorMap = new HashMap<>();
        IntStream.range(0, manufacturers.size()).forEach(x -> colorMap.put(manufacturers.get(x), colors[x]));


        int multiSampling = 10;

        BufferedImage image = new BufferedImage(500 * multiSampling, 500 * multiSampling, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();

        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());


        double xMin = weight.stream().filter(Objects::nonNull).min(Double::compare).get();
        double xMax = weight.stream().filter(Objects::nonNull).max(Double::compare).get();
        double yMin = mpg.stream().filter(Objects::nonNull).min(Double::compare).get();
        double yMax = mpg.stream().filter(Objects::nonNull).max(Double::compare).get();

        Axis xAxis = new Axis(xMin, xMax, 50 * multiSampling, image.getWidth() - 50 * multiSampling);
        Axis yAxis = new Axis(yMin, yMax, 50 * multiSampling, image.getHeight() - 50 * multiSampling);

        graphics.setColor(Color.BLACK);
        graphics.setStroke(new BasicStroke(2 * multiSampling));
        graphics.drawLine((int) xAxis.map(xMin), image.getHeight() - (int) yAxis.map(yMin), (int) xAxis.map(xMax),image.getHeight() -  (int) yAxis.map(yMin));
        graphics.drawLine((int) xAxis.map(xMin), image.getHeight () - (int) yAxis.map(yMin), (int) xAxis.map(xMin), image.getHeight() - (int) yAxis.map(yMax));

        // Add ticks
        graphics.setFont(graphics.getFont().deriveFont(15f * multiSampling));
        Arrays.stream(new int[]{2000, 3000, 4000, 5000}).forEach(x -> {
            int newX = (int)xAxis.map(x);
            graphics.drawLine(newX, image.getHeight() - (int) yAxis.map(yMin), newX, image.getHeight() - ((int) yAxis.map(yMin) - 5 * multiSampling));

            String label = Integer.toString(x);
            Rectangle2D bounds = graphics.getFontMetrics().getStringBounds(label, graphics);
            graphics.drawString(label, newX - (int) bounds.getWidth() / 2, image.getHeight() - ((int) yAxis.map(yMin) - 10 * multiSampling - (int) bounds.getHeight()));
        });

        Arrays.stream(new int[]{10, 20, 30, 40}).forEach(y -> {
            int newY = (int)yAxis.map(y);
            graphics.drawLine((int)xAxis.map(xMin), image.getHeight() - newY, (int) xAxis.map(xMin) - 5 * multiSampling, image.getHeight() - newY);

            String label = Integer.toString(y);
            Rectangle2D bounds = graphics.getFontMetrics().getStringBounds(label, graphics);
            graphics.drawString(label, (int)xAxis.map(xMin) - (int) bounds.getWidth() - 10 * multiSampling, image.getHeight() - (newY - (int) bounds.getHeight() / 2));
        });

        AffineTransform globalTransform = new AffineTransform();
        globalTransform.translate(0.0, image.getHeight());
        globalTransform.scale(1.0, -1.0);
        graphics.transform(globalTransform);


        Ellipse2D.Double circle = new Ellipse2D.Double(-0.5, -0.5, 1.0, 1.0);
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));

        for (int idx = 0; idx < weight.size(); idx++) {
            if (weight.get(idx) == null || mpg.get(idx) == null) continue;

            graphics.setColor(colorMap.get(csv.get("Manufacturer").get(idx)));

            double scaling = weight.get(idx) / 150.0;

            AffineTransform transform = new AffineTransform();
            transform.translate(xAxis.map(weight.get(idx)), yAxis.map(mpg.get(idx)));
            transform.scale(scaling * multiSampling, scaling * multiSampling);

            graphics.fill(transform.createTransformedShape(circle));
        }

        BufferedImage shrunk = new BufferedImage(image.getWidth() / multiSampling, image.getHeight() / multiSampling, image.getType());
        Image scaledImage = image.getScaledInstance(image.getWidth() / multiSampling, image.getHeight() / multiSampling, Image.SCALE_SMOOTH);

        Graphics2D shrunkGraphics = shrunk.createGraphics();
        shrunkGraphics.drawImage(scaledImage, new AffineTransform(), null);

        ImageIO.write(shrunk, "png", new File("out.png"));
    }













}