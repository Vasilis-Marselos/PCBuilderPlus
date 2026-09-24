package com.pcbuilder.data;

import com.pcbuilder.CPU;
import com.pcbuilder.Cooler;
import com.pcbuilder.GPU;
import com.pcbuilder.Motherboard;
import com.pcbuilder.PSU;
import com.pcbuilder.RAM;
import com.pcbuilder.Storage;
import com.pcbuilder.PCCase;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;







public class CsvCatalogRepository {

    private final Class<?> resourceBase;

    public CsvCatalogRepository(Class<?> resourceBase) {
        this.resourceBase = resourceBase;
    }

    public List<CPU> loadCpuCatalog() throws IOException {
        List<CPU> list = new ArrayList<>();
        for (Map<String, String> row : readCsv("/data/cpu.csv")) {
            list.add(new CPU(
                    required(row, "id"),
                    required(row, "brand"),
                    required(row, "model"),
                    required(row, "socket"),
                    parseInt(required(row, "cores")),
                    parseDouble(required(row, "base_clock_ghz")),
                    parseDouble(required(row, "perf_score")),
                    parseInt(required(row, "tdp_w")),
                    parseDouble(required(row, "price_eur")),
                    optional(row, "manufacturer_part_number"),
                    optional(row, "skroutz_query"),
                    optional(row, "skroutz_url")
            ));
        }
        return list;
    }

    public List<GPU> loadGpuCatalog() throws IOException {
        List<GPU> list = new ArrayList<>();
        for (Map<String, String> row : readCsv("/data/gpu.csv")) {
            int lengthMm = parseInt(optional(row, "length_mm"));
            list.add(new GPU(
                    required(row, "id"),
                    required(row, "brand"),
                    required(row, "model"),
                    parseInt(required(row, "vram_gb")),
                    parseDouble(required(row, "perf_score")),
                    parseInt(required(row, "tdp_w")),
                    lengthMm,
                    parseDouble(required(row, "price_eur")),
                    optional(row, "manufacturer_part_number"),
                    optional(row, "skroutz_query"),
                    optional(row, "skroutz_url")
            ));
        }
        return list;
    }

    public List<Motherboard> loadMotherboardCatalog() throws IOException {
        List<Motherboard> list = new ArrayList<>();
        for (Map<String, String> row : readCsv("/data/motherboard.csv")) {
            list.add(new Motherboard(
                    required(row, "id"),
                    required(row, "brand"),
                    required(row, "model"),
                    required(row, "socket"),
                    required(row, "form_factor"),
                    optional(row, "memory_type"),
                    parseDouble(required(row, "price_eur")),
                    optional(row, "manufacturer_part_number"),
                    optional(row, "skroutz_query"),
                    optional(row, "skroutz_url")
            ));
        }
        return list;
    }

    public List<RAM> loadRamCatalog() throws IOException {
        List<RAM> list = new ArrayList<>();
        for (Map<String, String> row : readCsv("/data/ram.csv")) {
            list.add(new RAM(
                    required(row, "id"),
                    required(row, "brand"),
                    required(row, "model"),
                    parseInt(required(row, "capacity_gb")),
                    parseInt(required(row, "sticks")),
                    parseInt(required(row, "speed_mhz")),
                    parseDouble(required(row, "price_eur")),
                    optional(row, "manufacturer_part_number"),
                    optional(row, "skroutz_query"),
                    optional(row, "skroutz_url")
            ));
        }
        return list;
    }

    public List<Storage> loadStorageCatalog() throws IOException {
        List<Storage> list = new ArrayList<>();
        for (Map<String, String> row : readCsv("/data/storage.csv")) {
            list.add(new Storage(
                    required(row, "id"),
                    required(row, "brand"),
                    required(row, "model"),
                    required(row, "type"),
                    parseInt(required(row, "capacity_gb")),
                    parseDouble(required(row, "perf_score")),
                    parseDouble(required(row, "price_eur")),
                    optional(row, "manufacturer_part_number"),
                    optional(row, "skroutz_query"),
                    optional(row, "skroutz_url")
            ));
        }
        return list;
    }

