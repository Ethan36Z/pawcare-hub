package com.pawcarehub.backend.controller;

import com.pawcarehub.backend.dto.scheduling.ScheduleBookingConflictResponse;
import com.pawcarehub.backend.service.ScheduleBookingConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ScheduleConflictExceptionHandler {

    @ExceptionHandler(ScheduleBookingConflictException.class)
    public ResponseEntity<ScheduleBookingConflictResponse> handleScheduleBookingConflict(
        ScheduleBookingConflictException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(exception.getResponse());
    }
}
