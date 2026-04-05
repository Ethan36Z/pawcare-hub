package com.pawcarehub.backend.service;

import com.pawcarehub.backend.dto.admin.AdminBookingResponse;
import com.pawcarehub.backend.dto.admin.CompleteBookingRequest;
import com.pawcarehub.backend.entity.Booking;
import com.pawcarehub.backend.entity.Pet;
import com.pawcarehub.backend.entity.PetMedicalNote;
import com.pawcarehub.backend.repository.BookingRepository;
import com.pawcarehub.backend.repository.PetMedicalNoteRepository;
import com.pawcarehub.backend.repository.PetRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminBookingService {
    private static final DateTimeFormatter BOOKING_DATE_FORMATTER =
        DateTimeFormatter.ofPattern("MMMM d, uuuu", Locale.US);

    private final BookingRepository bookingRepository;
    private final PetRepository petRepository;
    private final PetMedicalNoteRepository petMedicalNoteRepository;

    public AdminBookingService(
        BookingRepository bookingRepository,
        PetRepository petRepository,
        PetMedicalNoteRepository petMedicalNoteRepository
    ) {
        this.bookingRepository = bookingRepository;
        this.petRepository = petRepository;
        this.petMedicalNoteRepository = petMedicalNoteRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminBookingResponse> getAllBookings(String status, String service, String owner, String sort) {
        return bookingRepository.findAll().stream()
            .filter(booking -> matchesFilter(booking.getStatus(), status))
            .filter(booking -> matchesFilter(booking.getService(), service))
            .filter(booking -> matchesOwnerFilter(booking, owner))
            .sorted(resolveSort(sort))
            .map(this::toAdminBookingResponse)
            .toList();
    }

    @Transactional
    public AdminBookingResponse confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if ("Cancelled".equalsIgnoreCase(booking.getStatus()) || "Completed".equalsIgnoreCase(booking.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This booking cannot be confirmed");
        }

        booking.setStatus("Confirmed");
        Booking savedBooking = bookingRepository.save(booking);
        return toAdminBookingResponse(savedBooking);
    }

    @Transactional
    public AdminBookingResponse cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if ("Cancelled".equalsIgnoreCase(booking.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Booking is already cancelled");
        }

        if ("Completed".equalsIgnoreCase(booking.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Completed bookings cannot be cancelled");
        }

        booking.setStatus("Cancelled");
        Booking savedBooking = bookingRepository.save(booking);
        return toAdminBookingResponse(savedBooking);
    }

    @Transactional
    public AdminBookingResponse completeBooking(Long bookingId, CompleteBookingRequest request) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));

        if ("Cancelled".equalsIgnoreCase(booking.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cancelled bookings cannot be completed");
        }

        booking.setStatus("Completed");
        booking.setVisitSummary(normalizeOptionalField(request.visitSummary()));
        booking.setDiagnosisAssessment(normalizeOptionalField(request.diagnosisAssessment()));
        booking.setTreatmentRecommendation(normalizeOptionalField(request.treatmentRecommendation()));
        booking.setFollowUpNote(normalizeOptionalField(request.followUpNote()));

        Booking savedBooking = bookingRepository.save(booking);
        syncPetMedicalNote(savedBooking);
        return toAdminBookingResponse(savedBooking);
    }

    private boolean matchesFilter(String value, String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }

        return value != null && value.equalsIgnoreCase(filter.trim());
    }

    private boolean matchesOwnerFilter(Booking booking, String owner) {
        if (owner == null || owner.isBlank()) {
            return true;
        }

        String normalizedOwner = owner.trim().toLowerCase();
        return containsIgnoreCase(booking.getOwner().getName(), normalizedOwner)
            || containsIgnoreCase(booking.getOwner().getEmail(), normalizedOwner);
    }

    private boolean containsIgnoreCase(String value, String normalizedFilter) {
        return value != null && value.toLowerCase().contains(normalizedFilter);
    }

    private Comparator<Booking> resolveSort(String sort) {
        if ("oldest".equalsIgnoreCase(sort)) {
            return Comparator.comparing(Booking::getId);
        }

        return Comparator.comparing(Booking::getId).reversed();
    }

    private AdminBookingResponse toAdminBookingResponse(Booking booking) {
        return new AdminBookingResponse(
            booking.getId(),
            booking.getPetName(),
            booking.getResolvedServiceName(),
            booking.getDate(),
            booking.getTime(),
            booking.getStatus(),
            booking.getClinic(),
            booking.getResolvedStaffName(),
            booking.getOwner().getId(),
            booking.getOwner().getName(),
            booking.getOwner().getEmail(),
            booking.getOwnerNote(),
            booking.getVisitSummary(),
            booking.getDiagnosisAssessment(),
            booking.getTreatmentRecommendation(),
            booking.getFollowUpNote()
        );
    }

    private void syncPetMedicalNote(Booking booking) {
        if (!hasOutcomeContent(booking)) {
            return;
        }

        Pet pet = petRepository.findFirstByOwnerIdAndNameIgnoreCase(booking.getOwner().getId(), booking.getPetName())
            .orElse(null);
        if (pet == null) {
            return;
        }

        String noteText = buildOutcomeNoteText(booking);
        PetMedicalNote existingNote = petMedicalNoteRepository.findFirstByRelatedBookingId(booking.getId()).orElse(null);

        if (existingNote != null) {
            existingNote.setNoteDate(resolveNoteDate(booking));
            existingNote.setAuthor(booking.getResolvedStaffName());
            existingNote.setNoteText(noteText);
            petMedicalNoteRepository.save(existingNote);
            return;
        }

        petMedicalNoteRepository.save(new PetMedicalNote(
            pet,
            resolveNoteDate(booking),
            booking.getResolvedStaffName(),
            booking.getId(),
            noteText
        ));
    }

    private boolean hasOutcomeContent(Booking booking) {
        return StringUtils.hasText(booking.getVisitSummary())
            || StringUtils.hasText(booking.getDiagnosisAssessment())
            || StringUtils.hasText(booking.getTreatmentRecommendation())
            || StringUtils.hasText(booking.getFollowUpNote());
    }

    private LocalDate resolveNoteDate(Booking booking) {
        try {
            return LocalDate.parse(booking.getDate(), BOOKING_DATE_FORMATTER);
        } catch (Exception exception) {
            return LocalDate.now();
        }
    }

    private String buildOutcomeNoteText(Booking booking) {
        StringBuilder builder = new StringBuilder("Completed visit outcome");

        appendOutcomeSection(builder, "Visit summary", booking.getVisitSummary());
        appendOutcomeSection(builder, "Assessment", booking.getDiagnosisAssessment());
        appendOutcomeSection(builder, "Treatment", booking.getTreatmentRecommendation());
        appendOutcomeSection(builder, "Follow-up", booking.getFollowUpNote());

        return builder.toString();
    }

    private void appendOutcomeSection(StringBuilder builder, String label, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }

        builder.append("\n\n").append(label).append(": ").append(value.trim());
    }

    private String normalizeOptionalField(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        return value.trim();
    }
}
