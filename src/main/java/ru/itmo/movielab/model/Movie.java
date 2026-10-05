package ru.itmo.movielab.model;

import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import ru.itmo.movielab.validation.Finite;

@JsonbNillable
@Entity
@Table(name = "movies")
public class Movie {

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
    @Valid
    @ManyToOne(optional = false)
    @JoinColumn(name = "coordinates_id", nullable = false)
    private Coordinates coordinates;

    @NotNull
    @Column(name = "creation_date", nullable = false, updatable = false)
    private java.time.LocalDate creationDate = java.time.LocalDate.now(java.time.ZoneOffset.UTC);

    @Positive
    @Column(name = "oscars_count")
    private Long oscarsCount;

    @Positive
    @Finite
    @Column(nullable = false)
    private double budget;

    @Positive
    @Finite
    @Column(name = "total_box_office", nullable = false)
    private float totalBoxOffice;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "mpaa_rating", nullable = false)
    private MpaaRating mpaaRating;

    @NotNull
    @Valid
    @ManyToOne(optional = false)
    @JoinColumn(name = "director_id", nullable = false)
    private Person director;

    @Valid
    @ManyToOne
    @JoinColumn(name = "screenwriter_id")
    private Person screenwriter;

    @Valid
    @ManyToOne
    @JoinColumn(name = "operator_id")
    private Person operator;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer length;

    @Positive
    @Column(name = "golden_palm_count")
    private Long goldenPalmCount;

    @Column(columnDefinition = "text")
    private String tagline;

    @Enumerated(EnumType.STRING)
    private MovieGenre genre;

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

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(Coordinates value) {
        coordinates = value;
    }

    public java.time.LocalDate getCreationDate() {
        return creationDate;
    }

    public Long getOscarsCount() {
        return oscarsCount;
    }

    public void setOscarsCount(Long value) {
        oscarsCount = value;
    }

    public double getBudget() {
        return budget;
    }

    public void setBudget(double value) {
        budget = value;
    }

    public float getTotalBoxOffice() {
        return totalBoxOffice;
    }

    public void setTotalBoxOffice(float value) {
        totalBoxOffice = value;
    }

    public MpaaRating getMpaaRating() {
        return mpaaRating;
    }

    public void setMpaaRating(MpaaRating value) {
        mpaaRating = value;
    }

    public Person getDirector() {
        return director;
    }

    public void setDirector(Person value) {
        director = value;
    }

    public Person getScreenwriter() {
        return screenwriter;
    }

    public void setScreenwriter(Person value) {
        screenwriter = value;
    }

    public Person getOperator() {
        return operator;
    }

    public void setOperator(Person value) {
        operator = value;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer value) {
        length = value;
    }

    public Long getGoldenPalmCount() {
        return goldenPalmCount;
    }

    public void setGoldenPalmCount(Long value) {
        goldenPalmCount = value;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String value) {
        tagline = value;
    }

    public MovieGenre getGenre() {
        return genre;
    }

    public void setGenre(MovieGenre value) {
        genre = value;
    }

    @PrePersist
    void initializeDate() {
        creationDate = java.time.LocalDate.now(java.time.ZoneOffset.UTC);
    }
}
