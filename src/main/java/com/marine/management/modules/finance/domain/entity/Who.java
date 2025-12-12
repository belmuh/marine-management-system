package com.marine.management.modules.finance.domain.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "who_list")
public class Who {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String nameTr;

    @Column(nullable = false, length = 100)
    private String nameEn;

    private boolean technical;

    @Column(name = "suggested_main_category_id")
    private Long suggestedMainCategoryId;  // Sadece öneri için

    @Column(nullable = false)
    private Boolean active = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Who() {
    }

    public Who(Long id, String code, String nameTr, String nameEn, boolean technical, Long suggestedMainCategoryId, Boolean active) {
        this.id = id;
        this.code = code;
        this.nameTr = nameTr;
        this.nameEn = nameEn;
        this.technical = technical;
        this.suggestedMainCategoryId = suggestedMainCategoryId;
        this.active = active;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNameTr() {
        return nameTr;
    }

    public void setNameTr(String nameTr) {
        this.nameTr = nameTr;
    }

    public String getNameEn() {
        return nameEn;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public boolean getTechnical() {
        return technical;
    }

    public void setTechnical(boolean technical) {
        this.technical = technical;
    }

    public Long getSuggestedMainCategoryId() {
        return suggestedMainCategoryId;
    }

    public void setSuggestedMainCategoryId(Long suggestedMainCategoryId) {
        this.suggestedMainCategoryId = suggestedMainCategoryId;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }


}

