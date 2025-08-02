package com.BossLiftingClub.BossLifting.Promo;

import com.BossLiftingClub.BossLifting.User.User;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "promo")
public class Promo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "code_token")
    private String codeToken;

    @ManyToMany
    @JoinTable(
            name = "promo_users",
            joinColumns = @JoinColumn(name = "promo_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> users = new ArrayList<>();

    @Column(name = "free_pass_count")
    private Integer freePassCount = 0;

    @Column(name = "url_visit_count")
    private Integer urlVisitCount = 0;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCodeToken() {
        return codeToken;
    }

    public void setCodeToken(String codeToken) {
        this.codeToken = codeToken;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public Integer getFreePassCount() {
        return freePassCount;
    }

    public void setFreePassCount(Integer freePassCount) {
        this.freePassCount = freePassCount;
    }

    public Integer getUrlVisitCount() {
        return urlVisitCount;
    }

    public void setUrlVisitCount(Integer urlVisitCount) {
        this.urlVisitCount = urlVisitCount;
    }
}