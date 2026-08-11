package com.example.accidentscatchmanagement.config

import com.example.accidentscatchmanagement.domain.AccidentScene
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.axonframework.common.jpa.SimpleEntityManagerProvider
import org.axonframework.eventhandling.EventBus
import org.axonframework.messaging.annotation.ParameterResolverFactory
import org.axonframework.modelling.command.GenericJpaRepository
import org.axonframework.modelling.command.Repository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AxonRepositoriesConfiguration(
    @PersistenceContext
    private val entityManager: EntityManager
) {

    @Bean("axonAccidentSceneRepository")
    fun accidentSceneRepository(
        eventBus: EventBus,
        parameterResolverFactory: ParameterResolverFactory
    ): Repository<AccidentScene> {
        return GenericJpaRepository.builder(AccidentScene::class.java)
            .entityManagerProvider(
                SimpleEntityManagerProvider(entityManager)
            )
            .parameterResolverFactory(parameterResolverFactory)
            .eventBus(eventBus)
            .build()
    }
}