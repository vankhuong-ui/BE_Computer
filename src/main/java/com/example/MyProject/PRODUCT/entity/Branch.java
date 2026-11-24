package com.example.MyProject.PRODUCT.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "branches")
@Getter
@Setter
@NoArgsConstructor
public class Branch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 100)
    private String district;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(length = 50)
    private String phone;

    @Column(name = "opening_hours", nullable = false, length = 100)
    private String openingHours = "8:00 - 21:00";

    @Column(length = 255)
    private String services;

    @Column(nullable = false)
    private boolean active = true;
}
