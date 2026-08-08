package com.vantrack.routes.web.dto;

import java.util.UUID;

public record AddParentRequest (
        UUID parentId,
        String studentName
) {
}
