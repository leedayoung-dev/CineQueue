package com.cinequeue.backend.theater.schedule;

import com.cinequeue.backend.theater.entity.CinemaChain;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class CinemaScheduleClient {

    private static final Logger log = LoggerFactory.getLogger(CinemaScheduleClient.class);
    private static final DateTimeFormatter PLAY_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public CinemaScheduleClient(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${cinema.schedule.base-url:https://mcp.aka.page}") String baseUrl
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(4));
        requestFactory.setReadTimeout(Duration.ofSeconds(12));
        this.restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.objectMapper = objectMapper;
    }

    public List<CinemaScreening> fetch(CinemaChain chain, String keyword, LocalDate playDate) {
        String path = switch (chain) {
            case CGV -> "/api/cgv/timetable";
            case MEGABOX -> "/api/megabox/movies";
            case LOTTE -> "/api/lottecinema/movies";
        };
        try {
            String body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam("playDate", PLAY_DATE.format(playDate))
                            .queryParam("keyword", keyword)
                            .queryParam("limit", 80)
                            .build())
                    .retrieve()
                    .body(String.class);
            return parse(chain, body);
        } catch (RestClientException exception) {
            log.warn("Cinema schedule request failed for {} {}: {}", chain, keyword, exception.getMessage());
            return List.of();
        }
    }

    private List<CinemaScreening> parse(CinemaChain chain, String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            if (!root.path("success").asBoolean(false)) {
                return List.of();
            }
            JsonNode data = root.path("data");
            JsonNode rows = data.path("showtimes");
            if (!rows.isArray() || rows.isEmpty()) {
                rows = data.path("timetable");
            }
            if (!rows.isArray()) {
                return List.of();
            }
            List<CinemaScreening> screenings = new ArrayList<>();
            for (JsonNode row : rows) {
                String movieName = text(row, "movieName");
                String playDate = text(row, "playDate");
                String startTime = text(row, "startTime");
                if (movieName.isBlank() || playDate.isBlank() || startTime.isBlank()) {
                    continue;
                }
                String movieCode = text(row, "movieCode");
                if (movieCode.isBlank()) {
                    movieCode = text(row, "movieId");
                }
                String scheduleId = text(row, "scheduleId");
                String sourceKey = chain.name() + ":" + scheduleId + ":" + movieCode + ":" + playDate + ":" + startTime;
                screenings.add(new CinemaScreening(
                        sourceKey,
                        movieName,
                        playDate,
                        startTime,
                        text(row, "endTime"),
                        text(row, "screenName"),
                        integer(row, "totalSeats"),
                        integer(row, "remainingSeats")
                ));
            }
            return screenings;
        } catch (Exception exception) {
            log.warn("Cinema schedule response could not be read: {}", exception.getMessage());
            return List.of();
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText("").trim();
    }

    private Integer integer(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.asInt();
        }
        String text = value.asText("").trim();
        if (text.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
