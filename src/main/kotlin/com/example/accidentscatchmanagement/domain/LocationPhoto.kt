package com.example.accidentscatchmanagement.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

// Kept in a separate table so location searches do not load image bytes.
@Entity
@Table(name = "location_photo")
class LocationPhoto(
    @Id var id: String = "",
    @Column(nullable = false) var contentType: String = "image/png",
    @Column(nullable = false, columnDefinition = "bytea") var bytes: ByteArray = byteArrayOf()
)
