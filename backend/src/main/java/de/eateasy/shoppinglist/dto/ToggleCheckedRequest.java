package de.eateasy.shoppinglist.dto;

import jakarta.validation.constraints.NotNull;

public record ToggleCheckedRequest(
    @NotNull Boolean checked
) {
}
