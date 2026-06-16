package com.fluxusbackend.shrinkage.domain.model.commands;

import com.fluxusbackend.shrinkage.domain.model.valueobjects.ShrinkageId;

public record MarkShrinkageRequestedCommand(ShrinkageId shrinkageId) {
}
