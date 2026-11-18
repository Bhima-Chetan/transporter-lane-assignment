package com.transporter.assignment.repository;

import com.transporter.assignment.model.TransporterInput;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Repository
public class InMemoryTransporterInputRepository implements TransporterInputRepository {

    private final AtomicReference<TransporterInput> current = new AtomicReference<>();

    @Override
    public void save(TransporterInput input) {
        current.set(input);
    }

    @Override
    public Optional<TransporterInput> findCurrent() {
        return Optional.ofNullable(current.get());
    }
}
