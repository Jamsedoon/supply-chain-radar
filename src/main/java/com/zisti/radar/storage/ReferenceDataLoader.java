package com.zisti.radar.storage;

import com.zisti.radar.matching.Normalizer;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the reference package list into the database on startup.
 *
 * <p>Skips the load when the table already holds rows, so restarts are cheap.
 * The CSV ships inside the jar, which keeps the deployed container independent
 * of the npm API being reachable at boot.
 */
@Component
public class ReferenceDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ReferenceDataLoader.class);
    private static final String RESOURCE = "/reference-packages.csv";
    private static final int BATCH_SIZE = 500;

    private final JdbcTemplate jdbc;

    public ReferenceDataLoader(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        Integer existing = jdbc.queryForObject(
            "SELECT count(*) FROM reference_package", Integer.class);

        if (existing != null && existing > 0) {
            log.info("reference_package already holds {} rows, skipping load", existing);
            return;
        }

        List<Object[]> batch = new ArrayList<>(BATCH_SIZE);
        int loaded = 0;

        try (InputStream in = getClass().getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("not on the classpath: " + RESOURCE);
            }

            BufferedReader reader =
                new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

            String line;
            boolean header = true;

            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                if (line.isBlank()) {
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length < 2) {
                    continue;
                }

                String name = parts[0].trim();
                batch.add(new Object[] {
                    name,
                    Normalizer.normalize(name),
                    Long.parseLong(parts[1].trim())
                });

                if (batch.size() >= BATCH_SIZE) {
                    loaded += flush(batch);
                }
            }
        }

        loaded += flush(batch);
        log.info("loaded {} reference packages", loaded);
    }

    private int flush(List<Object[]> batch) {
        if (batch.isEmpty()) {
            return 0;
        }
        jdbc.batchUpdate(
            "INSERT INTO reference_package (name, normalized_name, weekly_downloads) "
                + "VALUES (?, ?, ?) ON CONFLICT (name) DO NOTHING",
            batch);
        int size = batch.size();
        batch.clear();
        return size;
    }
}