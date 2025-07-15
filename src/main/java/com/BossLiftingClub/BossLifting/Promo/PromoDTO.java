package com.BossLiftingClub.BossLifting.Promo;


import java.util.List;

public class PromoDTO {
    private Long id;
    private String name;
    private String codeToken;
    private List<Long> userIds; // Only include user IDs to avoid loading full User objects

    // Constructor
    public PromoDTO(Long id, String name, String codeToken, List<Long> userIds) {
        this.id = id;
        this.name = name;
        this.codeToken = codeToken;
        this.userIds = userIds;
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

    public List<Long> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<Long> userIds) {
        this.userIds = userIds;
    }
}