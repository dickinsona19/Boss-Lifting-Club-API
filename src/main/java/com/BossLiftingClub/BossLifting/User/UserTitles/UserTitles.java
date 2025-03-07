package com.BossLiftingClub.BossLifting.User.UserTitles;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "user_titles")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class UserTitles {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String title;

    // Default constructor
    public UserTitles() {}

    // Constructor with title
    public UserTitles(String title) {
        this.title = title;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
