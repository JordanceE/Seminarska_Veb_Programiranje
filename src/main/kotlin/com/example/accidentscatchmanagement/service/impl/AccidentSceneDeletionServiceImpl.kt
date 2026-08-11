package com.example.accidentscatchmanagement.service.impl
import com.example.accidentscatchmanagement.exceptions.AccidentSceneNotFoundException
import com.example.accidentscatchmanagement.repository.AccidentSceneJpaRepository
import com.example.accidentscatchmanagement.service.AccidentSceneDeletionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
@Service
class AccidentSceneDeletionServiceImpl(
    private val repository: AccidentSceneJpaRepository
) : AccidentSceneDeletionService {

    @Transactional
    override fun deleteById(id: String) {
        val scene = repository.findById(id)
            .orElseThrow {
                AccidentSceneNotFoundException(id)
            }

        repository.delete(scene)
        repository.flush()
    }
}