package com.example.accidentscatchmanagement.repository

import com.example.accidentscatchmanagement.domain.LocationPhoto
import org.springframework.data.jpa.repository.JpaRepository

interface LocationPhotoRepository : JpaRepository<LocationPhoto, String>
