package com.example.accidentscatchmanagement.exceptions

class AccidentSceneNotFoundException(
    id: String
) : RuntimeException(
    "Accident scene with id $id was not found"
)