    public List<PSU> loadPsuCatalog() throws IOException {
        List<PSU> list = new ArrayList<>();
        for (Map<String, String> row : readCsv("/data/psu.csv")) {
            list.add(new PSU(
                    required(row, "id"),
                    required(row, "brand"),
                    required(row, "model"),
                    parseInt(required(row, "wattage_w")),
                    parseDouble(required(row, "price_eur")),
                    optional(row, "manufacturer_part_number"),
                    optional(row, "skroutz_query"),
                    optional(row, "skroutz_url")
            ));
        }
        return list;
    }

    public List<PCCase> loadCaseCatalog() throws IOException {
        List<PCCase> list = new ArrayList<>();
        for (Map<String, String> row : readCsv("/data/case.csv")) {
            list.add(new PCCase(
                    required(row, "id"),
                    required(row, "brand"),
                    required(row, "model"),
                    required(row, "supported_form_factors"),
                    parseInt(required(row, "max_gpu_length_mm")),
                    parseInt(required(row, "max_air_cooler_height_mm")),
                    parseBoolean(required(row, "supports_240_aio")),
                    parseBoolean(required(row, "supports_280_aio")),
                    parseBoolean(required(row, "supports_360_aio")),
                    parseDouble(required(row, "price_eur")),
                    optional(row, "manufacturer_part_number"),
                    optional(row, "skroutz_query"),
                    optional(row, "skroutz_url")
            ));
        }
        return list;
    }

    public List<Cooler> loadCoolerCatalog() throws IOException {
        List<Cooler> list = new ArrayList<>();
        for (Map<String, String> row : readCsv("/data/cooler.csv")) {
            list.add(new Cooler(
                    required(row, "id"),
                    required(row, "brand"),
                    required(row, "model"),
                    Cooler.CoolerType.valueOf(required(row, "type").trim().toUpperCase()),
                    parseInt(required(row, "height_mm")),
                    parseInt(required(row, "radiator_size_mm")),
                    parseInt(required(row, "tdp_rating_w")),
                    parseDouble(required(row, "price_eur")),
                    optional(row, "manufacturer_part_number"),
                    optional(row, "skroutz_query"),
                    optional(row, "skroutz_url")
            ));
        }
        return list;
    }

    private List<Map<String, String>> readCsv(String resourcePath) throws IOException {
        InputStream in = resourceBase.getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IOException("Missing resource: " + resourcePath);
        }

        List<Map<String, String>> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String headerLine = null;
            while ((headerLine = reader.readLine()) != null) {
                if (!headerLine.trim().isEmpty()) {
                    break;
                }
            }
            if (headerLine == null) {
                return rows;
            }

            List<String> headers = parseCsvLine(headerLine);
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                List<String> values = parseCsvLine(line);
                if (values.isEmpty()) {
                    continue;
                }

                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    String key = headers.get(i).trim();
                    String value = i < values.size() ? values.get(i).trim() : "";
                    row.put(key, value);
                }
                rows.add(row);
            }
        }
        return rows;
    }

    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        fields.add(current.toString());
        return fields;
    }

    private String required(Map<String, String> row, String key) throws IOException {
        String value = row.get(key);
        if (value == null || value.isBlank()) {
            throw new IOException("Missing required CSV column '" + key + "' in row: " + row);
        }
        return value.trim();
    }

    private String optional(Map<String, String> row, String key) {
        String value = row.get(key);
        return value == null ? "" : value.trim();
    }

    private int parseInt(String value) {
        return Integer.parseInt(value.trim());
    }

    private double parseDouble(String value) {
        return Double.parseDouble(value.trim());
    }

    private boolean parseBoolean(String value) {
        return Boolean.parseBoolean(value.trim());
    }
}
