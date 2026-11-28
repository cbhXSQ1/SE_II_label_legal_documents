package com.seii.legal.function.dto.adjudication;

import java.util.List;

public record ConflictListView(
        List<ConflictView> conflicts,
        int decidedCount,
        int undecidedCount
) {
}
