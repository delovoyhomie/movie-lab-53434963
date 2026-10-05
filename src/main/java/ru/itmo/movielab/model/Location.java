package ru.itmo.movielab.model;

import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import ru.itmo.movielab.validation.Finite;

@JsonbNillable
@Entity
@Table(name = "locations")
public class Location {

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
    @Column(nullable = false)
    private Integer y;

    @Finite
    @Column(nullable = false)
    private float z;

    @NotNull
    @Column(nullable = false, columnDefinition = "text")
    private String name;

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

    public Integer getY() {
        return y;
    }

    public void setY(Integer value) {
        y = value;
    }

    public float getZ() {
        return z;
    }

    public void setZ(float value) {
        z = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        name = value;
    }
}
