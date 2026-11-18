package com.transporter.assignment.repository;

import com.transporter.assignment.model.TransporterInput;

import java.util.Optional;

public interface TransporterInputRepository {

    void save(TransporterInput input);

    Optional<TransporterInput> findCurrent();
}
