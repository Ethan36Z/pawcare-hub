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
        String status,
        User owner
    ) {
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.age = age;
        this.weight = weight;
        this.note = note;
        this.status = status;
        this.owner = owner;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSpecies() {
        return species;
    }

    public String getBreed() {
        return breed;
    }

    public String getAge() {
        return age;
    }

    public String getWeight() {
        return weight;
    }

    public String getNote() {
        return note;
    }

    public String getStatus() {
        return status;
    }

    public User getOwner() {
        return owner;
    }
}
