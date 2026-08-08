package com.vantrack.routes.web.dto;

import com.vantrack.routes.UserRoute;

import java.util.UUID;

public record ParentResponse (
        UUID parentId,
        String studentName
) {
    public static ParentResponse fromEntity(UserRoute userRoute) {
        return new ParentResponse(
                userRoute.getParent().getId(),
                userRoute.getStudentName()
        );
    }
}
