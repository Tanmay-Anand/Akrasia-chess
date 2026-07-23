package com.praxis.domain.enums;

public enum AnalysisState {
    EXPLAINED,   // LLM provided explanation + tactical motif
    SKIPPED,     // Engine-only (no LLM call — not in top 3 worst mistakes)
    LLM_FAILED   // LLM was called but returned invalid/unparseable response
}
