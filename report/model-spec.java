public class Movie {
    private long id; //Значение поля должно быть больше 0, Значение этого поля должно быть уникальным, Значение этого поля должно генерироваться автоматически
    private String name; //Поле не может быть null, Строка не может быть пустой
    private Coordinates coordinates; //Поле не может быть null
    private java.time.LocalDate creationDate; //Поле не может быть null, Значение этого поля должно генерироваться автоматически
    private Long oscarsCount; //Значение поля должно быть больше 0, Поле может быть null
    private double budget; //Значение поля должно быть больше 0
    private float totalBoxOffice; //Значение поля должно быть больше 0
    private MpaaRating mpaaRating; //Поле не может быть null
    private Person director; //Поле не может быть null
    private Person screenwriter;
    private Person operator; //Поле может быть null
    private Integer length; //Поле не может быть null, Значение поля должно быть больше 0
    private Long goldenPalmCount; //Значение поля должно быть больше 0, Поле может быть null
    private String tagline; //Поле может быть null
    private MovieGenre genre; //Поле может быть null
}
public class Coordinates {
    private Integer x; //Поле не может быть null
    private Float y; //Максимальное значение поля: 2, Поле не может быть null
}
public class Person {
    private String name; //Поле не может быть null, Строка не может быть пустой
    private Color eyeColor; //Поле не может быть null
    private Color hairColor; //Поле не может быть null
    private Location location; //Поле может быть null
    private Double height; //Поле не может быть null, Значение поля должно быть больше 0
    private Country nationality; //Поле может быть null
}
public class Location {
    private Integer x; //Поле не может быть null
    private Integer y; //Поле не может быть null
    private float z;
    private String name; //Поле не может быть null
}
public enum MpaaRating {
    G,
    PG,
    R,
    NC_17;
}
public enum MovieGenre {
    ACTION,
    WESTERN,
    COMEDY,
    FANTASY;
}
public enum Color {
    RED,
    YELLOW,
    WHITE;
}
public enum Country {
    UNITED_KINGDOM,
    GERMANY,
    THAILAND,
    SOUTH_KOREA;
}
