package com.transporter.assignment.service;

import com.transporter.assignment.exception.InvalidInputException;
import com.transporter.assignment.model.Lane;
import com.transporter.assignment.model.LaneQuote;
import com.transporter.assignment.model.Transporter;
import com.transporter.assignment.model.TransporterInput;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class QuoteSheetParser {

    private static final String BYTE_ORDER_MARK = "﻿";
    private static final Pattern WHOLE_NUMBER = Pattern.compile("\\d{1,15}");

    public TransporterInput parse(String csv) {
        List<String[]> rows = csv.replace(BYTE_ORDER_MARK, "").lines()
                .filter(line -> !line.isBlank())
                .map(line -> line.split(",", -1))
                .toList();

        if (rows.size() < 2) {
            throw new InvalidInputException("The sheet needs a header row followed by at least one lane row");
        }
        String[] header = rows.getFirst();
        if (header.length < 2) {
            throw new InvalidInputException("The header row needs a lane column followed by one column per transporter");
        }

        List<Lane> lanes = new ArrayList<>();
        List<List<LaneQuote>> quotesByColumn = new ArrayList<>();
        for (int column = 1; column < header.length; column++) {
            quotesByColumn.add(new ArrayList<>());
        }

        for (int row = 1; row < rows.size(); row++) {
            String[] cells = rows.get(row);
            if (cells.length != header.length) {
                throw new InvalidInputException("Row %d has %d columns but the header has %d"
                        .formatted(row + 1, cells.length, header.length));
            }

            long laneId = row;
            lanes.add(new Lane(laneId, null, null));
            for (int column = 1; column < cells.length; column++) {
                String cell = cells[column].trim();
                if (!cell.isEmpty()) {
                    quotesByColumn.get(column - 1).add(new LaneQuote(laneId, parseQuote(cell, row + 1, header[column])));
                }
            }
        }

        List<Transporter> transporters = new ArrayList<>();
        for (int column = 1; column < header.length; column++) {
            transporters.add(new Transporter((long) column, header[column].trim(), quotesByColumn.get(column - 1)));
        }
        return new TransporterInput(lanes, transporters);
    }

    private static long parseQuote(String cell, int rowNumber, String columnName) {
        if (!WHOLE_NUMBER.matcher(cell).matches() || Long.parseLong(cell) == 0) {
            throw new InvalidInputException("Row %d, column %s: '%s' is not a positive whole number"
                    .formatted(rowNumber, columnName.trim(), cell));
        }
        return Long.parseLong(cell);
    }
}
