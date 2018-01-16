import java.io.*;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Weather {

    private static final String[] regions = {"UK", "England", "Wales", "Scotland"};
    private static final String[] parameters = {"Tmax", "Tmin", "Tmean", "Sunshine", "Rainfall"};
    private static final int cellSize = 14;

    private static final int YEAR_ORDERED = 1, RANKED = 2;

    // Set the below variable accordingly
    private static int mode = YEAR_ORDERED;
    private static boolean useLocal = false;

    public static void main(String[] args) {
        try {
            File file = new File("weather.csv");
            if (file.createNewFile()) {
                System.out.println("File has been created successfully at " + file.getAbsolutePath());
                OutputStream outputStream = new FileOutputStream(file);
                try {
                    System.out.println("Please wait populating file with data...");
                    String titles = "region_code,weather_param,year,key,value\n";
                    outputStream.write(titles.getBytes());
                    if (mode == YEAR_ORDERED)
                        generateCSVDate(outputStream);
                    else
                        generateCSVRanked(outputStream);
                    System.out.println("Done");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                System.out.println("File already present at the " + file.getAbsolutePath() +
                        ". Please delete the file and try again.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void generateCSVRanked(OutputStream outputStream) {
        for (String region : regions) {
            for (String param : parameters) {
                try {
                    String requiredData = extractRequiredData(getContent(param, region));
                    assert requiredData != null;
                    String[] lines = requiredData.split("\n");
                    HashMap<Integer, String[]> yearWiseData = new HashMap<>();

                    // System.out.println("Validation for " + region + " " + param);
                    for (String line : lines) {
                        for (int i = 0, j = 0; i < line.length() - 1; i = i + cellSize, j++) {
                            String dataYear = line.substring(i, i + cellSize).trim();
                            // System.out.println(i + " " + (i + 14)+" "+j);
                            // if the cell is not empty
                            if (!dataYear.matches("^\\s*$")) {
                                // System.out.print(dataYear + "\t");

                                // this cell has data and year
                                String[] temp = dataYear.split("\\s+");
                                int year = Integer.parseInt(temp[1]);
                                if (!yearWiseData.containsKey(year))
                                    yearWiseData.put(year, new String[17]);
                                String k[] = yearWiseData.get(year);
                                //yearWiseData.put()
                                k[j] = temp[0];
                                yearWiseData.put(year, k);
                            }
                        }
                        // System.out.println();
                    }

                    for (int i = 1910; i <= 2017; i++) {
                        String k[] = yearWiseData.get(i);
                        for (int j = 0; j < 17; j++) {
                            String csvLine;
                            if (yearWiseData.containsKey(i)) {
                                if (k[j] != null) {
                                    csvLine = region + "," + correctedParameter(param) + "," + i + "," +
                                            weatherHeaders.values()[j] + "," + k[j] + "\n";
                                } else {
                                    csvLine = region + "," + correctedParameter(param) + "," + i + "," +
                                            weatherHeaders.values()[j] + "," + "N/A" + "\n";
                                }
                                // System.out.print(csvLine);
                                outputStream.write(csvLine.getBytes());
                            }
                        }
                    }
                } catch (ConnectException e) {
                    System.out.println("Could not connect to the URL. Please check.");
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    private static void generateCSVDate(OutputStream outputStream) {
        for (String region : regions) {
            for (String param : parameters) {
                try {
                    String requiredContent = extractRequiredData(getContent(param, region));
                    if (requiredContent != null) {
                        String[] lines = requiredContent.split("\n");
                        for (String line : lines) {
                            String[] cells = line.split("\\s+");
                            String year = cells[0];
                            for (int i = 1; i < cells.length; i++) {
                                String cell = cells[i];
                                if (cell.equals("---"))
                                    cell = "N/A";
                                String csvLine = region + "," + correctedParameter(param) + "," + year + "," +
                                        weatherHeaders.values()[i - 1] + "," + cell + "\n";
                                // System.out.println(csvLine);
                                outputStream.write(csvLine.getBytes());
                            }
                        }
                    }
                } catch (ConnectException e) {
                    System.out.println("Could not connect to the URL. Please check.");
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    private static String correctedParameter(String param) {
        switch (param) {
            case "Tmin":
                param = "Min temp";
                break;
            case "Tmax":
                param = "Max temp";
                break;
            case "Tmean":
                param = "Mean temp";
                break;
            default:
                break;
        }
        return param;
    }

    private static String getContent(String param, String region) throws IOException {
        final String weatherParam = "%weather_param%";
        final String regionCode = "%region_code%";

        // https://www.metoffice.gov.uk/pub/data/weather/uk/climate/datasets/Tmax/ranked/UK.txt
        // https://www.metoffice.gov.uk/pub/data/weather/uk/climate/datasets/Tmax/date/UK.txt
        final String baseURLRemote = "https://www.metoffice.gov.uk/pub/data/weather/uk/climate/datasets/"
                + weatherParam + "/" + (mode == RANKED ? "ranked" : "date") + "/" + regionCode + ".txt";

        // http://localhost/Weather/ranked/Tmax/UK.txt
        // http://localhost/Weather/date/Tmax/UK.txt
        final String baseURLLocal = "http://localhost/Weather/" + (mode == RANKED ? "ranked" : "date") + "/"
                + weatherParam + "/" + regionCode + ".txt";

        String baseURL = baseURLRemote;
        if (useLocal)
            baseURL = baseURLLocal;

        String temp = baseURL.replace(weatherParam, param).replace(regionCode, region);
        int i;
        URL url = new URL(temp);
        URLConnection con = url.openConnection();
        InputStream inputStream = con.getInputStream();
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        StringBuilder fileContent = new StringBuilder();
        while ((i = bufferedInputStream.read()) != -1) {
            fileContent.append((char) i);
        }
        return fileContent.toString();
    }

    private static String extractRequiredData(String fileContent) {
        // String lastUpdatedRegex = "Last updated \\d\\d/\\d\\d/\\d\\d\\d\\d\\.";
        String headerRegex;
        if (mode == RANKED)
            headerRegex = "AUT {2}Year {5}ANN {2}Year";
        else
            headerRegex = "WIN {4}SPR {4}SUM {4}AUT {5}ANN";
        Pattern headerPattern = Pattern.compile(headerRegex);
        Matcher headerMatcher = headerPattern.matcher(fileContent);
        if (headerMatcher.find()) {
            String requiredData = fileContent.substring(headerMatcher.end());
            // removing all empty lines
            return requiredData.replaceAll("(?m)^[ \t]*\r?\n", "");
        }
        return null;
    }

    private enum weatherHeaders {
        JAN, FEB, MAR, APR, MAY, JUN, JUL, AUG, SEP, OCT, NOV, DEC, WIN, SPR, SUM, AUT, ANN
    }
}