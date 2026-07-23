package com.praxis.service.analysis;

import java.util.List;

public record MultiPVResult(double score, String bestMoveUci, List<String> pvLines) {}
