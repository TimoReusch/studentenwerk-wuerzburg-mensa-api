package com.example.mensaapi.data_fetcher.retrieval;

import com.example.mensaapi.data_fetcher.dataclasses.FetchedDay;
import com.example.mensaapi.data_fetcher.dataclasses.enums.Location;
import com.example.mensaapi.data_fetcher.dataclasses.interfaces.FetchedCanteen;
import com.example.mensaapi.data_fetcher.dataclasses.interfaces.FetchedOpeningHours;
import com.example.mensaapi.data_fetcher.retrieval.interfaces.Fetcher;
import com.example.mensaapi.data_fetcher.retrieval.interfaces.Parser;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CanteenParser implements Parser<FetchedCanteen> {
    /**
     * Expects an Element of class "mensa"
     *
     * @param fetched
     * @return
     */
    @Override
    public Optional<FetchedCanteen> parse(Element fetched) {
        Elements details = fetched.getElementsByClass("right");
        String name = "";
        String headerInfo = "";
        Location location = Location.WÜRZBURG;
        String linkToFood = "";
        String additional = "";
        List<FetchedOpeningHours> op = new ArrayList<>();


        for (Element detail : details) {
            // Get Name
            String nameElement = detail.getElementsByTag("h4").text();
            // Info text (e.g. mensa is closed) is directly after the mensa-name in a h4-Tag, seperated by hyphen or pipe
            if (nameElement.contains(" - ")) {
                String[] parts = nameElement.split(" - ");
                name = parts[0];
                headerInfo = parts[1];
            } else if (nameElement.contains(" | ")) {
                String[] parts = nameElement.split(" \\| ");
                name = parts[0];
                headerInfo = parts[1];
            } else {
                // If none of those delimiters can be found, we assume there is no info text.
                name = nameElement;
            }

            // Get the city the mensa is located in
            location = getLocation(nameElement);

            // Get opening hours
            Elements opening = detail.getElementsByClass("opening");
            op = constructOpeningHours(opening.get(0).getElementsByTag("tr"));

            // Get additional information (e.g. evening mensa)
            additional = (opening.get(0).getElementsByTag("p").text());

            // Get Link
            linkToFood = ("https://www.studentenwerk-wuerzburg.de" + detail.getElementsByClass("fi").attr("href"));
        }

        Optional<Document> menuOfCanteen = Fetcher.createJSOUPFetcher(linkToFood).fetchCurrentData();

        List<FetchedDay> menus = new ArrayList<>();
        Parser<FetchedDay> dayParser = Parser.createDayParser();

        menuOfCanteen.ifPresent(document -> {
            Elements days = document.getElementsByClass("day").stream().filter(dayElement -> dayElement.tagName().equals("div")).collect(Collectors.toCollection(Elements::new));
            for (Element day: days) {
                menus.add(dayParser.parse(day).orElseThrow());
            }
        });

        return Optional.of(FetchedCanteen.createCanteen(
                name,
                location,
                headerInfo,
                op,
                additional,
                linkToFood,
                menus
        ));
    }

    private Location getLocation(String nameElement) {
        if (nameElement.contains(Location.WÜRZBURG.getValue()))
            return Location.WÜRZBURG;
        if (nameElement.contains(Location.BAMBERG.getValue()))
            return Location.BAMBERG;
        if (nameElement.contains(Location.ASCHAFFENBURG.getValue()))
            return Location.ASCHAFFENBURG;
        if (nameElement.contains(Location.SCHWEINFURT.getValue()))
            return Location.SCHWEINFURT;

        return null;
    }

    private int weekdayNameToInt(String shortName){
        return switch (shortName) {
            case "Mo" -> 1;
            case "Di" -> 2;
            case "Mi" -> 3;
            case "Do" -> 4;
            case "Fr" -> 5;
            case "Sa" -> 6;
            case "So" -> 7;
            default -> 0;
        };
    }

    private int weekdaysToIteratorNumber(String weekdays) {
        int day1, day2 = 0;
        if (weekdays.length() > 2) {
            String[] split = weekdays.split(" - ");
            day1 = weekdayNameToInt(split[0]);
            day2 = weekdayNameToInt(split[1]);
            return (day2 - day1) + 1;
        }
        if (weekdays.length() == 2)
            return 1;

        return 0;
    }

    private List<FetchedOpeningHours> constructOpeningHours(Elements contentTableRows) {
        List<FetchedOpeningHours> fetchedOpeningHoursList = new ArrayList<>();
        int weekdayCounter = 0;
        for (Element tableRow : contentTableRows) {
            Elements tableRowItems = tableRow.children();

            // Do we have opening hours?
            if (tableRowItems.size() > 2) {
                String[] hours = tableRowItems.get(1).text().split(" - | ");
                String mealOutTill = "";
                if(!tableRowItems.get(2).text().isEmpty()){
                    mealOutTill = tableRowItems.get(2).text().split(" ")[2];
                }
                int numberOfLoopExecutions = weekdaysToIteratorNumber(tableRowItems.get(0).text());

                for (int i = 0; i < numberOfLoopExecutions; i++) {


                    DayOfWeek day = DayOfWeek.values()[weekdayCounter];
                    boolean open = true;
                    String openingAt = "";
                    String closingAt = "";
                    String getAMealTill;

                    if (hours.length > 0 && !hours[0].equals("geschlossen")) {
                        openingAt = (hours[0]);
                        closingAt = (hours[1]);
                    } else {
                        open = (false);
                    }
                    getAMealTill = mealOutTill;

                    FetchedOpeningHours h = FetchedOpeningHours.createOpeningHours(
                            day,
                            open,
                            openingAt,
                            closingAt,
                            getAMealTill
                    );

                    weekdayCounter++;
                    fetchedOpeningHoursList.add(h);
                }
            }
        }
        return fetchedOpeningHoursList;
    }
}

