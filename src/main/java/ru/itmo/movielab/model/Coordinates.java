package ru.itmo.movielab.model;

import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import ru.itmo.movielab.validation.Finite;

@JsonbNillable
@Entity
@Table(name = "coordinates")
public class Coordinates {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Version
    @Column(nullable = false)
    private long version;

    @NotNull
    @Column(nullable = false)
    private Integer x;

    @NotNull
    @DecimalMax("2")
    @Finite
    @Column(nullable = false)
    private Float y;

    public long getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer value) {
        x = value;
    }

    public Float getY() {
        return y;
    }

    public void setY(Float value) {
        y = value;
    }
}
