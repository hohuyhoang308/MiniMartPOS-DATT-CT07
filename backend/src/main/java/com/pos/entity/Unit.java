package com.pos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Đơn vị tính (FR2.2) — bảng {@code units}: lon, chai, gói, thùng... */
@Entity
@Table(name = "units")
@Getter
@Setter
@NoArgsConstructor
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;
}
