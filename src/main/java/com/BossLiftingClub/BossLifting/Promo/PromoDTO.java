package com.BossLiftingClub.BossLifting.Promo;


import com.BossLiftingClub.BossLifting.User.UserDTO;
import java.util.List;

public class PromoDTO {
    private Long id;
    private String name;
    private String codeToken;
    private List<UserDTO> users; // Include full UserDTO objects instead of just IDs
    private Integer freePassCount;


    // Constructor
    public PromoDTO(Long id, String name, String codeToken, List<UserDTO> users, Integer freePassCount) {
        this.id = id;
        this.name = name;
        this.codeToken = codeToken;
        this.users = users;
        this.freePassCount = freePassCount;
    }

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

    public List<UserDTO> getUsers() {
        return users;
    }

    public void setUsers(List<UserDTO> users) {
        this.users = users;
    }

    public Integer getFreePassCount() {
        return freePassCount;
    }

    public void setFreePassCount(Integer freePassCount) {
        this.freePassCount = freePassCount;
    }
}