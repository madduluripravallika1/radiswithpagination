package com.example.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import javax.persistence.Entity;
import java.io.Serializable;
import java.time.ZonedDateTime;

@Entity
@Data
public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private int id;

    private String name;
@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
private ZonedDateTime localdatetime;


    public Customer() {
    }

    @JsonCreator
    public Customer(@JsonProperty("id") int id,
                    @JsonProperty("name") String name,
                    @JsonProperty("localdatetime") ZonedDateTime localdatetime) {
        this.id = id;
        this.name = name;
        this.localdatetime = localdatetime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ZonedDateTime getLocaldatetime() {
        return localdatetime;
    }

    public void setLocaldatetime(ZonedDateTime localdatetime) {
        this.localdatetime = localdatetime;
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", localdatetime=" + localdatetime +
                '}';
    }
}
