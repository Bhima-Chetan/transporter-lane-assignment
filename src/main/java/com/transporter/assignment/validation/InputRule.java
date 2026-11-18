package com.transporter.assignment.validation;

import com.transporter.assignment.model.TransporterInput;

import java.util.List;

public interface InputRule {

    List<String> findProblems(TransporterInput input);
}
