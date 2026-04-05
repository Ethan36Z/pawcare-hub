package com.pawcarehub.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "pets")
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String species;

    @Column(nullable = false)
    private String breed;

    @Column(nullable = false)
    private String age;

    @Column(nullable = false)
    private String weight;

    @Column(nullable = false, length = 1000)
    private String note;

    @Column
    private String sex;

    @Column
    private LocalDate dateOfBirth;

    @Column
    private String color;

    @Column
    private String microchipNumber;

    @Column(length = 2000)
    private String allergies;

    @Column(length = 2000)
    private String chronicConditions;

    @Column(length = 2000)
    private String medications;

    @Column(length = 2000)
    private String vaccinationNotes;

    @Column(length = 2000)
    private String generalMedicalNotes;

    @Column(nullable = false)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    protected Pet() {
    }

    public Pet(
        String name,
        String species,
        String breed,
        String age,
        String weight,
        String note,
        String sex,
        LocalDate dateOfBirth,
        String color,
        String microchipNumber,
        String allergies,
        String chronicConditions,
        String medications,
        String vaccinationNotes,
        String generalMedicalNotes,
        String status,
        User owner
    ) {
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.age = age;
        this.weight = weight;
        this.note = note;
        this.sex = sex;
        this.dateOfBirth = dateOfBirth;
        this.color = color;
        this.microchipNumber = microchipNumber;
        this.allergies = allergies;
        this.chronicConditions = chronicConditions;
        this.medications = medications;
        this.vaccinationNotes = vaccinationNotes;
        this.generalMedicalNotes = generalMedicalNotes;
        this.status = status;
        this.owner = owner;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getMicrochipNumber() {
        return microchipNumber;
    }

    public void setMicrochipNumber(String microchipNumber) {
        this.microchipNumber = microchipNumber;
    }

    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public String getChronicConditions() {
        return chronicConditions;
    }

    public void setChronicConditions(String chronicConditions) {
        this.chronicConditions = chronicConditions;
    }

    public String getMedications() {
        return medications;
    }

    public void setMedications(String medications) {
        this.medications = medications;
    }

    public String getVaccinationNotes() {
        return vaccinationNotes;
    }

    public void setVaccinationNotes(String vaccinationNotes) {
        this.vaccinationNotes = vaccinationNotes;
    }

    public String getGeneralMedicalNotes() {
        return generalMedicalNotes;
    }

    public void setGeneralMedicalNotes(String generalMedicalNotes) {
        this.generalMedicalNotes = generalMedicalNotes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public User getOwner() {
        return owner;
    }
}
