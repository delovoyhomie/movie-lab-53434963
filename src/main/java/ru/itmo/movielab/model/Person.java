package ru.itmo.movielab.model;

import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import ru.itmo.movielab.validation.Finite;

@JsonbNillable
@Entity
@Table(name = "persons")
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Version
    @Column(nullable = false)
    private long version;

    @NotBlank
    @Column(nullable = false, columnDefinition = "text")
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "eye_color", nullable = false)
    private Color eyeColor;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "hair_color", nullable = false)
    private Color hairColor;

    @Valid
    @ManyToOne
    @JoinColumn(name = "location_id")
    private Location location;

    @NotNull
    @Positive
    @Finite
    @Column(nullable = false)
    private Double height;

    @Enumerated(EnumType.STRING)
    private Country nationality;

    public long getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        name = value;
    }

    public Color getEyeColor() {
        return eyeColor;
    }

    public void setEyeColor(Color value) {
        eyeColor = value;
    }

    public Color getHairColor() {
        return hairColor;
    }

    public void setHairColor(Color value) {
        hairColor = value;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location value) {
        location = value;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double value) {
        height = value;
    }

    public Country getNationality() {
        return nationality;
    }

    public void setNationality(Country value) {
        nationality = value;
    }
}
